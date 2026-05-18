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
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "parkvision.persistence", name = "mode", havingValue = "fallback", matchIfMissing = true)
public class FallbackParkVisionRepository implements ParkVisionRepository {
    private final List<ParkingSlot> slots = new ArrayList<>();
    private final List<ParkingOrder> orders = new ArrayList<>();
    private final List<AlertEvent> alerts = new ArrayList<>();
    private final List<PricingRule> pricingRules = new ArrayList<>();
    private final List<AccessListItem> accessList = new ArrayList<>();
    private final List<SystemNodeStatus> systemNodes = new ArrayList<>();
    private final List<AgvUnit> agvUnits = new ArrayList<>();
    private final List<DispatchTask> dispatchQueue = new ArrayList<>();
    private final List<CameraDevice> cameraDevices = new ArrayList<>();
    private final List<GateDevice> gateDevices = new ArrayList<>();
    private final List<ChargingStation> chargingStations = new ArrayList<>();
    private final List<DeviceEvent> deviceEvents = new ArrayList<>();

    public FallbackParkVisionRepository() {
        seedParkingSlots();
        seedOrders();
        seedAdminData();
        seedDispatchData();
        seedDeviceData();
    }

    @Override
    public List<ParkingSlot> findSlots() {
        return Collections.unmodifiableList(slots);
    }

    @Override
    public Optional<ParkingSlot> findSlotById(String slotId) {
        return slots.stream().filter(slot -> slot.getId().equals(slotId)).findFirst();
    }

    @Override
    public Optional<ParkingSlot> findFirstAvailableSlot() {
        return slots.stream().filter(slot -> slot.getStatus() == SlotStatus.EMPTY).findFirst();
    }

    @Override
    public Optional<ParkingSlot> findFirstDeepOccupiedSlot() {
        return slots.stream()
                .filter(slot -> "Deep".equals(slot.getLayer()))
                .filter(slot -> slot.getStatus() == SlotStatus.OCCUPIED)
                .findFirst();
    }

    @Override
    public ParkingSlot saveSlot(ParkingSlot slot) {
        slots.removeIf(existing -> existing.getId().equals(slot.getId()));
        slots.add(slot);
        return slot;
    }

    @Override
    public List<ParkingOrder> findOrders() {
        return Collections.unmodifiableList(orders);
    }

    @Override
    public Optional<ParkingOrder> findOrderByNo(String orderNo) {
        return orders.stream().filter(order -> order.getOrderNo().equals(orderNo)).findFirst();
    }

    @Override
    public ParkingOrder saveOrder(ParkingOrder order) {
        orders.removeIf(existing -> existing.getOrderNo().equals(order.getOrderNo()));
        orders.add(0, order);
        return order;
    }

    @Override
    public List<AlertEvent> findAlerts() {
        return Collections.unmodifiableList(alerts);
    }

    @Override
    public AlertEvent saveAlert(AlertEvent alert) {
        alerts.removeIf(existing -> existing.alertNo().equals(alert.alertNo()));
        alerts.add(0, alert);
        return alert;
    }

    @Override
    public List<PricingRule> findPricingRules() {
        return Collections.unmodifiableList(pricingRules);
    }

    @Override
    public List<AccessListItem> findAccessList() {
        return Collections.unmodifiableList(accessList);
    }

    @Override
    public List<SystemNodeStatus> findSystemNodes() {
        return Collections.unmodifiableList(systemNodes);
    }

    @Override
    public SystemNodeStatus saveSystemNode(SystemNodeStatus node) {
        systemNodes.removeIf(existing -> existing.name().equals(node.name()));
        systemNodes.add(node);
        return node;
    }

    @Override
    public List<AgvUnit> findAgvUnits() {
        return Collections.unmodifiableList(agvUnits);
    }

    @Override
    public Optional<AgvUnit> findAgvById(String agvId) {
        return agvUnits.stream().filter(agv -> agv.getId().equals(agvId)).findFirst();
    }

    @Override
    public AgvUnit saveAgvUnit(AgvUnit agv) {
        agvUnits.removeIf(existing -> existing.getId().equals(agv.getId()));
        agvUnits.add(agv);
        return agv;
    }

    @Override
    public List<DispatchTask> findDispatchQueue() {
        return Collections.unmodifiableList(dispatchQueue);
    }

