package com.parkvision.cps.dto.pricing;

import java.math.BigDecimal;

public record PricingComponent(
        String label,
        String formula,
        BigDecimal amount,
        String accent
) {
}
