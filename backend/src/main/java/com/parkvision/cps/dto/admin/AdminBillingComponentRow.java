package com.parkvision.cps.dto.admin;

import java.math.BigDecimal;

public record AdminBillingComponentRow(
        String componentNo,
        String orderNo,
        String componentType,
        String description,
        BigDecimal amount,
        String createdAt
) {
}