    @Override
    public DispatchTask enqueueDispatchTask(DispatchTask task) {
        dispatchQueue.add(0, task);
        return task;
    }

    @Override
    public List<CameraDevice> findCameraDevices() {
        return Collections.unmodifiableList(cameraDevices);
    }

    @Override
    public Optional<CameraDevice> findCameraDeviceById(String cameraId) {
        return cameraDevices.stream().filter(camera -> camera.cameraId().equals(cameraId)).findFirst();
    }

    @Override
    public CameraDevice saveCameraDevice(CameraDevice camera) {
        cameraDevices.removeIf(existing -> existing.cameraId().equals(camera.cameraId()));
        cameraDevices.add(camera);
        return camera;
    }

    @Override
    public List<GateDevice> findGateDevices() {
        return Collections.unmodifiableList(gateDevices);
    }

    @Override
    public Optional<GateDevice> findGateDeviceById(String gateId) {
        return gateDevices.stream().filter(gate -> gate.gateId().equals(gateId)).findFirst();
    }

    @Override
    public GateDevice saveGateDevice(GateDevice gate) {
        gateDevices.removeIf(existing -> existing.gateId().equals(gate.gateId()));
        gateDevices.add(gate);
        return gate;
    }

    @Override
    public List<ChargingStation> findChargingStations() {
        return Collections.unmodifiableList(chargingStations);
    }

    @Override
    public Optional<ChargingStation> findChargingStationById(String chargerId) {
        return chargingStations.stream().filter(station -> station.chargerId().equals(chargerId)).findFirst();
    }

    @Override
    public ChargingStation saveChargingStation(ChargingStation station) {
        chargingStations.removeIf(existing -> existing.chargerId().equals(station.chargerId()));
        chargingStations.add(station);
        return station;
    }

    @Override
    public List<DeviceEvent> findDeviceEvents() {
        return Collections.unmodifiableList(deviceEvents);
    }

    @Override
    public DeviceEvent saveDeviceEvent(DeviceEvent event) {
        deviceEvents.removeIf(existing -> existing.eventId().equals(event.eventId()));
        deviceEvents.add(0, event);
        return event;
    }

    private void seedParkingSlots() {
        SlotStatus[] statusCycle = {
                SlotStatus.EMPTY, SlotStatus.OCCUPIED, SlotStatus.OCCUPIED, SlotStatus.EMPTY,
                SlotStatus.CHARGING, SlotStatus.BUFFER, SlotStatus.OCCUPIED, SlotStatus.EMPTY
        };
        for (int index = 0; index < 72; index++) {
            String id = "%s%02d".formatted((char) ('A' + index / 12), index % 12 + 1);
            String layer = index < 24 ? "Shallow" : index < 48 ? "Mid" : "Deep";
            SlotStatus status = index == 64 ? SlotStatus.MAINTENANCE : statusCycle[index % statusCycle.length];
            slots.add(new ParkingSlot(id, layer, status));
        }

        updateSlot("A09", SlotStatus.BUFFER);
        updateSlot("B01", SlotStatus.EMPTY);
        updateSlot("C05", SlotStatus.CHARGING);
        updateSlot("E06", SlotStatus.OCCUPIED);
    }

