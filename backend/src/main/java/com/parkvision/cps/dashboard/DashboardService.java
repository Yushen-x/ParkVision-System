package com.parkvision.cps.dashboard;

import com.parkvision.cps.infrastructure.repository.ParkVisionRepository;
import com.parkvision.cps.parking.SlotStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {
    private final ParkVisionRepository repository;

    public DashboardService(ParkVisionRepository repository) {
        this.repository = repository;
    }

    public DashboardSummary summary() {
        var slots = repository.findSlots();
        long occupied = slots.stream().filter(slot -> slot.getStatus() != SlotStatus.EMPTY).count();
        int occupancyRate = (int) Math.round(occupied * 100.0 / slots.size());

        return new DashboardSummary(
                occupancyRate,
                414 + repository.findOrders().size(),
                "%d/%d".formatted(repository.findAgvUnits().size(), repository.findAgvUnits().size()),
                repository.findAlerts().size(),
                8426,
                "04:12",
                "7.4 次/时"
        );
    }

    public TrafficForecast forecast() {
        return new TrafficForecast(
                List.of(12, 18, 16, 22, 35, 48, 52, 42, 36, 58, 64, 49),
                List.of(61, 57, 49, 43, 38, 32)
        );
    }
}
