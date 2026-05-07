package com.parkvision.cps.order;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.infrastructure.repository.ParkVisionRepository;
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

    private final ParkVisionRepository repository;
    private final Random random = new Random();

    public OrderService(ParkVisionRepository repository) {
        this.repository = repository;
    }

    public ParkingOrder simulateEntry() {
        ParkingSlot slot = repository.findFirstAvailableSlot()
                .orElseThrow(() -> new BusinessException("NO_AVAILABLE_SLOT", "当前无可用泊位，已进入排队区"));
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
        return repository.saveOrder(order);
    }

    public ParkingOrder changeStatus(String orderNo, OrderStatus status) {
        ParkingOrder order = repository.findOrderByNo(orderNo)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在：" + orderNo));
        order.setStatus(status);
        if (status == OrderStatus.FINISHED) {
            order.setAmount(new BigDecimal("18.00"));
        }
        return order;
    }
}