    private void seedOrders() {
        LocalDateTime now = LocalDateTime.now();
        addSeedOrder("PV20260506001", "SH-A7686", "E06", now.minusHours(2).minusMinutes(5), OrderStatus.PARKED, "18.00");
        addSeedOrder("PV20260506002", "SH-D5218", "C05", now.minusHours(4).minusMinutes(5), OrderStatus.RETRIEVING, "42.50");
        addSeedOrder("PV20260506003", "SU-M9021", "A09", now.minusHours(3).minusMinutes(12), OrderStatus.TOUCHING, "25.00");
        addSeedOrder("PV20260506004", "SH-K1314", "B01", now.minusHours(1).minusMinutes(8), OrderStatus.FINISHED, "16.00");
        addSeedOrder("PV20260506005", "SH-P3308", "D04", now.minusHours(5).minusMinutes(18), OrderStatus.PARKED, "26.00");
        addSeedOrder("PV20260506006", "SH-R1188", "F08", now.minusHours(8).minusMinutes(30), OrderStatus.FINISHED, "38.00");
        addSeedOrder("PV20260506007", "SH-L5521", "C02", now.minusHours(2).minusMinutes(48), OrderStatus.PARKED, "14.00");
        addSeedOrder("PV20260506008", "SH-T6502", "D11", now.minusHours(3).minusMinutes(40), OrderStatus.PAYING, "28.00");
        addSeedOrder("PV20260506009", "SH-N4820", "B07", now.minusHours(10).minusMinutes(5), OrderStatus.FINISHED, "44.00");
        addSeedOrder("PV20260506010", "SH-C8871", "A04", now.minusHours(6).minusMinutes(15), OrderStatus.ABNORMAL, "12.00");
        addSeedOrder("PV20260506011", "SH-G6205", "F03", now.minusHours(1).minusMinutes(55), OrderStatus.PARKED, "12.00");
        addSeedOrder("PV20260506012", "SH-D9082", "E02", now.minusHours(7).minusMinutes(20), OrderStatus.PARKED, "51.80");
        addSeedOrder("PV20260506013", "SU-Q1137", "C09", now.minusHours(12).minusMinutes(2), OrderStatus.FINISHED, "52.00");
        addSeedOrder("PV20260506014", "SH-M4401", "D06", now.minusHours(2).minusMinutes(36), OrderStatus.RETRIEVING, "21.00");
        addSeedOrder("PV20260506015", "SH-V7780", "E11", now.minusMinutes(88), OrderStatus.PARKED, "8.00");
        addSeedOrder("PV20260506016", "SH-X2204", "B10", now.minusHours(9).minusMinutes(44), OrderStatus.FINISHED, "40.00");
        addSeedOrder("PV20260506017", "SH-H3819", "F01", now.minusHours(1).minusMinutes(38), OrderStatus.PARKED, "10.00");
        addSeedOrder("PV20260506018", "SH-Y6608", "C12", now.minusHours(3).minusMinutes(2), OrderStatus.TOUCHING, "23.00");
        addSeedOrder("PV20260506019", "SH-D3015", "E04", now.minusHours(5).minusMinutes(25), OrderStatus.PARKED, "47.60");
        addSeedOrder("PV20260506020", "SU-A7005", "A12", now.minusHours(11).minusMinutes(18), OrderStatus.FINISHED, "36.00");
        addSeedOrder("PV20260506021", "SH-U1140", "D08", now.minusHours(2).minusMinutes(58), OrderStatus.PAYING, "19.00");
        addSeedOrder("PV20260506022", "SH-W3099", "F10", now.minusMinutes(96), OrderStatus.PARKED, "9.00");
        addSeedOrder("PV20260506023", "SH-J5520", "B03", now.minusHours(6).minusMinutes(52), OrderStatus.FINISHED, "31.00");
        addSeedOrder("PV20260506024", "SH-Z9907", "C07", now.minusHours(4).minusMinutes(8), OrderStatus.PARKED, "22.00");
    }

