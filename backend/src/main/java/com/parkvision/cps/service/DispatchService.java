package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.ParkingSlot;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DispatchService {
    private final ParkVisionRepository repository;

    public DispatchService(ParkVisionRepository repository) {
        this.repository = repository;
    }

    public List<DispatchTask> queue() {
        return repository.findDispatchQueue();
    }

    public List<AgvUnit> agvs() {
        return repository.findAgvUnits();
    }

    public DispatchTask preDispatch() {
        ParkingSlot slot = repository.findFirstDeepOccupiedSlot()
                .orElseGet(() -> repository.findSlots().stream()
                        .filter(candidate -> candidate.getStatus() == SlotStatus.OCCUPIED)
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("NO_PRE_DISPATCH_TARGET", "No occupied slot is available for pre-dispatch")));
        slot.setStatus(SlotStatus.BUFFER);

        ParkingOrder order = repository.findOrders().stream()
                .filter(candidate -> candidate.getSlotId().equals(slot.getId()))
                .findFirst()
                .orElseGet(this::latestActiveOrder);

        DispatchTask task = repository.enqueueDispatchTask(
                new DispatchTask(order.getPlateNo(), "Pre-dispatch relocation", "PRE", "00:48", true)
        );
        updateLeadAgv("Relocating " + order.getPlateNo() + " from " + slot.getId(), true);
        return task;
    }

    public DispatchTask vip(String orderNo) {
        ParkingOrder order = resolveOrder(orderNo);
        order.setStatus(OrderStatus.RETRIEVING);
        repository.saveOrder(order);

        DispatchTask task = repository.enqueueDispatchTask(
                new DispatchTask(order.getPlateNo(), "VIP retrieval", "VIP", "00:30", true)
        );
        updateLeadAgv("VIP pickup for " + order.getPlateNo(), true);
        return task;
    }

    private ParkingOrder resolveOrder(String orderNo) {
        if (orderNo != null && !orderNo.isBlank()) {
            return repository.findOrderByNo(orderNo)
                    .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found: " + orderNo));
        }
        return latestActiveOrder();
    }

    private ParkingOrder latestActiveOrder() {
        return repository.findOrders().stream()
                .filter(order -> order.getStatus() != OrderStatus.FINISHED)
                .findFirst()
                .orElseThrow(() -> new BusinessException("NO_ACTIVE_ORDER", "No active order is available"));
    }

    private void updateLeadAgv(String task, boolean loaded) {
        repository.findAgvUnits().stream().findFirst().ifPresent(agv -> {
            agv.setTask(task);
            agv.setLoaded(loaded);
        });
    }
}
