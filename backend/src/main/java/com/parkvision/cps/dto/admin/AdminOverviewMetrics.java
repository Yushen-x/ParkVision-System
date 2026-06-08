package com.parkvision.cps.dto.admin;

import java.math.BigDecimal;

public record AdminOverviewMetrics(
        int activeOrders,
        int settledOrders,
        int customerCount,
        int vehicleCount,
        int paymentCount,
        int liveAlerts,
        long vipTasks,
        BigDecimal collectedRevenue
) {
}