    private void seedAdminData() {
        alerts.add(new AlertEvent("AL20260518001", "Safety", "Person intrusion detected in transfer zone", "Emergency stop", "High"));
        alerts.add(new AlertEvent("AL20260518002", "Device", "AGV-04 battery below 20%", "Processing", "Medium"));
        alerts.add(new AlertEvent("AL20260518003", "Order", "Secondary plate recognition mismatch", "Pending review", "Medium"));
        alerts.add(new AlertEvent("AL20260518004", "Gate", "Inbound barrier queue depth exceeded dispatch threshold", "Open", "Medium"));
        alerts.add(new AlertEvent("AL20260518005", "Charging", "EVSE-03 handshake retried after connector wake-up timeout", "Resolved", "Low"));
        alerts.add(new AlertEvent("AL20260518006", "Vision", "North camera bitrate degraded below expected edge profile", "Monitoring", "Low"));
        alerts.add(new AlertEvent("AL20260518007", "Billing", "One finished order is pending settlement confirmation", "Processing", "Medium"));
        alerts.add(new AlertEvent("AL20260518008", "Dispatch", "Manual review requested for abnormal slot release on A04", "Escalated", "High"));

        pricingRules.add(new PricingRule("Workday hourly", "07:00-22:00", "First hour 6, then 4/hour", "Cap 48", "Active"));
        pricingRules.add(new PricingRule("Night package", "22:00-07:00", "12 flat rate", "Monthly pass exempt", "Active"));
        pricingRules.add(new PricingRule("VIP retrieval", "All day", "Base fee + 8", "Queue weight +40", "Active"));
        pricingRules.add(new PricingRule("EV charging", "All day", "1.2/kWh", "Auto release when full", "Active"));

        accessList.add(new AccessListItem("SH-A7686", "Whitelist", "Monthly member", "2026-12-31", "Auto pass"));
        accessList.add(new AccessListItem("SH-D5218", "Whitelist", "EV owner", "2026-09-01", "Charging priority"));
        accessList.add(new AccessListItem("SU-M9021", "Normal", "Temporary visitor", "Single order", "Supports contactless pay"));
        accessList.add(new AccessListItem("SH-B9001", "Blacklist", "Payment exception", "Manual review", "Entry blocked"));
        accessList.add(new AccessListItem("SH-P3308", "Whitelist", "Corporate account", "2026-11-15", "Peak-hour inbound priority"));
        accessList.add(new AccessListItem("SH-D9082", "Whitelist", "EV monthly member", "2026-10-20", "Charging fee discount"));
        accessList.add(new AccessListItem("SH-V7780", "Whitelist", "Reserved retrieval user", "2026-08-31", "Supports advance outbound booking"));
        accessList.add(new AccessListItem("SH-T6502", "Normal", "Retail visitor", "Single order", "Self-service payment enabled"));
        accessList.add(new AccessListItem("SH-C8871", "Watchlist", "Exception review", "Manual review", "Route release requires staff approval"));
        accessList.add(new AccessListItem("SH-H3819", "Whitelist", "Night-package member", "2026-12-01", "Night cap applies automatically"));
        accessList.add(new AccessListItem("SU-A7005", "Normal", "Temporary visitor", "Single order", "Invoice requested"));
        accessList.add(new AccessListItem("SH-X2204", "Blacklist", "Repeated overtime exit", "Manual review", "Outbound confirmation required"));

        systemNodes.add(new SystemNodeStatus("Edge-Cam-01", "98ms", "South gate vision pre-processing node is healthy and forwarding OCR metadata", "stable"));
        systemNodes.add(new SystemNodeStatus("PLC-Master-Controller", "12ms", "Barrier controller and AGV fleet gateway heartbeats are stable", "stable"));
        systemNodes.add(new SystemNodeStatus("Redis-Sync-Cluster", "31ms", "Operational cache and report fan-out are synchronized", "stable"));
        systemNodes.add(new SystemNodeStatus("Billing-Service", "44ms", "Invoice and settlement projection jobs are completing on schedule", "stable"));
        systemNodes.add(new SystemNodeStatus("Device-Simulator", "21ms", "Camera, gate, charger, and AGV telemetry simulation loops are healthy", "stable"));
    }

    private void seedDispatchData() {
        agvUnits.add(new AgvUnit("AGV-01", 10, 12, false, "Patrolling Zone A", 91, "IDLE", 0.42, "patrol"));
        agvUnits.add(new AgvUnit("AGV-02", 45, 32, true, "Carrying SH-A7686", 74, "CARRYING", 0.86, "deliver"));
        agvUnits.add(new AgvUnit("AGV-03", 72, 58, false, "Heading to shallow buffer", 68, "TRANSIT", 0.65, "relocate"));
        agvUnits.add(new AgvUnit("AGV-04", 28, 76, false, "Charging standby", 19, "CHARGING", 0.00, "dock"));

        dispatchQueue.add(new DispatchTask("SH-A7686", "Standard retrieval", "FIFO", "04:12", false));
        dispatchQueue.add(new DispatchTask("SH-D5218", "Charging bay release", "Charging done", "03:40", false));
        dispatchQueue.add(new DispatchTask("SU-M9021", "Touch-and-Go", "Touch", "02:10", false));
        dispatchQueue.add(new DispatchTask("SH-V7780", "Reserved outbound", "Booking", "01:58", false));
        dispatchQueue.add(new DispatchTask("SH-P3308", "Pre-dispatch relocation", "PRE", "00:48", true));
        dispatchQueue.add(new DispatchTask("SH-M4401", "VIP retrieval", "VIP", "00:30", true));
        dispatchQueue.add(new DispatchTask("SH-D9082", "Charging connector release", "EV", "02:42", false));
        dispatchQueue.add(new DispatchTask("SH-T6502", "Payment confirmation hold", "PAY", "01:24", false));
        dispatchQueue.add(new DispatchTask("SH-C8871", "Manual review transfer", "RISK", "05:36", false));
        dispatchQueue.add(new DispatchTask("SH-H3819", "Night package outbound", "Night", "03:06", false));
    }

