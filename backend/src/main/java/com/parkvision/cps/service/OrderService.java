package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.ParkingSlot;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class OrderService {
    private static final List<String> PLATES = List.of("SH-A7686", "SH-D5218", "SU-M9021", "SH-K1314", "SH-V7780");

    private final ParkVisionRepository repository;
    private final DeviceService deviceService;
    private final BillingService billingService;
    private final Random random = new Random();

    public OrderService(ParkVisionRepository repository, DeviceService deviceService, BillingService billingService) {
        this.repository = repository;
        this.deviceService = deviceService;
        this.billingService = billingService;
    }

    public List<ParkingOrder> listOrders() {
        return repository.findOrders();
    }

    public ParkingOrder simulateEntry() {
        return createEntry(PLATES.get(random.nextInt(PLATES.size())));
    }

    /**
     * Register an entry for a specific plate (owner self check-in or admin
     * gate registration). Rejects a plate that already has a live order so the
     * same car cannot be parked twice.
     */
    public ParkingOrder entryForPlate(String plateRaw) {
        String plate = plateRaw == null ? "" : plateRaw.trim().toUpperCase();
        if (plate.isEmpty()) {
            throw new BusinessException("INVALID_PLATE", "车牌不能为空");
        }
        boolean alreadyInside = repository.findOrders().stream()
                .anyMatch(order -> order.getPlateNo().equalsIgnoreCase(plate)
                        && order.getStatus() != OrderStatus.FINISHED);
        if (alreadyInside) {
            throw new BusinessException("ALREADY_PARKED", plate + " 已在场内，请勿重复入场");
        }
        return createEntry(plate);
    }

    private ParkingOrder createEntry(String plate) {
        ParkingSlot slot = repository.findFirstAvailableSlot()
                .orElseThrow(() -> new BusinessException("NO_AVAILABLE_SLOT", "No slot is currently available"));
        return admit(plate, slot);
    }

    /**
     * Admit a plate onto a specific slot (used when fulfilling a reservation that
     * already holds the slot).
     */
    public ParkingOrder admitToSlot(String plateRaw, String slotId) {
        String plate = plateRaw == null ? "" : plateRaw.trim().toUpperCase();
        if (plate.isEmpty()) {
            throw new BusinessException("INVALID_PLATE", "车牌不能为空");
        }
        boolean alreadyInside = repository.findOrders().stream()
                .anyMatch(order -> order.getPlateNo().equalsIgnoreCase(plate)
                        && order.getStatus() != OrderStatus.FINISHED);
        if (alreadyInside) {
            throw new BusinessException("ALREADY_PARKED", plate + " 已在场内，请勿重复入场");
        }
        ParkingSlot slot = repository.findSlotById(slotId)
                .orElseThrow(() -> new BusinessException("SLOT_NOT_FOUND", "车位不存在: " + slotId));
        return admit(plate, slot);
    }

    private ParkingOrder admit(String plate, ParkingSlot slot) {
        slot.setStatus(isChargingPlate(plate) ? SlotStatus.CHARGING : SlotStatus.OCCUPIED);
        repository.saveSlot(slot);

        ParkingOrder order = new ParkingOrder(
                "PV" + String.valueOf(System.currentTimeMillis()).substring(4),
                plate,
                slot.getId(),
                LocalDateTime.now(),
                OrderStatus.PARKED,
                BigDecimal.ZERO
        );
        ParkingOrder saved = repository.saveOrder(order);
        DispatchTask inboundTask = repository.enqueueDispatchTask(
                new DispatchTask(saved.getPlateNo(), "入场存车", "入场", "00:36", false, saved.getSlotId())
        );
        deviceService.recordEntry(saved);
        deviceService.recordDispatchTask(inboundTask);
        return saved;
    }

    public ParkingOrder changeStatus(String orderNo, OrderStatus status) {
        ParkingOrder order = repository.findOrderByNo(orderNo)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found: " + orderNo));
        if (order.getStatus() == status) {
            return order;
        }
        if (status == OrderStatus.TOUCHING && order.getStatus() != OrderStatus.PARKED) {
            throw new BusinessException("INVALID_STATUS", "仅在场停车中的订单可发起临停取物");
        }
        if (status == OrderStatus.PARKED && order.getStatus() == OrderStatus.TOUCHING) {
            order.setStatus(status);
            syncSlotState(order, status);
            return repository.saveOrder(order);
        }
        order.setStatus(status);
        syncSlotState(order, status);
        if (status == OrderStatus.FINISHED) {
            closeAndSettleOrder(order);
        }
        ParkingOrder saved = repository.saveOrder(order);
        if (status == OrderStatus.RETRIEVING) {
            DispatchTask task = repository.enqueueDispatchTask(new DispatchTask(saved.getPlateNo(), "标准取车", "先到先取", "04:12", false, saved.getSlotId()));
            deviceService.recordDispatchTask(task);
        } else if (status == OrderStatus.TOUCHING) {
            DispatchTask task = repository.enqueueDispatchTask(new DispatchTask(saved.getPlateNo(), "临停取物", "临取", "02:10", false, saved.getSlotId()));
            deviceService.recordDispatchTask(task);
        } else if (status == OrderStatus.FINISHED) {
            deviceService.recordOrderClosed(saved);
        }
        return saved;
    }

    private void closeAndSettleOrder(ParkingOrder order) {
        billingService.settle(order);
    }

    private void syncSlotState(ParkingOrder order, OrderStatus status) {
        repository.findSlotById(order.getSlotId()).ifPresent(slot -> {
            if (status == OrderStatus.RETRIEVING || status == OrderStatus.TOUCHING) {
                slot.setStatus(SlotStatus.BUFFER);
            } else if (status == OrderStatus.FINISHED) {
                slot.setStatus(SlotStatus.EMPTY);
            } else if (status == OrderStatus.PARKED) {
                slot.setStatus(isChargingPlate(order.getPlateNo()) ? SlotStatus.CHARGING : SlotStatus.OCCUPIED);
            }
            repository.saveSlot(slot);
        });
    }

    private boolean isChargingPlate(String plate) {
        return plate.startsWith("SH-D");
    }
}
