package com.parkvision.cps.domain.billing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransaction(
        String paymentNo,
        String orderNo,
        String plateNo,
        BigDecimal amount,
        String method,
        String status,
        LocalDateTime paidAt
) {
}
