package com.parkvision.cps.admin;

import com.parkvision.cps.infrastructure.repository.ParkVisionRepository;
import com.parkvision.cps.order.ParkingOrder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final ParkVisionRepository repository;

    public AdminService(ParkVisionRepository repository) {
        this.repository = repository;
    }

    public List<List<String>> orderTable() {
        return repository.findOrders().stream().map(this::toOrderRow).toList();
    }

    public List<List<String>> alertTable() {
        return repository.findAlerts().stream()
                .map(alert -> List.of(alert.alertNo(), alert.type(), alert.content(), alert.status(), alert.level()))
                .toList();
    }

    public List<PricingRule> pricingRules() {
        return repository.findPricingRules();
    }

    private List<String> toOrderRow(ParkingOrder order) {
        return List.of(
                order.getOrderNo(),
                order.getPlateNo(),
                order.getStatus().name(),
                order.getSlotId(),
                translateStatus(order.getStatus().name()),
                "¥%s".formatted(order.getAmount())
        );
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "PARKED" -> "停车中";
            case "RETRIEVING" -> "取车中";
            case "TOUCHING" -> "临时取物";
            case "PAYING" -> "待支付";
            case "FINISHED" -> "已完成";
            default -> "异常";
        };
    }
}
