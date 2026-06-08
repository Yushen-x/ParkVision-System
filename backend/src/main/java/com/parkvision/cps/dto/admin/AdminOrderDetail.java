package com.parkvision.cps.dto.admin;

import java.math.BigDecimal;
import java.util.List;

public record AdminOrderDetail(
        String orderNo,
        String plateNo,
        String slotId,
        String event,
        String status,
        String entryTime,
        String exitTime,
        Integer durationMinutes,
        BigDecimal amount,
        BigDecimal discountAmount,
        String paymentStatus,
        String paymentMethod,
        String paidAt,
        CustomerSummary customer,
        VehicleSummary vehicle,
        PaymentSummary payment,
        List<AdminBillingComponentRow> billingComponents
) {
    public record CustomerSummary(
            String ownerId,
            String ownerName,
            String phoneMasked,
            String memberLevel,
            String accountStatus,
            BigDecimal balance,
            String createdAt
    ) {
    }

    public record VehicleSummary(
            String plateNo,
            String ownerId,
            String vehicleType,
            String energyType,
            String membershipType,
            String defaultAuthStatus,
            String createdAt
    ) {
    }

    public record PaymentSummary(
            String paymentNo,
            BigDecimal amount,
            String method,
            String status,
            String paidAt
    ) {
    }
}
