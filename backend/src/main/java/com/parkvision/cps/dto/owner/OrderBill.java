package com.parkvision.cps.dto.owner;

import com.parkvision.cps.domain.billing.OrderBillingComponent;
import com.parkvision.cps.domain.billing.PaymentTransaction;

import java.math.BigDecimal;
import java.util.List;

public record OrderBill(
        String orderNo,
        String plateNo,
        BigDecimal amount,
        BigDecimal discountAmount,
        String paymentStatus,
        String paymentMethod,
        List<OrderBillingComponent> components,
        PaymentTransaction payment
) {
}
