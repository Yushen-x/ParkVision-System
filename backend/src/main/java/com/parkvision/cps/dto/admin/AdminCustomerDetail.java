package com.parkvision.cps.dto.admin;

import java.math.BigDecimal;
import java.util.List;

public record AdminCustomerDetail(
        String ownerId,
        String ownerName,
        String phoneMasked,
        String memberLevel,
        String accountStatus,
        BigDecimal balance,
        String createdAt,
        int totalVehicles,
        int evVehicles,
        int activeOrders,
        int settledOrders,
        BigDecimal totalPaid,
        String lastPaymentAt,
        List<VehicleSummary> vehicles,
        List<OrderSummary> recentOrders
) {
    public record VehicleSummary(
            String plateNo,
            String vehicleType,
            String energyType,
            String membershipType,
            String defaultAuthStatus,
            String accessType,
            String createdAt
    ) {
    }

    public record OrderSummary(
            String orderNo,
            String plateNo,
            String status,
            BigDecimal amount,
            String paymentStatus,
            String entryTime
    ) {
    }
}
