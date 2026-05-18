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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class OrderService {
    private static final List<String> PLATES = List.of("SH-A7686", "SH-D5218", "SU-M9021", "SH-K1314", "SH-V7780");

    private final ParkVisionRepository repository;
    private final DeviceService deviceService;
    private final Random random = new Random();

    public OrderService(ParkVisionRepository repository, DeviceService deviceService) {
        this.repository = repository;
        this.deviceService = deviceService;
    }

    public List<ParkingOrder> listOrders() {
        return repository.findOrders();
    }

    public ParkingOrder simulateEntry() {
        ParkingSlot slot = repository.findFirstAvailableSlot()
                .orElseThrow(() -> new BusinessException("NO_AVAILABLE_SLOT", "No slot is currently available"));
        String plate = PLATES.get(random.nextInt(PLATES.size()));
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
                new DispatchTask(saved.getPlateNo(), "Inbound storage", "IN", "00:36", false)
        );
        updateLeadAgv("Storing " + saved.getPlateNo() + " into " + saved.getSlotId(), true, "TRANSIT", 0.72, "store");
        deviceService.recordEntry(saved);
        deviceService.recordDispatchTask(inboundTask);
        return saved;
    }

    public ParkingOrder changeStatus(String orderNo, OrderStatus status) {
        ParkingOrder order = repository.findOrderByNo(orderNo)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found: " + orderNo));
        order.setStatus(status);
        syncSlotState(order, status);
        if (status == OrderStatus.FINISHED) {
            order.setAmount(calculateAmount(order));
        }
        ParkingOrder saved = repository.saveOrder(order);
        if (status == OrderStatus.RETRIEVING) {
            DispatchTask task = repository.enqueueDispatchTask(new DispatchTask(saved.getPlateNo(), "Standard retrieval", "FIFO", "04:12", false));
            updateLeadAgv("Retrieving " + saved.getPlateNo(), true, "CARRYING", 0.84, "retrieve");
            deviceService.recordDispatchTask(task);
        } else if (status == OrderStatus.TOUCHING) {
            DispatchTask task = repository.enqueueDispatchTask(new DispatchTask(saved.getPlateNo(), "Touch-and-Go", "Touch", "02:10", false));
            updateLeadAgv("Handoff delivery for " + saved.getPlateNo(), true, "CARRYING", 0.68, "handoff");
            deviceService.recordDispatchTask(task);
        } else if (status == OrderStatus.FINISHED) {
            updateLeadAgv("Release corridor clear", false, "IDLE", 0.00, "hold");
            deviceService.recordOrderClosed(saved);
        }
        return saved;
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

    private BigDecimal calculateAmount(ParkingOrder order) {
        long minutes = Math.max(30, Duration.between(order.getEntryTime(), LocalDateTime.now()).toMinutes());
        long billedHours = (long) Math.ceil(minutes / 60.0);
        BigDecimal amount = new BigDecimal("6.00");
        if (billedHours > 1) {
            amount = amount.add(new BigDecimal("4.00").multiply(BigDecimal.valueOf(billedHours - 1)));
        }
        if (isChargingPlate(order.getPlateNo())) {
            amount = amount.add(new BigDecimal("12.50"));
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private void updateLeadAgv(String task, boolean loaded, String mode, double velocityMps, String command) {
        repository.findAgvUnits().stream().findFirst().ifPresent(agv -> {
            agv.setTask(task);
            agv.setLoaded(loaded);
            agv.setMode(mode);
            agv.setVelocityMps(velocityMps);
            agv.setLastCommand(command);
            repository.saveAgvUnit(agv);
        });
    }
}
