package com.parkvision.cps.repository;

import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.domain.admin.SystemNodeStatus;
import com.parkvision.cps.domain.billing.OrderBillingComponent;
import com.parkvision.cps.domain.billing.PaymentTransaction;
import com.parkvision.cps.domain.customer.CustomerAccount;
import com.parkvision.cps.domain.customer.VehicleProfile;
import com.parkvision.cps.domain.device.CameraDevice;
import com.parkvision.cps.domain.device.ChargingStation;
import com.parkvision.cps.domain.device.DeviceEvent;
import com.parkvision.cps.domain.device.GateDevice;
import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.ParkingSlot;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.domain.reservation.Reservation;
import com.parkvision.cps.domain.vision.RecognitionEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "parkvision.persistence", name = "mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcParkVisionRepository implements ParkVisionRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcParkVisionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ParkingSlot> findSlots() {
        return jdbcTemplate.query(
                "select slot_id, layer_name, status from parking_slot order by slot_id",
                (rs, rowNum) -> new ParkingSlot(
                        rs.getString("slot_id"),
                        rs.getString("layer_name"),
                        SlotStatus.valueOf(rs.getString("status"))
                )
        );
    }

    @Override
    public Optional<ParkingSlot> findSlotById(String slotId) {
        return queryOne(
                "select slot_id, layer_name, status from parking_slot where slot_id = ?",
                this::mapSlot,
                slotId
        );
    }

    @Override
    public Optional<ParkingSlot> findFirstAvailableSlot() {
        return queryOne(
                "select slot_id, layer_name, status from parking_slot where status = ? order by slot_id limit 1",
                this::mapSlot,
                SlotStatus.EMPTY.name()
        );
    }

    @Override
    public Optional<ParkingSlot> findFirstDeepOccupiedSlot() {
        return queryOne(
                "select slot_id, layer_name, status from parking_slot where layer_name = ? and status = ? order by slot_id limit 1",
                this::mapSlot,
                "Deep",
                SlotStatus.OCCUPIED.name()
        );
    }

    @Override
    public ParkingSlot saveSlot(ParkingSlot slot) {
        upsert(
                "update parking_slot set layer_name = ?, status = ? where slot_id = ?",
                "insert into parking_slot (slot_id, layer_name, status) values (?, ?, ?)",
                new Object[]{slot.getLayer(), slot.getStatus().name(), slot.getId()},
                new Object[]{slot.getId(), slot.getLayer(), slot.getStatus().name()}
        );
        return slot;
    }

    @Override
    public List<ParkingOrder> findOrders() {
        return jdbcTemplate.query(
                "select order_no, plate_no, slot_id, entry_time, exit_time, status, amount, payment_status, payment_method, paid_at, duration_minutes, discount_amount from parking_order order by entry_time desc, order_no desc",
                this::mapOrder
        );
    }

    @Override
    public Optional<ParkingOrder> findOrderByNo(String orderNo) {
        return queryOne(
                "select order_no, plate_no, slot_id, entry_time, exit_time, status, amount, payment_status, payment_method, paid_at, duration_minutes, discount_amount from parking_order where order_no = ?",
                this::mapOrder,
                orderNo
        );
    }

    @Override
    public ParkingOrder saveOrder(ParkingOrder order) {
        upsert(
                "update parking_order set plate_no = ?, slot_id = ?, entry_time = ?, exit_time = ?, status = ?, amount = ?, payment_status = ?, payment_method = ?, paid_at = ?, duration_minutes = ?, discount_amount = ? where order_no = ?",
                "insert into parking_order (order_no, plate_no, slot_id, entry_time, exit_time, status, amount, payment_status, payment_method, paid_at, duration_minutes, discount_amount) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        order.getPlateNo(),
                        order.getSlotId(),
                        Timestamp.valueOf(order.getEntryTime()),
                        timestampOrNull(order.getExitTime()),
                        order.getStatus().name(),
                        order.getAmount(),
                        order.getPaymentStatus(),
                        order.getPaymentMethod(),
                        timestampOrNull(order.getPaidAt()),
                        order.getDurationMinutes(),
                        order.getDiscountAmount(),
                        order.getOrderNo()
                },
                new Object[]{
                        order.getOrderNo(),
                        order.getPlateNo(),
                        order.getSlotId(),
                        Timestamp.valueOf(order.getEntryTime()),
                        timestampOrNull(order.getExitTime()),
                        order.getStatus().name(),
                        order.getAmount(),
                        order.getPaymentStatus(),
                        order.getPaymentMethod(),
                        timestampOrNull(order.getPaidAt()),
                        order.getDurationMinutes(),
                        order.getDiscountAmount()
                }
        );
        return order;
    }

    @Override
    public List<Reservation> findReservations() {
        return jdbcTemplate.query(
                "select id, plate_no, phone, energy_type, slot_id, status, owner_id, order_no, created_at, expires_at from reservation order by created_at desc",
                this::mapReservation
        );
    }

    @Override
    public Optional<Reservation> findReservationById(String id) {
        return queryOne(
                "select id, plate_no, phone, energy_type, slot_id, status, owner_id, order_no, created_at, expires_at from reservation where id = ?",
                this::mapReservation,
                id
        );
    }

    @Override
    public Reservation saveReservation(Reservation reservation) {
        upsert(
                "update reservation set plate_no = ?, phone = ?, energy_type = ?, slot_id = ?, status = ?, owner_id = ?, order_no = ?, created_at = ?, expires_at = ? where id = ?",
                "insert into reservation (id, plate_no, phone, energy_type, slot_id, status, owner_id, order_no, created_at, expires_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        reservation.plateNo(),
                        reservation.phone(),
                        reservation.energyType(),
                        reservation.slotId(),
                        reservation.status(),
                        reservation.ownerId(),
                        reservation.orderNo(),
                        Timestamp.valueOf(reservation.createdAt()),
                        Timestamp.valueOf(reservation.expiresAt()),
                        reservation.id()
                },
                new Object[]{
                        reservation.id(),
                        reservation.plateNo(),
                        reservation.phone(),
                        reservation.energyType(),
                        reservation.slotId(),
                        reservation.status(),
                        reservation.ownerId(),
                        reservation.orderNo(),
                        Timestamp.valueOf(reservation.createdAt()),
                        Timestamp.valueOf(reservation.expiresAt())
                }
        );
        return reservation;
    }

    private Reservation mapReservation(ResultSet rs, int rowNum) throws SQLException {
        return new Reservation(
                rs.getString("id"),
                rs.getString("plate_no"),
                rs.getString("phone"),
                rs.getString("energy_type"),
                rs.getString("slot_id"),
                rs.getString("status"),
                rs.getString("owner_id"),
                rs.getString("order_no"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("expires_at").toLocalDateTime()
        );
    }

    @Override
    public List<CustomerAccount> findCustomerAccounts() {
        return jdbcTemplate.query(
                "select owner_id, owner_name, phone_masked, member_level, account_status, balance, created_at from customer_account order by owner_id",
                this::mapCustomerAccount
        );
    }

    @Override
    public CustomerAccount saveCustomerAccount(CustomerAccount account) {
        upsert(
                "update customer_account set owner_name = ?, phone_masked = ?, member_level = ?, account_status = ?, balance = ?, created_at = ? where owner_id = ?",
                "insert into customer_account (owner_id, owner_name, phone_masked, member_level, account_status, balance, created_at) values (?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        account.ownerName(),
                        account.phoneMasked(),
                        account.memberLevel(),
                        account.accountStatus(),
                        account.balance(),
                        Timestamp.valueOf(account.createdAt()),
                        account.ownerId()
                },
                new Object[]{
                        account.ownerId(),
                        account.ownerName(),
                        account.phoneMasked(),
                        account.memberLevel(),
                        account.accountStatus(),
                        account.balance(),
                        Timestamp.valueOf(account.createdAt())
                }
        );
        return account;
    }

    @Override
    public List<VehicleProfile> findVehicleProfiles() {
        return jdbcTemplate.query(
                "select plate_no, owner_id, vehicle_type, energy_type, membership_type, default_auth_status, created_at from vehicle_profile order by plate_no",
                this::mapVehicleProfile
        );
    }

    @Override
    public VehicleProfile saveVehicleProfile(VehicleProfile vehicle) {
        upsert(
                "update vehicle_profile set owner_id = ?, vehicle_type = ?, energy_type = ?, membership_type = ?, default_auth_status = ?, created_at = ? where plate_no = ?",
                "insert into vehicle_profile (plate_no, owner_id, vehicle_type, energy_type, membership_type, default_auth_status, created_at) values (?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        vehicle.ownerId(),
                        vehicle.vehicleType(),
                        vehicle.energyType(),
                        vehicle.membershipType(),
                        vehicle.defaultAuthStatus(),
                        Timestamp.valueOf(vehicle.createdAt()),
                        vehicle.plateNo()
                },
                new Object[]{
                        vehicle.plateNo(),
                        vehicle.ownerId(),
                        vehicle.vehicleType(),
                        vehicle.energyType(),
                        vehicle.membershipType(),
                        vehicle.defaultAuthStatus(),
                        Timestamp.valueOf(vehicle.createdAt())
                }
        );
        return vehicle;
    }

    @Override
    public void deleteVehicleProfile(String plateNo) {
        jdbcTemplate.update("delete from vehicle_profile where plate_no = ?", plateNo);
    }

    @Override
    public List<PaymentTransaction> findPaymentTransactions() {
        return jdbcTemplate.query(
                "select payment_no, order_no, plate_no, amount, method, status, paid_at from payment_transaction order by paid_at desc, payment_no desc",
                this::mapPaymentTransaction
        );
    }

    @Override
    public Optional<PaymentTransaction> findPaymentByOrderNo(String orderNo) {
        return queryOne(
                "select payment_no, order_no, plate_no, amount, method, status, paid_at from payment_transaction where order_no = ? order by paid_at desc limit 1",
                this::mapPaymentTransaction,
                orderNo
        );
    }

    @Override
    public PaymentTransaction savePaymentTransaction(PaymentTransaction payment) {
        upsert(
                "update payment_transaction set order_no = ?, plate_no = ?, amount = ?, method = ?, status = ?, paid_at = ? where payment_no = ?",
                "insert into payment_transaction (payment_no, order_no, plate_no, amount, method, status, paid_at) values (?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        payment.orderNo(),
                        payment.plateNo(),
                        payment.amount(),
                        payment.method(),
                        payment.status(),
                        Timestamp.valueOf(payment.paidAt()),
                        payment.paymentNo()
                },
                new Object[]{
                        payment.paymentNo(),
                        payment.orderNo(),
                        payment.plateNo(),
                        payment.amount(),
                        payment.method(),
                        payment.status(),
                        Timestamp.valueOf(payment.paidAt())
                }
        );
        return payment;
    }

    @Override
    public List<OrderBillingComponent> findBillingComponentsByOrderNo(String orderNo) {
        return jdbcTemplate.query(
                "select component_no, order_no, component_type, description, amount, created_at from order_billing_component where order_no = ? order by component_no",
                this::mapBillingComponent,
                orderNo
        );
    }

    @Override
    public OrderBillingComponent saveBillingComponent(OrderBillingComponent component) {
        upsert(
                "update order_billing_component set order_no = ?, component_type = ?, description = ?, amount = ?, created_at = ? where component_no = ?",
                "insert into order_billing_component (component_no, order_no, component_type, description, amount, created_at) values (?, ?, ?, ?, ?, ?)",
                new Object[]{
                        component.orderNo(),
                        component.componentType(),
                        component.description(),
                        component.amount(),
                        Timestamp.valueOf(component.createdAt()),
                        component.componentNo()
                },
                new Object[]{
                        component.componentNo(),
                        component.orderNo(),
                        component.componentType(),
                        component.description(),
                        component.amount(),
                        Timestamp.valueOf(component.createdAt())
                }
        );
        return component;
    }

    @Override
    public List<AlertEvent> findAlerts() {
        return jdbcTemplate.query(
                "select alert_no, alert_type, content, status, level_name from alert_event order by alert_no desc",
                (rs, rowNum) -> new AlertEvent(
                        rs.getString("alert_no"),
                        rs.getString("alert_type"),
                        rs.getString("content"),
                        rs.getString("status"),
                        rs.getString("level_name")
                )
        );
    }

    @Override
    public AlertEvent saveAlert(AlertEvent alert) {
        upsert(
                "update alert_event set alert_type = ?, content = ?, status = ?, level_name = ? where alert_no = ?",
                "insert into alert_event (alert_no, alert_type, content, status, level_name) values (?, ?, ?, ?, ?)",
                new Object[]{alert.type(), alert.content(), alert.status(), alert.level(), alert.alertNo()},
                new Object[]{alert.alertNo(), alert.type(), alert.content(), alert.status(), alert.level()}
        );
        return alert;
    }

    private static final String PRICING_COLUMNS =
            "id, rule_name, vehicle_type, free_minutes, first_hour_fee, hourly_fee, daily_cap, peak_start_hour, peak_end_hour, peak_multiplier, status";

    private PricingRule mapPricingRule(ResultSet rs, int rowNum) throws SQLException {
        return new PricingRule(
                rs.getString("id"),
                rs.getString("rule_name"),
                rs.getString("vehicle_type"),
                rs.getInt("free_minutes"),
                rs.getBigDecimal("first_hour_fee"),
                rs.getBigDecimal("hourly_fee"),
                rs.getBigDecimal("daily_cap"),
                rs.getInt("peak_start_hour"),
                rs.getInt("peak_end_hour"),
                rs.getBigDecimal("peak_multiplier"),
                rs.getString("status")
        );
    }

    @Override
    public List<PricingRule> findPricingRules() {
        return jdbcTemplate.query(
                "select " + PRICING_COLUMNS + " from pricing_rule order by rule_name",
                this::mapPricingRule
        );
    }

    @Override
    public Optional<PricingRule> findPricingRuleById(String id) {
        return queryOne(
                "select " + PRICING_COLUMNS + " from pricing_rule where id = ?",
                this::mapPricingRule,
                id
        );
    }

    @Override
    public PricingRule savePricingRule(PricingRule rule) {
        upsert(
                "update pricing_rule set rule_name = ?, vehicle_type = ?, free_minutes = ?, first_hour_fee = ?, hourly_fee = ?, daily_cap = ?, peak_start_hour = ?, peak_end_hour = ?, peak_multiplier = ?, status = ? where id = ?",
                "insert into pricing_rule (rule_name, vehicle_type, free_minutes, first_hour_fee, hourly_fee, daily_cap, peak_start_hour, peak_end_hour, peak_multiplier, status, id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        rule.name(), rule.vehicleType(), rule.freeMinutes(), rule.firstHourFee(), rule.hourlyFee(),
                        rule.dailyCap(), rule.peakStartHour(), rule.peakEndHour(), rule.peakMultiplier(), rule.status(), rule.id()
                },
                new Object[]{
                        rule.name(), rule.vehicleType(), rule.freeMinutes(), rule.firstHourFee(), rule.hourlyFee(),
                        rule.dailyCap(), rule.peakStartHour(), rule.peakEndHour(), rule.peakMultiplier(), rule.status(), rule.id()
                }
        );
        return rule;
    }

    @Override
    public void deletePricingRule(String id) {
        jdbcTemplate.update("delete from pricing_rule where id = ?", id);
    }

    @Override
    public List<AccessListItem> findAccessList() {
        return jdbcTemplate.query(
                "select plate_no, list_type, user_type, valid_until, remark from access_list_item order by plate_no",
                (rs, rowNum) -> new AccessListItem(
                        rs.getString("plate_no"),
                        rs.getString("list_type"),
                        rs.getString("user_type"),
                        rs.getString("valid_until"),
                        rs.getString("remark")
                )
        );
    }

    @Override
    public Optional<AccessListItem> findAccessListItem(String plateNo) {
        if (plateNo == null) {
            return Optional.empty();
        }
        return queryOne(
                "select plate_no, list_type, user_type, valid_until, remark from access_list_item where upper(plate_no) = upper(?)",
                (rs, rowNum) -> new AccessListItem(
                        rs.getString("plate_no"),
                        rs.getString("list_type"),
                        rs.getString("user_type"),
                        rs.getString("valid_until"),
                        rs.getString("remark")
                ),
                plateNo
        );
    }

    @Override
    public List<RecognitionEvent> findRecognitionEvents() {
        return jdbcTemplate.query(
                "select id, camera_id, plate_no, confidence, energy_type, list_type, decision, reason, order_no, intrusion, created_at "
                        + "from recognition_event order by created_at desc",
                this::mapRecognitionEvent
        );
    }

    @Override
    public RecognitionEvent saveRecognitionEvent(RecognitionEvent event) {
        upsert(
                "update recognition_event set camera_id = ?, plate_no = ?, confidence = ?, energy_type = ?, list_type = ?, decision = ?, reason = ?, order_no = ?, intrusion = ?, created_at = ? where id = ?",
                "insert into recognition_event (id, camera_id, plate_no, confidence, energy_type, list_type, decision, reason, order_no, intrusion, created_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        event.cameraId(),
                        event.plateNo(),
                        event.confidence(),
                        event.energyType(),
                        event.listType(),
                        event.decision(),
                        event.reason(),
                        event.orderNo(),
                        event.intrusion(),
                        Timestamp.valueOf(event.createdAt()),
                        event.id()
                },
                new Object[]{
                        event.id(),
                        event.cameraId(),
                        event.plateNo(),
                        event.confidence(),
                        event.energyType(),
                        event.listType(),
                        event.decision(),
                        event.reason(),
                        event.orderNo(),
                        event.intrusion(),
                        Timestamp.valueOf(event.createdAt())
                }
        );
        return event;
    }

    private RecognitionEvent mapRecognitionEvent(ResultSet rs, int rowNum) throws SQLException {
        return new RecognitionEvent(
                rs.getString("id"),
                rs.getString("camera_id"),
                rs.getString("plate_no"),
                rs.getDouble("confidence"),
                rs.getString("energy_type"),
                rs.getString("list_type"),
                rs.getString("decision"),
                rs.getString("reason"),
                rs.getString("order_no"),
                rs.getBoolean("intrusion"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    @Override
    public List<SystemNodeStatus> findSystemNodes() {
        return jdbcTemplate.query(
                "select node_name, latency, detail, level_name from system_node_status order by node_name",
                (rs, rowNum) -> new SystemNodeStatus(
                        rs.getString("node_name"),
                        rs.getString("latency"),
                        rs.getString("detail"),
                        rs.getString("level_name")
                )
        );
    }

    @Override
    public SystemNodeStatus saveSystemNode(SystemNodeStatus node) {
        upsert(
                "update system_node_status set latency = ?, detail = ?, level_name = ? where node_name = ?",
                "insert into system_node_status (node_name, latency, detail, level_name) values (?, ?, ?, ?)",
                new Object[]{node.latency(), node.detail(), node.level(), node.name()},
                new Object[]{node.name(), node.latency(), node.detail(), node.level()}
        );
        return node;
    }

    @Override
    public List<com.parkvision.cps.domain.admin.AuditLog> findAuditLogs(int limit) {
        return jdbcTemplate.query(
                "select id, username, role, method, path, status, ip, created_at from audit_log order by id desc limit ?",
                (rs, rowNum) -> new com.parkvision.cps.domain.admin.AuditLog(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("method"),
                        rs.getString("path"),
                        rs.getInt("status"),
                        rs.getString("ip"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                limit
        );
    }

    @Override
    public com.parkvision.cps.domain.admin.AuditLog saveAuditLog(com.parkvision.cps.domain.admin.AuditLog log) {
        jdbcTemplate.update(
                "insert into audit_log (username, role, method, path, status, ip, created_at) values (?, ?, ?, ?, ?, ?, ?)",
                log.username(),
                log.role(),
                log.method(),
                log.path(),
                log.status(),
                log.ip(),
                Timestamp.valueOf(log.createdAt() == null ? LocalDateTime.now() : log.createdAt())
        );
        return log;
    }

    @Override
    public List<AgvUnit> findAgvUnits() {
        return jdbcTemplate.query(
                "select agv_id, x_pos, y_pos, loaded, task, battery_pct, mode_name, velocity_mps, last_command from agv_unit order by agv_id",
                this::mapAgv
        );
    }

    @Override
    public Optional<AgvUnit> findAgvById(String agvId) {
        return queryOne(
                "select agv_id, x_pos, y_pos, loaded, task, battery_pct, mode_name, velocity_mps, last_command from agv_unit where agv_id = ?",
                this::mapAgv,
                agvId
        );
    }

    @Override
    public AgvUnit saveAgvUnit(AgvUnit agv) {
        upsert(
                "update agv_unit set x_pos = ?, y_pos = ?, loaded = ?, task = ?, battery_pct = ?, mode_name = ?, velocity_mps = ?, last_command = ? where agv_id = ?",
                "insert into agv_unit (agv_id, x_pos, y_pos, loaded, task, battery_pct, mode_name, velocity_mps, last_command) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        agv.getX(),
                        agv.getY(),
                        agv.isLoaded(),
                        agv.getTask(),
                        agv.getBatteryPct(),
                        agv.getMode(),
                        agv.getVelocityMps(),
                        agv.getLastCommand(),
                        agv.getId()
                },
                new Object[]{
                        agv.getId(),
                        agv.getX(),
                        agv.getY(),
                        agv.isLoaded(),
                        agv.getTask(),
                        agv.getBatteryPct(),
                        agv.getMode(),
                        agv.getVelocityMps(),
                        agv.getLastCommand()
                }
        );
        return agv;
    }

    @Override
    public List<DispatchTask> findDispatchQueue() {
        return jdbcTemplate.query(
                "select task_id, plate_no, task_type, tag_name, wait_time, vip, status, progress, slot_id, agv_id, created_at, updated_at "
                        + "from dispatch_task order by case status when 'IN_PROGRESS' then 0 when 'QUEUED' then 1 else 2 end, vip desc, created_at desc, task_id desc",
                this::mapDispatchTask
        );
    }

    private DispatchTask mapDispatchTask(ResultSet rs, int rowNum) throws SQLException {
        return new DispatchTask(
                rs.getLong("task_id"),
                rs.getString("plate_no"),
                rs.getString("task_type"),
                rs.getString("tag_name"),
                rs.getString("wait_time"),
                rs.getBoolean("vip"),
                rs.getString("status"),
                rs.getInt("progress"),
                rs.getString("slot_id"),
                rs.getString("agv_id"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    @Override
    public DispatchTask enqueueDispatchTask(DispatchTask task) {
        LocalDateTime now = LocalDateTime.now();
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into dispatch_task (plate_no, task_type, tag_name, wait_time, vip, status, progress, slot_id, agv_id, created_at, updated_at) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{"task_id"});
            ps.setString(1, task.getPlateNo());
            ps.setString(2, task.getType());
            ps.setString(3, task.getTag());
            ps.setString(4, task.getWait());
            ps.setBoolean(5, task.isVip());
            ps.setString(6, task.getStatus());
            ps.setInt(7, task.getProgress());
            ps.setString(8, task.getSlotId());
            ps.setString(9, task.getAgvId());
            ps.setTimestamp(10, Timestamp.valueOf(now));
            ps.setTimestamp(11, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            task.setId(key.longValue());
        }
        return task;
    }

    @Override
    public DispatchTask saveDispatchTask(DispatchTask task) {
        if (task.getId() == null) {
            return enqueueDispatchTask(task);
        }
        task.setUpdatedAt(LocalDateTime.now());
        jdbcTemplate.update(
                "update dispatch_task set plate_no = ?, task_type = ?, tag_name = ?, wait_time = ?, vip = ?, status = ?, progress = ?, slot_id = ?, agv_id = ?, updated_at = ? where task_id = ?",
                task.getPlateNo(),
                task.getType(),
                task.getTag(),
                task.getWait(),
                task.isVip(),
                task.getStatus(),
                task.getProgress(),
                task.getSlotId(),
                task.getAgvId(),
                Timestamp.valueOf(task.getUpdatedAt()),
                task.getId()
        );
        return task;
    }

    @Override
    public List<CameraDevice> findCameraDevices() {
        return jdbcTemplate.query(
                "select camera_id, profile_name, codec, stream_url, fps, bitrate_kbps, status, last_plate, last_seen, tamper_alarm, intrusion_state, detail from vision_camera order by camera_id",
                (rs, rowNum) -> new CameraDevice(
                        rs.getString("camera_id"),
                        rs.getString("profile_name"),
                        rs.getString("codec"),
                        rs.getString("stream_url"),
                        rs.getInt("fps"),
                        rs.getInt("bitrate_kbps"),
                        rs.getString("status"),
                        rs.getString("last_plate"),
                        rs.getTimestamp("last_seen").toLocalDateTime(),
                        rs.getBoolean("tamper_alarm"),
                        rs.getBoolean("intrusion_state"),
                        rs.getString("detail")
                )
        );
    }

    @Override
    public Optional<CameraDevice> findCameraDeviceById(String cameraId) {
        return queryOne(
                "select camera_id, profile_name, codec, stream_url, fps, bitrate_kbps, status, last_plate, last_seen, tamper_alarm, intrusion_state, detail from vision_camera where camera_id = ?",
                (rs, rowNum) -> new CameraDevice(
                        rs.getString("camera_id"),
                        rs.getString("profile_name"),
                        rs.getString("codec"),
                        rs.getString("stream_url"),
                        rs.getInt("fps"),
                        rs.getInt("bitrate_kbps"),
                        rs.getString("status"),
                        rs.getString("last_plate"),
                        rs.getTimestamp("last_seen").toLocalDateTime(),
                        rs.getBoolean("tamper_alarm"),
                        rs.getBoolean("intrusion_state"),
                        rs.getString("detail")
                ),
                cameraId
        );
    }

    @Override
    public CameraDevice saveCameraDevice(CameraDevice camera) {
        upsert(
                "update vision_camera set profile_name = ?, codec = ?, stream_url = ?, fps = ?, bitrate_kbps = ?, status = ?, last_plate = ?, last_seen = ?, tamper_alarm = ?, intrusion_state = ?, detail = ? where camera_id = ?",
                "insert into vision_camera (camera_id, profile_name, codec, stream_url, fps, bitrate_kbps, status, last_plate, last_seen, tamper_alarm, intrusion_state, detail) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        camera.profile(),
                        camera.codec(),
                        camera.streamUrl(),
                        camera.fps(),
                        camera.bitrateKbps(),
                        camera.status(),
                        camera.lastPlate(),
                        Timestamp.valueOf(camera.lastSeen()),
                        camera.tamperAlarm(),
                        camera.intrusionState(),
                        camera.detail(),
                        camera.cameraId()
                },
                new Object[]{
                        camera.cameraId(),
                        camera.profile(),
                        camera.codec(),
                        camera.streamUrl(),
                        camera.fps(),
                        camera.bitrateKbps(),
                        camera.status(),
                        camera.lastPlate(),
                        Timestamp.valueOf(camera.lastSeen()),
                        camera.tamperAlarm(),
                        camera.intrusionState(),
                        camera.detail()
                }
        );
        return camera;
    }

    @Override
    public List<GateDevice> findGateDevices() {
        return jdbcTemplate.query(
                "select gate_id, protocol, endpoint, coil_address, queue_depth, gate_state, loop_occupied, estop_armed, last_decision, last_seen, detail from gate_device order by gate_id",
                (rs, rowNum) -> new GateDevice(
                        rs.getString("gate_id"),
                        rs.getString("protocol"),
                        rs.getString("endpoint"),
                        rs.getString("coil_address"),
                        rs.getInt("queue_depth"),
                        rs.getString("gate_state"),
                        rs.getBoolean("loop_occupied"),
                        rs.getBoolean("estop_armed"),
                        rs.getString("last_decision"),
                        rs.getTimestamp("last_seen").toLocalDateTime(),
                        rs.getString("detail")
                )
        );
    }

    @Override
    public Optional<GateDevice> findGateDeviceById(String gateId) {
        return queryOne(
                "select gate_id, protocol, endpoint, coil_address, queue_depth, gate_state, loop_occupied, estop_armed, last_decision, last_seen, detail from gate_device where gate_id = ?",
                (rs, rowNum) -> new GateDevice(
                        rs.getString("gate_id"),
                        rs.getString("protocol"),
                        rs.getString("endpoint"),
                        rs.getString("coil_address"),
                        rs.getInt("queue_depth"),
                        rs.getString("gate_state"),
                        rs.getBoolean("loop_occupied"),
                        rs.getBoolean("estop_armed"),
                        rs.getString("last_decision"),
                        rs.getTimestamp("last_seen").toLocalDateTime(),
                        rs.getString("detail")
                ),
                gateId
        );
    }

    @Override
    public GateDevice saveGateDevice(GateDevice gate) {
        upsert(
                "update gate_device set protocol = ?, endpoint = ?, coil_address = ?, queue_depth = ?, gate_state = ?, loop_occupied = ?, estop_armed = ?, last_decision = ?, last_seen = ?, detail = ? where gate_id = ?",
                "insert into gate_device (gate_id, protocol, endpoint, coil_address, queue_depth, gate_state, loop_occupied, estop_armed, last_decision, last_seen, detail) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        gate.protocol(),
                        gate.endpoint(),
                        gate.coilAddress(),
                        gate.queueDepth(),
                        gate.gateState(),
                        gate.loopOccupied(),
                        gate.estopArmed(),
                        gate.lastDecision(),
                        Timestamp.valueOf(gate.lastSeen()),
                        gate.detail(),
                        gate.gateId()
                },
                new Object[]{
                        gate.gateId(),
                        gate.protocol(),
                        gate.endpoint(),
                        gate.coilAddress(),
                        gate.queueDepth(),
                        gate.gateState(),
                        gate.loopOccupied(),
                        gate.estopArmed(),
                        gate.lastDecision(),
                        Timestamp.valueOf(gate.lastSeen()),
                        gate.detail()
                }
        );
        return gate;
    }

    @Override
    public List<ChargingStation> findChargingStations() {
        return jdbcTemplate.query(
                "select charger_id, protocol, endpoint, connector_status, power_kw, session_kwh, vehicle_plate, auth_status, last_seen, detail from charging_station order by charger_id",
                (rs, rowNum) -> new ChargingStation(
                        rs.getString("charger_id"),
                        rs.getString("protocol"),
                        rs.getString("endpoint"),
                        rs.getString("connector_status"),
                        rs.getBigDecimal("power_kw"),
                        rs.getBigDecimal("session_kwh"),
                        rs.getString("vehicle_plate"),
                        rs.getString("auth_status"),
                        rs.getTimestamp("last_seen").toLocalDateTime(),
                        rs.getString("detail")
                )
        );
    }

    @Override
    public Optional<ChargingStation> findChargingStationById(String chargerId) {
        return queryOne(
                "select charger_id, protocol, endpoint, connector_status, power_kw, session_kwh, vehicle_plate, auth_status, last_seen, detail from charging_station where charger_id = ?",
                (rs, rowNum) -> new ChargingStation(
                        rs.getString("charger_id"),
                        rs.getString("protocol"),
                        rs.getString("endpoint"),
                        rs.getString("connector_status"),
                        rs.getBigDecimal("power_kw"),
                        rs.getBigDecimal("session_kwh"),
                        rs.getString("vehicle_plate"),
                        rs.getString("auth_status"),
                        rs.getTimestamp("last_seen").toLocalDateTime(),
                        rs.getString("detail")
                ),
                chargerId
        );
    }

    @Override
    public ChargingStation saveChargingStation(ChargingStation station) {
        upsert(
                "update charging_station set protocol = ?, endpoint = ?, connector_status = ?, power_kw = ?, session_kwh = ?, vehicle_plate = ?, auth_status = ?, last_seen = ?, detail = ? where charger_id = ?",
                "insert into charging_station (charger_id, protocol, endpoint, connector_status, power_kw, session_kwh, vehicle_plate, auth_status, last_seen, detail) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        station.protocol(),
                        station.endpoint(),
                        station.connectorStatus(),
                        station.powerKw(),
                        station.sessionKwh(),
                        station.vehiclePlate(),
                        station.authStatus(),
                        Timestamp.valueOf(station.lastSeen()),
                        station.detail(),
                        station.chargerId()
                },
                new Object[]{
                        station.chargerId(),
                        station.protocol(),
                        station.endpoint(),
                        station.connectorStatus(),
                        station.powerKw(),
                        station.sessionKwh(),
                        station.vehiclePlate(),
                        station.authStatus(),
                        Timestamp.valueOf(station.lastSeen()),
                        station.detail()
                }
        );
        return station;
    }

    @Override
    public List<DeviceEvent> findDeviceEvents() {
        return jdbcTemplate.query(
                "select event_id, device_type, device_id, event_code, severity, message, event_time, acknowledged from device_event order by event_time desc, event_id desc",
                (rs, rowNum) -> new DeviceEvent(
                        rs.getString("event_id"),
                        rs.getString("device_type"),
                        rs.getString("device_id"),
                        rs.getString("event_code"),
                        rs.getString("severity"),
                        rs.getString("message"),
                        rs.getTimestamp("event_time").toLocalDateTime(),
                        rs.getBoolean("acknowledged")
                )
        );
    }

    @Override
    public DeviceEvent saveDeviceEvent(DeviceEvent event) {
        upsert(
                "update device_event set device_type = ?, device_id = ?, event_code = ?, severity = ?, message = ?, event_time = ?, acknowledged = ? where event_id = ?",
                "insert into device_event (event_id, device_type, device_id, event_code, severity, message, event_time, acknowledged) values (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        event.deviceType(),
                        event.deviceId(),
                        event.eventCode(),
                        event.severity(),
                        event.message(),
                        Timestamp.valueOf(event.eventTime()),
                        event.acknowledged(),
                        event.eventId()
                },
                new Object[]{
                        event.eventId(),
                        event.deviceType(),
                        event.deviceId(),
                        event.eventCode(),
                        event.severity(),
                        event.message(),
                        Timestamp.valueOf(event.eventTime()),
                        event.acknowledged()
                }
        );
        return event;
    }

    private ParkingSlot mapSlot(ResultSet rs, int rowNum) throws SQLException {
        return new ParkingSlot(
                rs.getString("slot_id"),
                rs.getString("layer_name"),
                SlotStatus.valueOf(rs.getString("status"))
        );
    }

    private ParkingOrder mapOrder(ResultSet rs, int rowNum) throws SQLException {
        Timestamp exitTime = rs.getTimestamp("exit_time");
        Timestamp paidAt = rs.getTimestamp("paid_at");
        int durationMinutes = rs.getInt("duration_minutes");
        boolean durationWasNull = rs.wasNull();
        return new ParkingOrder(
                rs.getString("order_no"),
                rs.getString("plate_no"),
                rs.getString("slot_id"),
                rs.getTimestamp("entry_time").toLocalDateTime(),
                exitTime == null ? null : exitTime.toLocalDateTime(),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount"),
                rs.getString("payment_status"),
                rs.getString("payment_method"),
                paidAt == null ? null : paidAt.toLocalDateTime(),
                durationWasNull ? null : durationMinutes,
                rs.getBigDecimal("discount_amount")
        );
    }

    private CustomerAccount mapCustomerAccount(ResultSet rs, int rowNum) throws SQLException {
        return new CustomerAccount(
                rs.getString("owner_id"),
                rs.getString("owner_name"),
                rs.getString("phone_masked"),
                rs.getString("member_level"),
                rs.getString("account_status"),
                rs.getBigDecimal("balance"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private VehicleProfile mapVehicleProfile(ResultSet rs, int rowNum) throws SQLException {
        return new VehicleProfile(
                rs.getString("plate_no"),
                rs.getString("owner_id"),
                rs.getString("vehicle_type"),
                rs.getString("energy_type"),
                rs.getString("membership_type"),
                rs.getString("default_auth_status"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private PaymentTransaction mapPaymentTransaction(ResultSet rs, int rowNum) throws SQLException {
        return new PaymentTransaction(
                rs.getString("payment_no"),
                rs.getString("order_no"),
                rs.getString("plate_no"),
                rs.getBigDecimal("amount"),
                rs.getString("method"),
                rs.getString("status"),
                rs.getTimestamp("paid_at").toLocalDateTime()
        );
    }

    private OrderBillingComponent mapBillingComponent(ResultSet rs, int rowNum) throws SQLException {
        return new OrderBillingComponent(
                rs.getString("component_no"),
                rs.getString("order_no"),
                rs.getString("component_type"),
                rs.getString("description"),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private AgvUnit mapAgv(ResultSet rs, int rowNum) throws SQLException {
        return new AgvUnit(
                rs.getString("agv_id"),
                rs.getInt("x_pos"),
                rs.getInt("y_pos"),
                rs.getBoolean("loaded"),
                rs.getString("task"),
                rs.getInt("battery_pct"),
                rs.getString("mode_name"),
                rs.getDouble("velocity_mps"),
                rs.getString("last_command")
        );
    }

    private <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... args) {
        List<T> results = jdbcTemplate.query(sql, mapper, args);
        return results.stream().findFirst();
    }

    private void upsert(String updateSql, String insertSql, Object[] updateArgs, Object[] insertArgs) {
        int updated = jdbcTemplate.update(updateSql, updateArgs);
        if (updated == 0) {
            jdbcTemplate.update(insertSql, insertArgs);
        }
    }

    private Timestamp timestampOrNull(LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime);
    }

}
