package com.parkvision.cps.domain.billing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderBillingComponent(
        String componentNo,
        String orderNo,
        String componentType,
        String description,
        BigDecimal amount,
        LocalDateTime createdAt
) {
}
