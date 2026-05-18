package com.parkvision.cps.repository;

import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.domain.admin.SystemNodeStatus;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
                "select order_no, plate_no, slot_id, entry_time, status, amount from parking_order order by entry_time desc, order_no desc",
                this::mapOrder
        );
    }

    @Override
    public Optional<ParkingOrder> findOrderByNo(String orderNo) {
        return queryOne(
                "select order_no, plate_no, slot_id, entry_time, status, amount from parking_order where order_no = ?",
                this::mapOrder,
                orderNo
        );
    }

    @Override
    public ParkingOrder saveOrder(ParkingOrder order) {
        upsert(
                "update parking_order set plate_no = ?, slot_id = ?, entry_time = ?, status = ?, amount = ? where order_no = ?",
                "insert into parking_order (order_no, plate_no, slot_id, entry_time, status, amount) values (?, ?, ?, ?, ?, ?)",
                new Object[]{
                        order.getPlateNo(),
                        order.getSlotId(),
                        Timestamp.valueOf(order.getEntryTime()),
                        order.getStatus().name(),
                        order.getAmount(),
                        order.getOrderNo()
                },
                new Object[]{
                        order.getOrderNo(),
                        order.getPlateNo(),
                        order.getSlotId(),
                        Timestamp.valueOf(order.getEntryTime()),
                        order.getStatus().name(),
                        order.getAmount()
                }
        );
        return order;
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

    @Override
    public List<PricingRule> findPricingRules() {
        return jdbcTemplate.query(
                "select rule_name, time_range, method, extra_policy, status from pricing_rule order by rule_name",
                (rs, rowNum) -> new PricingRule(
                        rs.getString("rule_name"),
                        rs.getString("time_range"),
                        rs.getString("method"),
                        rs.getString("extra_policy"),
                        rs.getString("status")
                )
        );
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
                "select plate_no, task_type, tag_name, wait_time, vip from dispatch_task order by created_at desc, task_id desc",
                (rs, rowNum) -> new DispatchTask(
                        rs.getString("plate_no"),
                        rs.getString("task_type"),
                        rs.getString("tag_name"),
                        rs.getString("wait_time"),
                        rs.getBoolean("vip")
                )
        );
    }

    @Override
    public DispatchTask enqueueDispatchTask(DispatchTask task) {
        jdbcTemplate.update(
                "insert into dispatch_task (plate_no, task_type, tag_name, wait_time, vip, created_at) values (?, ?, ?, ?, ?, ?)",
                task.getPlateNo(),
                task.getType(),
                task.getTag(),
                task.getWait(),
                task.isVip(),
                Timestamp.valueOf(LocalDateTime.now())
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
        return new ParkingOrder(
                rs.getString("order_no"),
                rs.getString("plate_no"),
                rs.getString("slot_id"),
                rs.getTimestamp("entry_time").toLocalDateTime(),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount")
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

}
