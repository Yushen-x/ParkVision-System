package com.parkvision.cps.dto.admin;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record PricingRuleRequest(
        @NotBlank String name,
        @NotBlank String vehicleType,
        Integer freeMinutes,
        BigDecimal firstHourFee,
        BigDecimal hourlyFee,
        BigDecimal dailyCap,
        Integer peakStartHour,
        Integer peakEndHour,
        BigDecimal peakMultiplier,
        String status
) {
}
