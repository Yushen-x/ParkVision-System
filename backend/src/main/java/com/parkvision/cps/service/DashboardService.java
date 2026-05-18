package com.parkvision.cps.service;

import com.parkvision.cps.domain.dashboard.DashboardSummary;
import com.parkvision.cps.domain.dashboard.TrafficForecast;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        int totalRevenue = repository.findOrders().stream()
                .map(order -> order.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        String avgWait = repository.findDispatchQueue().stream()
                .findFirst()
                .map(task -> task.getWait())
                .orElse("03:30");

        return new DashboardSummary(
                occupancyRate,
                410 + repository.findOrders().size(),
                "%d/%d".formatted(repository.findAgvUnits().size(), repository.findAgvUnits().size()),
                repository.findAlerts().size(),
                totalRevenue,
                avgWait,
                "7.4 / day"
        );
    }

    public TrafficForecast forecast() {
        return new TrafficForecast(
                List.of(12, 18, 16, 22, 35, 48, 52, 42, 36, 58, 64, 49),
                List.of(61, 57, 49, 43, 38, 32)
        );
    }
}