    private void seedDeviceData() {
        LocalDateTime now = LocalDateTime.now();
        cameraDevices.add(new CameraDevice(
                "CAM-SOUTH-01",
                "ONVIF Profile T",
                "H.265",
                "rtsp://10.10.1.21:554/Streaming/Channels/101",
                25,
                4096,
                "ONLINE",
                "SH-A7686",
                now.minusSeconds(12),
                false,
                false,
                "South gate edge camera with OCR and handoff-zone intrusion ROI"
        ));
        cameraDevices.add(new CameraDevice(
                "CAM-HANDOFF-02",
                "ONVIF Profile T",
                "H.264",
                "rtsp://10.10.1.33:554/Streaming/Channels/101",
                20,
                3072,
                "ONLINE",
                "SU-M9021",
                now.minusSeconds(7),
                false,
                false,
                "Transfer-bay safety camera with person intrusion alarm"
        ));
        cameraDevices.add(new CameraDevice(
                "CAM-NORTH-03",
                "ONVIF Profile T",
                "H.265",
                "rtsp://10.10.1.46:554/Streaming/Channels/101",
                25,
                3584,
                "ONLINE",
                "SH-P3308",
                now.minusSeconds(15),
                false,
                false,
                "North ramp camera used for outbound queue observation and secondary plate read"
        ));

        gateDevices.add(new GateDevice(
                "GATE-IN-01",
                "Modbus/TCP",
                "10.10.20.11:502",
                "00017",
                2,
                "OPEN",
                true,
                false,
                "ACCESS_GRANTED",
                now.minusSeconds(5),
                "Inbound barrier with loop detector and PLC relay control"
        ));
        gateDevices.add(new GateDevice(
                "GATE-OUT-01",
                "Modbus/TCP",
                "10.10.20.12:502",
                "00021",
                1,
                "CLOSED",
                false,
                false,
                "READY",
                now.minusSeconds(8),
                "Outbound handoff gate synchronized with AGV release window"
        ));
        gateDevices.add(new GateDevice(
                "GATE-SERVICE-01",
                "Modbus/TCP",
                "10.10.20.19:502",
                "00025",
                0,
                "CLOSED",
                false,
                false,
                "MAINTENANCE_READY",
                now.minusSeconds(11),
                "Service corridor gate used for manual review and maintenance detours"
        ));

        chargingStations.add(new ChargingStation(
                "EVSE-01",
                "OCPP 1.6J",
                "ws://10.10.30.41:9000/ocpp/EVSE-01",
                "Charging",
                new BigDecimal("11.00"),
                new BigDecimal("18.40"),
                "SH-D5218",
                "Accepted",
                now.minusSeconds(10),
                "AC charger in premium bay C05"
        ));
        chargingStations.add(new ChargingStation(
                "EVSE-02",
                "OCPP 1.6J",
                "ws://10.10.30.42:9000/ocpp/EVSE-02",
                "Available",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                "Idle",
                now.minusSeconds(4),
                "AC charger near handoff zone for short dwell sessions"
        ));
        chargingStations.add(new ChargingStation(
                "EVSE-03",
                "OCPP 1.6J",
                "ws://10.10.30.43:9000/ocpp/EVSE-03",
                "Preparing",
                new BigDecimal("2.40"),
                new BigDecimal("0.60"),
                "SH-D3015",
                "Authorizing",
                now.minusSeconds(9),
                "Fast-turnover charger waiting for connector lock confirmation"
        ));

        String[][] eventSpecs = {
                {"camera", "CAM-SOUTH-01", "PLATE_READ", "info", "OCR recognized SH-A7686 and forwarded metadata to the entry service"},
                {"gate", "GATE-IN-01", "LOOP_OCCUPIED", "info", "Induction loop detected a vehicle at the inbound barrier"},
                {"charger", "EVSE-01", "ENERGY_DELIVERY", "info", "Charging session for SH-D5218 reached 18.40 kWh"},
                {"agv", "AGV-02", "TASK_ASSIGNED", "info", "AGV-02 accepted a loaded retrieval task for SH-A7686"},
                {"camera", "CAM-HANDOFF-02", "ROI_CLEAR", "info", "Transfer-bay safety ROI cleared after pedestrian departure"},
                {"gate", "GATE-OUT-01", "RELEASE_READY", "info", "Outbound gate is synchronized with the next handoff slot"},
                {"agv", "AGV-04", "BATTERY_LOW", "medium", "AGV-04 entered charging standby below the configured battery threshold"},
                {"charger", "EVSE-03", "AUTHORIZE_START", "info", "Connector EVSE-03 started an authorization handshake for SH-D3015"},
                {"camera", "CAM-NORTH-03", "SECONDARY_READ", "info", "North ramp camera confirmed outbound plate SH-P3308"},
                {"gate", "GATE-IN-01", "QUEUE_BUILDUP", "medium", "Inbound queue depth crossed the pre-dispatch planning threshold"},
                {"dispatch", "VIP-QUEUE", "VIP_INSERT", "medium", "VIP retrieval for SH-M4401 was inserted at the head of the queue"},
                {"order", "PV20260506010", "MANUAL_REVIEW", "high", "Order PV20260506010 is waiting for manual release approval"},
                {"charger", "EVSE-01", "SESSION_UPDATE", "info", "Delivered energy increased by 0.8 kWh during the active session"},
                {"agv", "AGV-03", "RELOCATE_START", "info", "AGV-03 started pre-dispatch relocation from a deep-slot aisle"},
                {"camera", "CAM-HANDOFF-02", "PERSON_DETECTED", "low", "A short-lived pedestrian detection was logged outside the protected ROI"},
                {"gate", "GATE-SERVICE-01", "SERVICE_READY", "info", "Service corridor gate is idle and ready for exception routing"},
                {"dispatch", "PRE-DISPATCH", "TASK_QUEUED", "info", "Forecast-driven relocation task queued for SH-P3308"},
                {"billing", "INVOICE-SVC", "SETTLEMENT_PENDING", "medium", "One finished order is pending settlement callback confirmation"},
                {"charger", "EVSE-02", "HEARTBEAT", "info", "Idle charger EVSE-02 sent a normal heartbeat frame"},
                {"camera", "CAM-SOUTH-01", "BITRATE_DIP", "low", "Bitrate dipped briefly below the preferred edge profile before recovery"},
                {"agv", "AGV-01", "PATROL_TICK", "info", "AGV-01 completed a perimeter patrol cycle near Zone A"},
                {"gate", "GATE-OUT-01", "EXIT_CONFIRMED", "info", "Outbound barrier confirmed a completed handoff release"},
                {"charger", "EVSE-03", "LOCK_RETRY", "medium", "Connector lock on EVSE-03 retried once before engagement"},
                {"dispatch", "RISK-QUEUE", "HOLD_ACTIVE", "high", "Manual-review transfer remains blocked until staff clearance"}
        };
        for (int index = 0; index < eventSpecs.length; index++) {
            String[] spec = eventSpecs[index];
            deviceEvents.add(new DeviceEvent(
                    "DV20260518%03d".formatted(index + 1),
                    spec[0],
                    spec[1],
                    spec[2],
                    spec[3],
                    spec[4],
                    now.minusMinutes(eventSpecs.length - index),
                    index < 18
            ));
        }
    }

    private void updateSlot(String slotId, SlotStatus status) {
        findSlotById(slotId).ifPresent(slot -> slot.setStatus(status));
    }

    private void addSeedOrder(String orderNo, String plateNo, String slotId, LocalDateTime entryTime, OrderStatus status, String amount) {
        orders.add(new ParkingOrder(orderNo, plateNo, slotId, entryTime, status, new BigDecimal(amount)));
        updateSlotForOrder(slotId, plateNo, status);
    }

    private void updateSlotForOrder(String slotId, String plateNo, OrderStatus status) {
        findSlotById(slotId).ifPresent(slot -> {
            switch (status) {
                case PARKED -> slot.setStatus(plateNo.startsWith("SH-D") ? SlotStatus.CHARGING : SlotStatus.OCCUPIED);
                case RETRIEVING, TOUCHING, PAYING -> slot.setStatus(SlotStatus.BUFFER);
                case FINISHED -> slot.setStatus(SlotStatus.EMPTY);
                case ABNORMAL -> slot.setStatus(SlotStatus.OCCUPIED);
            }
        });
    }
}
