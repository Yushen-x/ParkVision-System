package com.parkvision.cps.dashboard;

import com.parkvision.cps.infrastructure.InMemoryDataStore;
import com.parkvision.cps.parking.SlotStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {
    private final InMemoryDataStore store;

    public DashboardService(InMemoryDataStore store) {
        this.store = store;
    }

    public DashboardSummary summary() {
        long occupied = store.slots().stream().filter(slot -> slot.getStatus() != SlotStatus.EMPTY).count();
        int occupancyRate = (int) Math.round(occupied * 100.0 / store.slots().size());
        return new DashboardSummary(
                occupancyRate,
                414 + store.orders().size(),
                "%d/%d".formatted(store.agvs().size(), store.agvs().size()),
                store.alerts().size(),
                8426,
                "04:12",
                "7.4 次/日"
        );
    }

    public TrafficForecast forecast() {
        return new TrafficForecast(
                List.of(12, 18, 16, 22, 35, 48, 52, 42, 36, 58, 64, 49),
                List.of(61, 57, 49, 43, 38, 32)
        );
    }
}
