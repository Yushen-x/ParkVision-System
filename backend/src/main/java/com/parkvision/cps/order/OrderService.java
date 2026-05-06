package com.parkvision.cps.order;

import com.parkvision.cps.infrastructure.InMemoryDataStore;
import com.parkvision.cps.parking.ParkingSlot;
import com.parkvision.cps.parking.SlotStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class OrderService {
    private static final List<String> PLATES = List.of("沪A·P7686", "沪D·E5218", "苏E·M9021", "浙A·K1314", "沪B·V7780");

    private final InMemoryDataStore store;
    private final Random random = new Random();

    public OrderService(InMemoryDataStore store) {
        this.store = store;
    }

    public ParkingOrder simulateEntry() {
        ParkingSlot slot = store.firstEmptySlot().orElseThrow(() -> new IllegalStateException("no empty slot"));
        String plate = PLATES.get(random.nextInt(PLATES.size()));
        slot.setStatus(plate.contains("D") ? SlotStatus.CHARGING : SlotStatus.OCCUPIED);
        ParkingOrder order = new ParkingOrder(
                "PV" + String.valueOf(System.currentTimeMillis()).substring(5),
                plate,
                slot.getId(),
                LocalDateTime.now(),
                OrderStatus.PARKED,
                BigDecimal.ZERO
        );
        store.addOrder(order);
        return order;
    }

    public ParkingOrder changeStatus(String orderNo, OrderStatus status) {
        ParkingOrder order = store.orders().stream()
                .filter(item -> item.getOrderNo().equals(orderNo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("order not found"));
        order.setStatus(status);
        if (status == OrderStatus.FINISHED) {
            order.setAmount(new BigDecimal("18.00"));
        }
        return order;
    }
}
