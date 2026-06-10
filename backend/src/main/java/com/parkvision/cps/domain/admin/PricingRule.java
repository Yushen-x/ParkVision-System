package com.parkvision.cps.domain.admin;

import java.math.BigDecimal;

/**
 * A machine-usable tariff. The settlement engine selects the active rule that
 * matches the vehicle type and computes the fee from these numeric fields.
 *
 * @param vehicleType ALL / FUEL / EV
 * @param freeMinutes grace period before billing starts
 * @param firstHourFee fee for the first billed hour
 * @param hourlyFee fee for each subsequent billed hour
 * @param dailyCap maximum charge per stay (0 = no cap)
 * @param peakStartHour inclusive peak window start hour (0-23)
 * @param peakEndHour exclusive peak window end hour (0-23)
 * @param peakMultiplier multiplier applied during the peak window
 * @param status ACTIVE / INACTIVE
 */
public record PricingRule(
        String id,
        String name,
        String vehicleType,
        int freeMinutes,
        BigDecimal firstHourFee,
        BigDecimal hourlyFee,
        BigDecimal dailyCap,
        int peakStartHour,
        int peakEndHour,
        BigDecimal peakMultiplier,
        String status
) {
}
