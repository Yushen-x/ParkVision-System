package com.parkvision.cps.dashboard;

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
