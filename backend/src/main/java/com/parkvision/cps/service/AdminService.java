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
                ? "VIP service trend in the last 7 days"
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

        String summary = "%d live alerts, %d queued dispatch tasks, %d occupied slots, realized revenue %d CNY. VIP and pre-dispatch actions currently account for %d priority jobs."
                .formatted(liveAlerts, queuedTasks, occupiedSlots, realizedRevenue, vipTasks);

        return new AdminReport(
                normalizedQuery,
                summary,
                List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
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
            case PARKED -> "Vehicle entry";
            case RETRIEVING -> "Retrieve request";
            case TOUCHING -> "Touch-and-go";
            case PAYING -> "Pending payment";
            case FINISHED -> "Completed exit";
            case ABNORMAL -> "Exception review";
        };
    }

    private String statusLabel(ParkingOrder order) {
        return switch (order.getStatus()) {
            case PARKED -> "Active parking";
            case RETRIEVING -> "Dispatching";
            case TOUCHING -> "At handoff bay";
            case PAYING -> "Awaiting payment";
            case FINISHED -> "Closed";
            case ABNORMAL -> "Needs review";
        };
    }

    private String formatAmount(BigDecimal amount) {
        return "CNY " + amount.setScale(2, RoundingMode.HALF_UP);
    }
}
