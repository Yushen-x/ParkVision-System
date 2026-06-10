package com.parkvision.cps.service;

import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.repository.FallbackParkVisionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchServiceTest {

    private DispatchService newService(FallbackParkVisionRepository repository) {
        DeviceService deviceService = new DeviceService(repository, false);
        return new DispatchService(repository, deviceService, true);
    }

    @Test
    void workerDrivesTaskFromQueuedToDoneAndConsumesAgv() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        DispatchService service = newService(repository);

        DispatchTask task = repository.enqueueDispatchTask(
                new DispatchTask("TEST-001", "标准取车", "测试", "04:00", false, "A01"));
        Long id = task.getId();
        assertThat(id).isNotNull();
        assertThat(task.getStatus()).isEqualTo(DispatchTask.QUEUED);

        // Run the worker enough ticks to fully complete the task (10% start + 25%/tick).
        for (int i = 0; i < 8; i++) {
            service.advance();
        }

        DispatchTask completed = repository.findDispatchQueue().stream()
                .filter(t -> id.equals(t.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(completed.getStatus()).isEqualTo(DispatchTask.DONE);
        assertThat(completed.getProgress()).isEqualTo(100);
        assertThat(completed.getAgvId()).isNotNull();
    }

    @Test
    void workerLeavesTaskQueuedWhenNoAgvCanServe() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        DispatchService service = newService(repository);

        // Drain every AGV battery below the working threshold so none can pick up work.
        new java.util.ArrayList<>(repository.findAgvUnits()).forEach(agv -> {
            agv.setBatteryPct(10);
            agv.setLoaded(false);
            agv.setMode("CHARGING");
            repository.saveAgvUnit(agv);
        });

        DispatchTask task = repository.enqueueDispatchTask(
                new DispatchTask("TEST-LOW", "标准取车", "测试", "04:00", false, "A02"));
        Long id = task.getId();

        service.advance();

        DispatchTask after = repository.findDispatchQueue().stream()
                .filter(t -> id.equals(t.getId()))
                .findFirst()
                .orElseThrow();
        // Battery may have trickle-charged, but it cannot exceed the >25 gate in a single tick.
        assertThat(after.getStatus()).isEqualTo(DispatchTask.QUEUED);
        assertThat(after.getAgvId()).isNull();
    }

    @Test
    void idleAgvsRechargeTowardFull() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        DispatchService service = newService(repository);

        // Clear the seeded queue so AGVs have nothing to pick up and stay idle.
        new java.util.ArrayList<>(repository.findDispatchQueue()).forEach(t -> {
            t.setStatus(DispatchTask.DONE);
            repository.saveDispatchTask(t);
        });

        new java.util.ArrayList<>(repository.findAgvUnits()).forEach(agv -> {
            agv.setBatteryPct(40);
            agv.setLoaded(false);
            agv.setMode("IDLE");
            repository.saveAgvUnit(agv);
        });

        // No queued tasks → idle AGVs should trickle-charge.
        for (int i = 0; i < 3; i++) {
            service.advance();
        }

        assertThat(repository.findAgvUnits())
                .allMatch(agv -> agv.getBatteryPct() >= 43);
    }
}
