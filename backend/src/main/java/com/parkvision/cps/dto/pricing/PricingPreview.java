package com.parkvision.cps.dto.pricing;

import java.math.BigDecimal;
import java.util.List;

public record PricingPreview(
        String orderNo,
        String plateNo,
        String pricingWindow,
        int durationMinutes,
        BigDecimal baseAmount,
        BigDecimal peakMultiplier,
        List<PricingComponent> components,
        BigDecimal totalAmount,
        String explanation
) {
}
