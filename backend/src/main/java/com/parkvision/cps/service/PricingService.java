package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
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

@Service
public class PricingService {
    private final ParkVisionRepository repository;

    public PricingService(ParkVisionRepository repository) {
        this.repository = repository;
    }

    public PricingPreview preview(String orderNo) {
        ParkingOrder order = resolveOrder(orderNo);
        int durationMinutes = (int) Math.max(30, Duration.between(order.getEntryTime(), LocalDateTime.now()).toMinutes());
        long billedHours = (long) Math.ceil(durationMinutes / 60.0);
        BigDecimal baseAmount = new BigDecimal("6.00");
        if (billedHours > 1) {
            baseAmount = baseAmount.add(new BigDecimal("4.00").multiply(BigDecimal.valueOf(billedHours - 1)));
        }

        BigDecimal peakMultiplier = isPeakWindow(LocalDateTime.now()) ? new BigDecimal("1.50") : BigDecimal.ONE;
        BigDecimal peakAdjusted = baseAmount.multiply(peakMultiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal peakSurcharge = peakAdjusted.subtract(baseAmount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal vipSurcharge = repository.findDispatchQueue().stream()
                .filter(DispatchTask::isVip)
                .filter(task -> task.getPlateNo().equals(order.getPlateNo()))
                .findFirst()
                .map(task -> new BigDecimal("5.00"))
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        BigDecimal chargingSurcharge = repository.findChargingStations().stream()
                .filter(station -> order.getPlateNo().equals(station.vehiclePlate()))
                .findFirst()
                .map(this::chargingFee)
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        List<PricingComponent> components = new ArrayList<>();
        components.add(new PricingComponent("基础停车费", billedHours + " 小时阶梯计费", baseAmount.setScale(2, RoundingMode.HALF_UP), "base"));
        components.add(new PricingComponent("高峰调节费", peakMultiplier + " 倍拥堵调节", peakSurcharge, "peak"));
        components.add(new PricingComponent("新能源充电", "1.20 元/千瓦时", chargingSurcharge, "charging"));
        components.add(new PricingComponent("VIP 优先取车", "固定调度优先费", vipSurcharge, "vip"));

        BigDecimal totalAmount = peakAdjusted
                .add(chargingSurcharge)
                .add(vipSurcharge)
                .setScale(2, RoundingMode.HALF_UP);
        if (order.getStatus() == OrderStatus.FINISHED && order.getAmount() != null && order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            totalAmount = order.getAmount().setScale(2, RoundingMode.HALF_UP);
        }

        return new PricingPreview(
                order.getOrderNo(),
                order.getPlateNo(),
                isPeakWindow(LocalDateTime.now()) ? "工作日高峰时段" : "标准计时时段",
                durationMinutes,
                baseAmount.setScale(2, RoundingMode.HALF_UP),
                peakMultiplier.setScale(2, RoundingMode.HALF_UP),
                components,
                totalAmount,
                "费用预览来自持久化订单、当前调度优先级和实时充电桩遥测。"
        );
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

    private BigDecimal chargingFee(ChargingStation station) {
        return station.sessionKwh()
                .multiply(new BigDecimal("1.20"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPeakWindow(LocalDateTime now) {
        int hour = now.getHour();
        return hour >= 17 && hour < 21;
    }
}
