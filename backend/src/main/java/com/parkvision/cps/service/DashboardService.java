package com.parkvision.cps.service;

import com.parkvision.cps.domain.dashboard.DashboardSummary;
import com.parkvision.cps.domain.dashboard.TrafficForecast;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
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
        int occupancyRate = slots.isEmpty() ? 0 : (int) Math.round(occupied * 100.0 / slots.size());

        var orders = repository.findOrders();
        int totalRevenue = orders.stream()
                .map(ParkingOrder::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        LocalDate today = LocalDate.now();
        long todayTraffic = orders.stream()
                .filter(order -> order.getEntryTime() != null && order.getEntryTime().toLocalDate().equals(today))
                .count();

        var agvs = repository.findAgvUnits();
        long agvOnline = agvs.stream()
                .filter(agv -> agv.getBatteryPct() > 10 && !"OFFLINE".equalsIgnoreCase(agv.getMode()))
                .count();

        String avgWait = repository.findDispatchQueue().stream()
                .findFirst()
                .map(task -> task.getWait())
                .orElse("00:00");

        long chargingActive = slots.stream().filter(slot -> slot.getStatus() == SlotStatus.CHARGING).count();

        return new DashboardSummary(
                occupancyRate,
                (int) todayTraffic,
                "%d/%d".formatted(agvOnline, agvs.size()),
                repository.findAlerts().size(),
                totalRevenue,
                avgWait,
                chargingActive + " active"
        );
    }

    /**
     * Forecast derived from real entry records: the last 12 hourly buckets form
     * the history, and the next 6 buckets are projected with a naive moving
     * average (mean of the previous three observations).
     */
    public TrafficForecast forecast() {
        int historyBuckets = 12;
        int predictionBuckets = 6;

        LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).minusHours(historyBuckets - 1L);
        int[] hourly = new int[historyBuckets];
        for (ParkingOrder order : repository.findOrders()) {
            if (order.getEntryTime() == null) {
                continue;
            }
            long idx = ChronoUnit.HOURS.between(base, order.getEntryTime().truncatedTo(ChronoUnit.HOURS));
            if (idx >= 0 && idx < historyBuckets) {
                hourly[(int) idx]++;
            }
        }
        List<Integer> history = Arrays.stream(hourly).boxed().toList();

        List<Integer> work = new ArrayList<>(history);
        List<Integer> prediction = new ArrayList<>();
        for (int i = 0; i < predictionBuckets; i++) {
            int n = work.size();
            double avg = (work.get(n - 1) + work.get(n - 2) + work.get(n - 3)) / 3.0;
            int projected = (int) Math.round(avg);
            prediction.add(projected);
            work.add(projected);
        }

        return new TrafficForecast(history, prediction);
    }
}
