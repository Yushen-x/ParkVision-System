package com.parkvision.cps.service;

import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.domain.admin.SystemNodeStatus;
import com.parkvision.cps.domain.billing.OrderBillingComponent;
import com.parkvision.cps.domain.billing.PaymentTransaction;
import com.parkvision.cps.domain.customer.CustomerAccount;
import com.parkvision.cps.domain.customer.VehicleProfile;
import com.parkvision.cps.domain.device.DeviceEvent;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.dto.admin.AdminAlertDetail;
import com.parkvision.cps.dto.admin.AdminBillingComponentRow;
import com.parkvision.cps.dto.admin.AdminCustomerDetail;
import com.parkvision.cps.dto.admin.AdminCustomerVehicleRow;
import com.parkvision.cps.dto.admin.AdminOrderDetail;
import com.parkvision.cps.dto.admin.AdminOverviewMetrics;
import com.parkvision.cps.dto.admin.AdminOrderRow;
import com.parkvision.cps.dto.admin.AdminReport;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AdminService {
    private static final DateTimeFormatter ADMIN_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ParkVisionRepository repository;

    public AdminService(ParkVisionRepository repository) {
        this.repository = repository;
    }

    public List<AdminOrderRow> orderRows(String status, String keyword, String dateFrom, String dateTo) {
        return repository.findOrders().stream()
                .filter(order -> matchesStatus(order.getStatus().name(), status))
                .filter(order -> matchesKeyword(keyword, order.getOrderNo(), order.getPlateNo(), order.getSlotId()))
                .filter(order -> matchesDateRange(order.getEntryTime(), dateFrom, dateTo))
                .map(this::toOrderRow)
                .toList();
    }

    public List<AlertEvent> alerts(String level, String status, String keyword) {
        return repository.findAlerts().stream()
                .filter(alert -> matchesStatus(alert.level(), level))
                .filter(alert -> matchesStatus(alert.status(), status))
                .filter(alert -> matchesKeyword(keyword, alert.alertNo(), alert.type(), alert.content()))
                .toList();
    }

    public AdminAlertDetail alertDetail(String alertNo) {
        AlertEvent alert = repository.findAlerts().stream()
                .filter(item -> item.alertNo().equalsIgnoreCase(alertNo))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Alert not found: " + alertNo));

        List<DeviceEvent> deviceEvents = repository.findDeviceEvents().stream()
                .filter(event -> matchesAlertDeviceEvent(alert, event))
                .limit(6)
                .toList();
        List<ParkingOrder> relatedOrders = repository.findOrders().stream()
                .filter(order -> matchesAlertOrder(alert, order))
                .limit(4)
                .toList();
        int acknowledgedCount = (int) deviceEvents.stream().filter(DeviceEvent::acknowledged).count();

        return new AdminAlertDetail(
                alert.alertNo(),
                alert.type(),
                alert.content(),
                alert.status(),
                alert.level(),
                recommendedAction(alert),
                escalationState(alert),
                deviceEvents.size(),
                acknowledgedCount,
                deviceEvents.stream().map(this::toAlertDeviceEventSummary).toList(),
                relatedOrders.stream().map(this::toAlertOrderHintSummary).toList()
        );
    }

    public List<PricingRule> pricingRules() {
        return repository.findPricingRules();
    }

    public List<AccessListItem> accessList() {
        return repository.findAccessList();
    }

    public List<AdminCustomerVehicleRow> customerVehicles(String energyType, String memberLevel, String keyword) {
        Map<String, CustomerAccount> accountsById = repository.findCustomerAccounts().stream()
                .collect(Collectors.toMap(CustomerAccount::ownerId, Function.identity()));
        Map<String, String> accessByPlate = repository.findAccessList().stream()
                .collect(Collectors.toMap(AccessListItem::plateNo, AccessListItem::listType, (left, right) -> left));

        return repository.findVehicleProfiles().stream()
                .map(vehicle -> toCustomerVehicleRow(vehicle, accountsById.get(vehicle.ownerId()), accessByPlate.get(vehicle.plateNo())))
                .filter(row -> matchesStatus(row.energyType(), energyType))
                .filter(row -> matchesStatus(row.memberLevel(), memberLevel))
                .filter(row -> matchesKeyword(keyword, row.ownerId(), row.ownerName(), row.plateNo(), row.phoneMasked()))
                .toList();
    }

    public AdminCustomerDetail customerDetail(String ownerId) {
        CustomerAccount account = repository.findCustomerAccounts().stream()
                .filter(item -> item.ownerId().equalsIgnoreCase(ownerId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found: " + ownerId));

        Map<String, String> accessByPlate = repository.findAccessList().stream()
                .collect(Collectors.toMap(AccessListItem::plateNo, AccessListItem::listType, (left, right) -> left));
        List<VehicleProfile> vehicles = repository.findVehicleProfiles().stream()
                .filter(vehicle -> vehicle.ownerId().equalsIgnoreCase(ownerId))
                .toList();
        Set<String> plates = vehicles.stream().map(VehicleProfile::plateNo).collect(Collectors.toSet());
        List<ParkingOrder> orders = repository.findOrders().stream()
                .filter(order -> plates.contains(order.getPlateNo()))
                .sorted((left, right) -> right.getEntryTime().compareTo(left.getEntryTime()))
                .toList();
        List<PaymentTransaction> payments = repository.findPaymentTransactions().stream()
                .filter(payment -> plates.contains(payment.plateNo()))
                .sorted((left, right) -> compareNullable(right.paidAt(), left.paidAt()))
                .toList();

        int activeOrders = (int) orders.stream().filter(order -> order.getStatus() != OrderStatus.FINISHED).count();
        int settledOrders = (int) orders.stream().filter(order -> order.getStatus() == OrderStatus.FINISHED).count();
        int evVehicles = (int) vehicles.stream().filter(vehicle -> "EV".equalsIgnoreCase(vehicle.energyType())).count();
        BigDecimal totalPaid = payments.stream()
                .filter(payment -> "SUCCESS".equalsIgnoreCase(payment.status()))
                .map(PaymentTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        String lastPaymentAt = payments.isEmpty() ? null : formatTime(payments.get(0).paidAt());

        return new AdminCustomerDetail(
                account.ownerId(),
                account.ownerName(),
                account.phoneMasked(),
                account.memberLevel(),
                account.accountStatus(),
                account.balance().setScale(2, RoundingMode.HALF_UP),
                formatTime(account.createdAt()),
                vehicles.size(),
                evVehicles,
                activeOrders,
                settledOrders,
                totalPaid,
                lastPaymentAt,
                vehicles.stream()
                        .map(vehicle -> toCustomerVehicleSummary(vehicle, accessByPlate.get(vehicle.plateNo())))
                        .toList(),
                orders.stream()
                        .limit(6)
                        .map(this::toCustomerOrderSummary)
                        .toList()
        );
    }

    public List<PaymentTransaction> payments(String status, String method, String keyword, String dateFrom, String dateTo) {
        return repository.findPaymentTransactions().stream()
                .filter(payment -> matchesStatus(payment.status(), status))
                .filter(payment -> matchesStatus(payment.method(), method))
                .filter(payment -> matchesKeyword(keyword, payment.paymentNo(), payment.orderNo(), payment.plateNo()))
                .filter(payment -> matchesDateRange(payment.paidAt(), dateFrom, dateTo))
                .toList();
    }

    public List<AdminBillingComponentRow> billingComponents(String orderNo) {
        return repository.findBillingComponentsByOrderNo(orderNo).stream()
                .map(this::toBillingComponentRow)
                .toList();
    }

    public AdminOrderDetail orderDetail(String orderNo) {
        ParkingOrder order = repository.findOrderByNo(orderNo)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found: " + orderNo));

        VehicleProfile vehicle = repository.findVehicleProfiles().stream()
                .filter(item -> item.plateNo().equalsIgnoreCase(order.getPlateNo()))
                .findFirst()
                .orElse(null);
        CustomerAccount account = vehicle == null ? null : repository.findCustomerAccounts().stream()
                .filter(item -> item.ownerId().equalsIgnoreCase(vehicle.ownerId()))
                .findFirst()
                .orElse(null);
        PaymentTransaction payment = repository.findPaymentByOrderNo(orderNo).orElse(null);
        List<AdminBillingComponentRow> billingComponents = billingComponents(orderNo);

        return new AdminOrderDetail(
                order.getOrderNo(),
                order.getPlateNo(),
                order.getSlotId(),
                eventOf(order),
                statusLabel(order),
                formatTime(order.getEntryTime()),
                formatTime(order.getExitTime()),
                order.getDurationMinutes(),
                order.getAmount().setScale(2, RoundingMode.HALF_UP),
                order.getDiscountAmount().setScale(2, RoundingMode.HALF_UP),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                formatTime(order.getPaidAt()),
                toCustomerSummary(account),
                toVehicleSummary(vehicle),
                toPaymentSummary(payment),
                billingComponents
        );
    }

    public List<SystemNodeStatus> systemNodes() {
        return repository.findSystemNodes();
    }

    public AdminOverviewMetrics overviewMetrics() {
        int activeOrders = (int) repository.findOrders().stream()
                .filter(order -> order.getStatus() != OrderStatus.FINISHED)
                .count();
        int settledOrders = (int) repository.findOrders().stream()
                .filter(order -> order.getStatus() == OrderStatus.FINISHED)
                .count();
        int customerCount = repository.findCustomerAccounts().size();
        int vehicleCount = repository.findVehicleProfiles().size();
        int paymentCount = repository.findPaymentTransactions().size();
        int liveAlerts = repository.findAlerts().size();
        long vipTasks = repository.findDispatchQueue().stream().filter(DispatchTask::isVip).count();
        BigDecimal collectedRevenue = repository.findPaymentTransactions().stream()
                .filter(payment -> "SUCCESS".equals(payment.status()))
                .map(PaymentTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new AdminOverviewMetrics(
                activeOrders,
                settledOrders,
                customerCount,
                vehicleCount,
                paymentCount,
                liveAlerts,
                vipTasks,
                collectedRevenue
        );
    }

    public AdminReport buildReport(String query) {
        String normalizedQuery = query == null || query.isBlank()
                ? "Last 7 days VIP dispatch trend"
                : query.trim();
        long vipTasks = repository.findDispatchQueue().stream().filter(DispatchTask::isVip).count();
        long occupiedSlots = repository.findSlots().stream().filter(slot -> slot.getStatus() != SlotStatus.EMPTY).count();
        int liveAlerts = repository.findAlerts().size();
        int queuedTasks = repository.findDispatchQueue().size();
        int realizedRevenue = repository.findPaymentTransactions().stream()
                .filter(payment -> "SUCCESS".equals(payment.status()))
                .map(PaymentTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        String summary = "Live alerts: %d, queued dispatch tasks: %d, occupied slots: %d, collected revenue: %d CNY, VIP tasks: %d."
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

    private AdminCustomerVehicleRow toCustomerVehicleRow(VehicleProfile vehicle, CustomerAccount account, String accessType) {
        return new AdminCustomerVehicleRow(
                account == null ? "UNASSIGNED" : account.ownerId(),
                account == null ? "Unknown owner" : account.ownerName(),
                account == null ? "--" : account.phoneMasked(),
                account == null ? "STANDARD" : account.memberLevel(),
                account == null ? "INCOMPLETE" : account.accountStatus(),
                vehicle.plateNo(),
                vehicle.energyType(),
                vehicle.membershipType(),
                accessType == null ? "NORMAL" : accessType
        );
    }

    private AdminBillingComponentRow toBillingComponentRow(OrderBillingComponent component) {
        return new AdminBillingComponentRow(
                component.componentNo(),
                component.orderNo(),
                component.componentType(),
                component.description(),
                component.amount(),
                component.createdAt().format(ADMIN_TIME)
        );
    }

    private AdminOrderDetail.CustomerSummary toCustomerSummary(CustomerAccount account) {
        if (account == null) {
            return null;
        }
        return new AdminOrderDetail.CustomerSummary(
                account.ownerId(),
                account.ownerName(),
                account.phoneMasked(),
                account.memberLevel(),
                account.accountStatus(),
                account.balance().setScale(2, RoundingMode.HALF_UP),
                formatTime(account.createdAt())
        );
    }

    private AdminOrderDetail.VehicleSummary toVehicleSummary(VehicleProfile vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new AdminOrderDetail.VehicleSummary(
                vehicle.plateNo(),
                vehicle.ownerId(),
                vehicle.vehicleType(),
                vehicle.energyType(),
                vehicle.membershipType(),
                vehicle.defaultAuthStatus(),
                formatTime(vehicle.createdAt())
        );
    }

    private AdminOrderDetail.PaymentSummary toPaymentSummary(PaymentTransaction payment) {
        if (payment == null) {
            return null;
        }
        return new AdminOrderDetail.PaymentSummary(
                payment.paymentNo(),
                payment.amount().setScale(2, RoundingMode.HALF_UP),
                payment.method(),
                payment.status(),
                formatTime(payment.paidAt())
        );
    }

    private AdminCustomerDetail.VehicleSummary toCustomerVehicleSummary(VehicleProfile vehicle, String accessType) {
        return new AdminCustomerDetail.VehicleSummary(
                vehicle.plateNo(),
                vehicle.vehicleType(),
                vehicle.energyType(),
                vehicle.membershipType(),
                vehicle.defaultAuthStatus(),
                accessType == null ? "NORMAL" : accessType,
                formatTime(vehicle.createdAt())
        );
    }

    private AdminCustomerDetail.OrderSummary toCustomerOrderSummary(ParkingOrder order) {
        return new AdminCustomerDetail.OrderSummary(
                order.getOrderNo(),
                order.getPlateNo(),
                statusLabel(order),
                order.getAmount().setScale(2, RoundingMode.HALF_UP),
                order.getPaymentStatus(),
                formatTime(order.getEntryTime())
        );
    }

    private AdminAlertDetail.DeviceEventSummary toAlertDeviceEventSummary(DeviceEvent event) {
        return new AdminAlertDetail.DeviceEventSummary(
                event.eventId(),
                event.deviceType(),
                event.deviceId(),
                event.eventCode(),
                event.severity(),
                event.message(),
                formatTime(event.eventTime()),
                event.acknowledged()
        );
    }

    private AdminAlertDetail.OrderHintSummary toAlertOrderHintSummary(ParkingOrder order) {
        return new AdminAlertDetail.OrderHintSummary(
                order.getOrderNo(),
                order.getPlateNo(),
                statusLabel(order),
                order.getPaymentStatus(),
                order.getSlotId()
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

    private String formatTime(LocalDateTime value) {
        return value == null ? null : value.format(ADMIN_TIME);
    }

    private boolean matchesDateRange(LocalDateTime value, String dateFrom, String dateTo) {
        if (dateFrom == null && dateTo == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        LocalDate targetDate = value.toLocalDate();
        LocalDate from = parseDate(dateFrom);
        LocalDate to = parseDate(dateTo);
        if (from != null && targetDate.isBefore(from)) {
            return false;
        }
        return to == null || !targetDate.isAfter(to);
    }

    private boolean matchesAlertDeviceEvent(AlertEvent alert, DeviceEvent event) {
        String type = alert.type().toLowerCase(Locale.ROOT);
        String content = alert.content().toLowerCase(Locale.ROOT);
        String message = event.message().toLowerCase(Locale.ROOT);
        String deviceType = event.deviceType().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "安全" -> "safety".equals(deviceType) || "camera".equals(deviceType) || message.contains("入侵") || message.contains("estop");
            case "设备" -> "dispatch".equals(deviceType) || "gate".equals(deviceType) || "charger".equals(deviceType) || message.contains("电量");
            case "订单", "计费" -> "order".equals(deviceType) || message.contains("order") || message.contains("结算");
            case "闸机" -> "gate".equals(deviceType) || message.contains("闸机");
            case "充电" -> "charger".equals(deviceType) || message.contains("evse");
            case "视觉" -> "camera".equals(deviceType) || message.contains("camera") || message.contains("码率");
            case "调度" -> "dispatch".equals(deviceType) || message.contains("复核") || message.contains("queue");
            default -> matchesKeyword(content, event.deviceId(), event.eventCode(), event.message());
        };
    }

    private boolean matchesAlertOrder(AlertEvent alert, ParkingOrder order) {
        String type = alert.type().toLowerCase(Locale.ROOT);
        if (matchesKeyword(alert.content(), order.getOrderNo(), order.getPlateNo(), order.getSlotId())) {
            return true;
        }
        return switch (type) {
            case "订单" -> order.getStatus() == OrderStatus.ABNORMAL || order.getStatus() == OrderStatus.PAYING;
            case "计费" -> order.getStatus() == OrderStatus.FINISHED || "PAID".equalsIgnoreCase(order.getPaymentStatus());
            case "调度" -> order.getStatus() == OrderStatus.RETRIEVING || order.getStatus() == OrderStatus.TOUCHING;
            case "安全" -> order.getStatus() != OrderStatus.FINISHED;
            default -> false;
        };
    }

    private String recommendedAction(AlertEvent alert) {
        String type = alert.type();
        String status = alert.status();
        if ("高".equals(alert.level()) || "急停中".equals(status)) {
            return "Lock handoff lane and require operator confirmation";
        }
        return switch (type) {
            case "订单", "计费" -> "Review the related order and settlement callback";
            case "设备", "闸机", "视觉", "充电" -> "Check device telemetry and retry the affected workflow";
            case "调度" -> "Inspect dispatch queue and confirm manual release path";
            default -> "Continue monitoring and log the next operator action";
        };
    }

    private String escalationState(AlertEvent alert) {
        if ("高".equals(alert.level()) || "急停中".equals(alert.status()) || "已升级".equals(alert.status())) {
            return "Immediate response";
        }
        if ("处理中".equals(alert.status()) || "待复核".equals(alert.status()) || "监控中".equals(alert.status())) {
            return "Operator follow-up";
        }
        return "Observe and close";
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private int compareNullable(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private boolean matchesStatus(String value, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return value != null && value.equalsIgnoreCase(expected.trim());
    }

    private boolean matchesKeyword(String keyword, String... values) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(normalized)) {
                return true;
            }
        }
        return false;
    }
}
