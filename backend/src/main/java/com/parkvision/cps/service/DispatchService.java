package com.parkvision.cps.service;

import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.dispatch.DispatchTask;
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
        repository.findFirstDeepOccupiedSlot().ifPresent(slot -> slot.setStatus(SlotStatus.BUFFER));
        DispatchTask task = new DispatchTask("沪A·P7686", "提前移库", "Pre", "00:48", true);
        repository.enqueueDispatchTask(task);
        repository.findAgvUnits().stream().findFirst().ifPresent(agv -> {
            agv.setTask("执行深浅库位预调度");
            agv.setLoaded(true);
        });
        return task;
    }

    public DispatchTask vip() {
        return repository.enqueueDispatchTask(new DispatchTask("沪V·IP888", "VIP 加急取车", "VIP", "00:30", true));
    }
}
