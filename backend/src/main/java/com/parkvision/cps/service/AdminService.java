package com.parkvision.cps.service;

import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.domain.admin.SystemNodeStatus;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.dto.admin.AdminOrderRow;
import com.parkvision.cps.dto.admin.AdminReport;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AdminService {
    private final ParkVisionRepository repository;

    public AdminService(ParkVisionRepository repository) {
        this.repository = repository;
    }

    public List<AdminOrderRow> orderRows() {
        return repository.findOrders().stream().map(this::toOrderRow).toList();
    }

    public List<AlertEvent> alerts() {
        return repository.findAlerts();
    }

    public List<PricingRule> pricingRules() {
        return repository.findPricingRules();
    }

    public List<AccessListItem> accessList() {
        return repository.findAccessList();
    }

    public List<SystemNodeStatus> systemNodes() {
        return repository.findSystemNodes();
    }

    public AdminReport buildReport(String query) {
        String normalizedQuery = query == null || query.isBlank()
                ? "最近 7 天 VIP 服务趋势"
                : query.trim();
        long vipTasks = repository.findDispatchQueue().stream().filter(DispatchTask::isVip).count();
        long occupiedSlots = repository.findSlots().stream().filter(slot -> slot.getStatus() != SlotStatus.EMPTY).count();
        int liveAlerts = repository.findAlerts().size();
        int queuedTasks = repository.findDispatchQueue().size();
        int realizedRevenue = repository.findOrders().stream()
                .map(ParkingOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        String summary = "当前 %d 条实时告警、%d 个调度排队任务、%d 个占用车位、已确认收入 %d 元。VIP 与预调度动作当前贡献 %d 个优先任务。"
                .formatted(liveAlerts, queuedTasks, occupiedSlots, realizedRevenue, vipTasks);

        return new AdminReport(
                normalizedQuery,
                summary,
                List.of("周一", "周二", "周三", "周四", "周五", "周六", "周日"),
                List.of(120, 132, 101, 134, 290, 430, 410),
                List.of(220, 182, 191, 234, 490, 530, 610)
        );
    }

    private AdminOrderRow toOrderRow(ParkingOrder order) {
        return new AdminOrderRow(
                order.getOrderNo(),
                order.getPlateNo(),
                eventOf(order),
                order.getSlotId(),
                statusLabel(order),
                formatAmount(order.getAmount())
        );
    }

    private String eventOf(ParkingOrder order) {
        return switch (order.getStatus()) {
            case PARKED -> "车辆入场";
            case RETRIEVING -> "取车请求";
            case TOUCHING -> "临停取物";
            case PAYING -> "待支付";
            case FINISHED -> "完成离场";
            case ABNORMAL -> "异常复核";
        };
    }

    private String statusLabel(ParkingOrder order) {
        return switch (order.getStatus()) {
            case PARKED -> "停车中";
            case RETRIEVING -> "调度中";
            case TOUCHING -> "交接区等待";
            case PAYING -> "等待支付";
            case FINISHED -> "已关闭";
            case ABNORMAL -> "需要复核";
        };
    }

    private String formatAmount(BigDecimal amount) {
        return "￥" + amount.setScale(2, RoundingMode.HALF_UP);
    }
}
