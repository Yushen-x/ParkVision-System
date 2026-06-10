package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.ParkingSlot;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DispatchService {
    private static final int PROGRESS_STEP = 25;

    private final ParkVisionRepository repository;
    private final DeviceService deviceService;
    private final boolean workerEnabled;

    public DispatchService(
            ParkVisionRepository repository,
            DeviceService deviceService,
            @Value("${parkvision.dispatch.worker-enabled:true}") boolean workerEnabled
    ) {
        this.repository = repository;
        this.deviceService = deviceService;
        this.workerEnabled = workerEnabled;
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
                new DispatchTask(order.getPlateNo(), "高峰预调度移位", "预调度", "00:48", true, slot.getId())
        );
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
                new DispatchTask(order.getPlateNo(), "VIP 优先取车", "VIP", "00:30", true, order.getSlotId())
        );
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

    /**
     * Dispatch worker: the authoritative engine that advances the task queue and
     * drives AGV motion. It assigns QUEUED tasks to idle AGVs, advances IN_PROGRESS
     * tasks toward completion, moves the assigned AGV's coordinates toward the slot,
     * and manages battery/charging — all persisted. This replaces the previous random
     * coordinate drift so the twin/queue reflect real backend progress.
     */
    @Scheduled(fixedDelayString = "${parkvision.dispatch.worker-interval-ms:2000}")
    public void advance() {
        if (!workerEnabled || deviceService.emergencyActive()) {
            return;
        }

        List<DispatchTask> tasks = new ArrayList<>(repository.findDispatchQueue());
        List<AgvUnit> agvs = new ArrayList<>(repository.findAgvUnits());
        if (agvs.isEmpty()) {
            return;
        }

        List<String> busyAgvIds = new ArrayList<>();

        // 1) Advance tasks already in progress.
        for (DispatchTask task : tasks) {
            if (!DispatchTask.IN_PROGRESS.equals(task.getStatus())) {
                continue;
            }
            AgvUnit agv = findAgv(agvs, task.getAgvId());
            int next = Math.min(100, task.getProgress() + PROGRESS_STEP);
            task.setProgress(next);
            if (next >= 100) {
                task.setStatus(DispatchTask.DONE);
                task.setWait("00:00");
                if (agv != null) {
                    agv.setLoaded(false);
                    agv.setMode("IDLE");
                    agv.setVelocityMps(0.0);
                    agv.setTask("已完成 " + task.getPlateNo() + "，待命复位");
                    agv.setLastCommand("park");
                    repository.saveAgvUnit(agv);
                }
                deviceService.recordDispatchDone(task);
            } else {
                task.setWait(remaining(next));
                if (agv != null) {
                    busyAgvIds.add(agv.getId());
                    moveToward(agv, task.getSlotId());
                    agv.setLoaded(true);
                    agv.setMode("CARRYING");
                    agv.setVelocityMps(task.isVip() ? 0.92 : 0.78);
                    agv.setTask(task.getType() + " " + task.getPlateNo());
                    agv.setLastCommand(task.isVip() ? "vip-carry" : "carry");
                    agv.setBatteryPct(Math.max(10, agv.getBatteryPct() - 1));
                    repository.saveAgvUnit(agv);
                }
            }
            repository.saveDispatchTask(task);
        }

        // 2) Assign queued tasks to free AGVs (queue is ordered IN_PROGRESS→QUEUED, VIP first).
        for (DispatchTask task : tasks) {
            if (!DispatchTask.QUEUED.equals(task.getStatus())) {
                continue;
            }
            AgvUnit agv = pickIdleAgv(agvs, busyAgvIds);
            if (agv == null) {
                break;
            }
            busyAgvIds.add(agv.getId());
            task.setStatus(DispatchTask.IN_PROGRESS);
            task.setAgvId(agv.getId());
            task.setProgress(Math.max(task.getProgress(), 10));
            task.setWait(remaining(task.getProgress()));
            repository.saveDispatchTask(task);

            agv.setLoaded(true);
            agv.setMode("CARRYING");
            agv.setVelocityMps(task.isVip() ? 0.92 : 0.78);
            agv.setTask(task.getType() + " " + task.getPlateNo());
            agv.setLastCommand("pickup");
            moveToward(agv, task.getSlotId());
            repository.saveAgvUnit(agv);
        }

        // 3) Idle AGVs dock and trickle-charge so they recover and become available.
        for (AgvUnit agv : agvs) {
            if (busyAgvIds.contains(agv.getId())) {
                continue;
            }
            agv.setLoaded(false);
            agv.setVelocityMps(0.0);
            if (agv.getBatteryPct() < 100) {
                agv.setBatteryPct(Math.min(100, agv.getBatteryPct() + 3));
            }
            boolean charging = agv.getBatteryPct() <= 30;
            agv.setMode(charging ? "CHARGING" : "IDLE");
            agv.setTask(charging ? "电池恢复充电" : "待命");
            agv.setLastCommand(charging ? "dock" : "hold");
            repository.saveAgvUnit(agv);
        }
    }

    private AgvUnit findAgv(List<AgvUnit> agvs, String agvId) {
        if (agvId == null) {
            return null;
        }
        return agvs.stream().filter(a -> agvId.equals(a.getId())).findFirst().orElse(null);
    }

    private AgvUnit pickIdleAgv(List<AgvUnit> agvs, List<String> busyAgvIds) {
        return agvs.stream()
                .filter(a -> !busyAgvIds.contains(a.getId()))
                .filter(a -> !"CHARGING".equals(a.getMode()))
                .filter(a -> a.getBatteryPct() > 25)
                .findFirst()
                .orElse(null);
    }

    private void moveToward(AgvUnit agv, String slotId) {
        int targetX = 50;
        int targetY = 48;
        if (slotId != null) {
            int hash = Math.abs(slotId.hashCode());
            targetX = 12 + hash % 74;
            targetY = 14 + (hash / 74) % 68;
        }
        agv.setX(agv.getX() + (int) Math.round((targetX - agv.getX()) * 0.4));
        agv.setY(agv.getY() + (int) Math.round((targetY - agv.getY()) * 0.4));
    }

    private String remaining(int progress) {
        int secondsLeft = (int) Math.round((100 - progress) / 100.0 * 240);
        return String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60);
    }
}
