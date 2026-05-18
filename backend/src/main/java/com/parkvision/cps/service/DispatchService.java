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
    private final DeviceService deviceService;

    public DispatchService(ParkVisionRepository repository, DeviceService deviceService) {
        this.repository = repository;
        this.deviceService = deviceService;
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
        repository.saveSlot(slot);

        ParkingOrder order = repository.findOrders().stream()
                .filter(candidate -> candidate.getSlotId().equals(slot.getId()))
                .findFirst()
                .orElseGet(this::latestActiveOrder);

        DispatchTask task = repository.enqueueDispatchTask(
                new DispatchTask(order.getPlateNo(), "高峰预调度移位", "预调度", "00:48", true)
        );
        updateLeadAgv("将 " + order.getPlateNo() + " 从 " + slot.getId() + " 移至缓冲区", true, "TRANSIT", 0.78, "relocate");
        deviceService.recordDispatchTask(task);
        return task;
    }

    public DispatchTask vip(String orderNo) {
        ParkingOrder order = resolveOrder(orderNo);
        order.setStatus(OrderStatus.RETRIEVING);
        repository.saveOrder(order);
        repository.findSlotById(order.getSlotId()).ifPresent(slot -> {
            slot.setStatus(SlotStatus.BUFFER);
            repository.saveSlot(slot);
        });

        DispatchTask task = repository.enqueueDispatchTask(
                new DispatchTask(order.getPlateNo(), "VIP 优先取车", "VIP", "00:30", true)
        );
        updateLeadAgv("VIP 优先取车 " + order.getPlateNo(), true, "CARRYING", 0.92, "vip-priority");
        deviceService.recordDispatchTask(task);
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
