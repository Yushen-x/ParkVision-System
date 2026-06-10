package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.domain.customer.VehicleProfile;
import com.parkvision.cps.domain.device.ChargingStation;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.dto.pricing.PricingComponent;
import com.parkvision.cps.dto.pricing.PricingPreview;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-driven pricing engine. Both the live preview and the final settlement go
 * through {@link #quote(ParkingOrder, int)} so the number the owner sees and the
 * number actually billed always come from the same active {@link PricingRule}.
 */
@Service
public class PricingService {
    private static final BigDecimal PER_KWH = new BigDecimal("1.20");
    private static final BigDecimal VIP_PRIORITY_FEE = new BigDecimal("5.00");

    /** Used only when no active rule exists in the database. */
    private static final PricingRule DEFAULT_RULE = new PricingRule(
            "PR-DEFAULT", "默认计费", "ALL", 0,
            new BigDecimal("6.00"), new BigDecimal("4.00"), new BigDecimal("48.00"),
            17, 21, new BigDecimal("1.50"), "ACTIVE");

    private final ParkVisionRepository repository;

    public PricingService(ParkVisionRepository repository) {
        this.repository = repository;
    }

    /** Computed breakdown for an order at a given parked duration. */
    public record Quote(
            PricingRule rule,
            int durationMinutes,
            int billedHours,
            boolean peak,
            BigDecimal baseAmount,
            BigDecimal peakMultiplier,
            BigDecimal gross,
            List<PricingComponent> components
    ) {
    }

    public Quote quote(ParkingOrder order, int durationMinutes) {
        PricingRule rule = resolveRule(order.getPlateNo());
        LocalDateTime now = LocalDateTime.now();

        int chargeableMinutes = Math.max(0, durationMinutes - rule.freeMinutes());
        int billedHours = chargeableMinutes == 0 ? 0 : (int) Math.ceil(chargeableMinutes / 60.0);

        BigDecimal base = BigDecimal.ZERO;
        if (billedHours >= 1) {
            base = rule.firstHourFee().add(rule.hourlyFee().multiply(BigDecimal.valueOf(billedHours - 1L)));
        }
        base = base.setScale(2, RoundingMode.HALF_UP);

        boolean peak = isPeak(now, rule);
        BigDecimal peakMultiplier = peak ? rule.peakMultiplier() : BigDecimal.ONE;
        BigDecimal peakAdjusted = base.multiply(peakMultiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal peakSurcharge = peakAdjusted.subtract(base).setScale(2, RoundingMode.HALF_UP);

        BigDecimal capAdjust = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal capped = peakAdjusted;
        if (rule.dailyCap().compareTo(BigDecimal.ZERO) > 0 && capped.compareTo(rule.dailyCap()) > 0) {
            capAdjust = rule.dailyCap().subtract(capped).setScale(2, RoundingMode.HALF_UP);
            capped = rule.dailyCap().setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal chargingSurcharge = chargingSurchargeFor(order);
        BigDecimal vipSurcharge = vipSurchargeFor(order);

        BigDecimal gross = capped.add(chargingSurcharge).add(vipSurcharge).setScale(2, RoundingMode.HALF_UP);

        List<PricingComponent> components = new ArrayList<>();
        components.add(new PricingComponent("基础停车费",
                billedHours + " 小时（前 " + rule.freeMinutes() + " 分钟免费）", base, "base"));
        components.add(new PricingComponent("高峰调节费",
                peak ? peakMultiplier + " 倍拥堵调节" : "非高峰时段", peakSurcharge, "peak"));
        if (capAdjust.compareTo(BigDecimal.ZERO) < 0) {
            components.add(new PricingComponent("封顶优惠",
                    "单次封顶 " + rule.dailyCap().setScale(2, RoundingMode.HALF_UP), capAdjust, "cap"));
        }
        components.add(new PricingComponent("新能源充电", PER_KWH + " 元/千瓦时", chargingSurcharge, "charging"));
        components.add(new PricingComponent("VIP 优先取车", "固定调度优先费", vipSurcharge, "vip"));

        return new Quote(rule, durationMinutes, billedHours, peak, base, peakMultiplier, gross, components);
    }

    public PricingPreview preview(String orderNo) {
        ParkingOrder order = resolveOrder(orderNo);
        int durationMinutes = (int) Math.max(30, Duration.between(order.getEntryTime(), LocalDateTime.now()).toMinutes());
        Quote quote = quote(order, durationMinutes);

        BigDecimal totalAmount = quote.gross();
        if (order.getStatus() == OrderStatus.FINISHED && order.getAmount() != null
                && order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            totalAmount = order.getAmount().setScale(2, RoundingMode.HALF_UP);
        }

        return new PricingPreview(
                order.getOrderNo(),
                order.getPlateNo(),
                quote.peak() ? "高峰时段（" + quote.rule().name() + "）" : "标准时段（" + quote.rule().name() + "）",
                durationMinutes,
                quote.baseAmount(),
                quote.peakMultiplier().setScale(2, RoundingMode.HALF_UP),
                quote.components(),
                totalAmount,
                "费用由当前生效的计费规则、实时调度优先级与充电桩遥测共同计算。"
        );
    }

    private PricingRule resolveRule(String plate) {
        List<PricingRule> active = repository.findPricingRules().stream()
                .filter(rule -> "ACTIVE".equalsIgnoreCase(rule.status()))
                .toList();
        String vehicleType = isEvVehicle(plate) ? "EV" : "FUEL";
        return active.stream()
                .filter(rule -> vehicleType.equalsIgnoreCase(rule.vehicleType()))
                .findFirst()
                .or(() -> active.stream().filter(rule -> "ALL".equalsIgnoreCase(rule.vehicleType())).findFirst())
                .orElse(DEFAULT_RULE);
    }

    private boolean isEvVehicle(String plate) {
        boolean profileEv = repository.findVehicleProfiles().stream()
                .filter(profile -> profile.plateNo().equalsIgnoreCase(plate))
                .map(VehicleProfile::energyType)
                .findFirst()
                .map(energy -> energy != null && (energy.toUpperCase().contains("EV") || energy.contains("电")))
                .orElse(false);
        return profileEv || plate.toUpperCase().startsWith("SH-D");
    }

    private boolean isPeak(LocalDateTime now, PricingRule rule) {
        int hour = now.getHour();
        int start = rule.peakStartHour();
        int end = rule.peakEndHour();
        if (start == end) {
            return false;
        }
        if (start < end) {
            return hour >= start && hour < end;
        }
        return hour >= start || hour < end;
    }

    private BigDecimal chargingSurchargeFor(ParkingOrder order) {
        return repository.findChargingStations().stream()
                .filter(station -> order.getPlateNo().equals(station.vehiclePlate()))
                .findFirst()
                .map(this::chargingFee)
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal vipSurchargeFor(ParkingOrder order) {
        return repository.findDispatchQueue().stream()
                .filter(DispatchTask::isVip)
                .filter(task -> task.getPlateNo().equals(order.getPlateNo()))
                .findFirst()
                .map(task -> VIP_PRIORITY_FEE.setScale(2, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal chargingFee(ChargingStation station) {
        return station.sessionKwh()
                .multiply(PER_KWH)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private ParkingOrder resolveOrder(String orderNo) {
        if (orderNo != null && !orderNo.isBlank()) {
            return repository.findOrderByNo(orderNo)
                    .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found: " + orderNo));
        }
        return repository.findOrders().stream()
                .filter(order -> order.getStatus() != OrderStatus.FINISHED)
                .findFirst()
                .orElseGet(() -> repository.findOrders().stream().findFirst()
                        .orElseThrow(() -> new BusinessException("NO_ORDER", "No order is available for pricing preview")));
    }
}
