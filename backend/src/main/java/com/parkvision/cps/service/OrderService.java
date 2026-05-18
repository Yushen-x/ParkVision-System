package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.order.OrderStatus;
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
    private final Random random = new Random();

    public OrderService(ParkVisionRepository repository) {
        this.repository = repository;
    }

    public List<ParkingOrder> listOrders() {
        return repository.findOrders();
    }

    public ParkingOrder simulateEntry() {
        ParkingSlot slot = repository.findFirstAvailableSlot()
                .orElseThrow(() -> new BusinessException("NO_AVAILABLE_SLOT", "No slot is currently available"));
        String plate = PLATES.get(random.nextInt(PLATES.size()));
        slot.setStatus(isChargingPlate(plate) ? SlotStatus.CHARGING : SlotStatus.OCCUPIED);

        ParkingOrder order = new ParkingOrder(
                "PV" + String.valueOf(System.currentTimeMillis()).substring(4),
                plate,
                slot.getId(),
                LocalDateTime.now(),
                OrderStatus.PARKED,
                BigDecimal.ZERO
        );
        return repository.saveOrder(order);
    }

    public ParkingOrder changeStatus(String orderNo, OrderStatus status) {
        ParkingOrder order = repository.findOrderByNo(orderNo)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found: " + orderNo));
        order.setStatus(status);
        syncSlotState(order, status);
        if (status == OrderStatus.FINISHED) {
            order.setAmount(calculateAmount(order));
        }
        return repository.saveOrder(order);
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
}
