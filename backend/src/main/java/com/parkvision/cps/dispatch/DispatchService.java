package com.parkvision.cps.dispatch;

import com.parkvision.cps.infrastructure.InMemoryDataStore;
import com.parkvision.cps.parking.ParkingSlot;
import com.parkvision.cps.parking.SlotStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DispatchService {
    private final InMemoryDataStore store;

    public DispatchService(InMemoryDataStore store) {
        this.store = store;
    }

    public List<DispatchTask> queue() {
        return store.queue();
    }

    public List<AgvUnit> agvs() {
        return store.agvs();
    }

    public DispatchTask preDispatch() {
        store.firstDeepOccupiedSlot().ifPresent(slot -> slot.setStatus(SlotStatus.BUFFER));
        DispatchTask task = new DispatchTask("沪A·P7686", "提前移库", "Pre", "00:48", true);
        store.addQueueTask(task);
        store.agvs().stream().findFirst().ifPresent(agv -> {
            agv.setTask("执行深浅库位预调度");
            agv.setLoaded(true);
        });
        return task;
    }

    public DispatchTask vip() {
        DispatchTask task = new DispatchTask("沪V·IP888", "VIP 加急取车", "VIP", "00:30", true);
        store.addQueueTask(task);
        return task;
    }
}
