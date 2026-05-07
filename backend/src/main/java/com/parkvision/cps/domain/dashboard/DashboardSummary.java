package com.parkvision.cps.domain.dashboard;

public record DashboardSummary(
        int occupancyRate,
        int trafficTotal,
        String agvOnline,
        int alertCount,
        int revenue,
        String avgWait,
        String chargingTurnover
) {
}
