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
        orders.add(new ParkingOrder("PV20260506001", "SH-A7686", "E06", LocalDateTime.now().minusHours(2), OrderStatus.PARKED, new BigDecimal("18.00")));
        orders.add(new ParkingOrder("PV20260506002", "SH-D5218", "C05", LocalDateTime.now().minusHours(4), OrderStatus.RETRIEVING, new BigDecimal("42.50")));
        orders.add(new ParkingOrder("PV20260506003", "SU-M9021", "A09", LocalDateTime.now().minusHours(3), OrderStatus.TOUCHING, new BigDecimal("25.00")));
        orders.add(new ParkingOrder("PV20260506004", "SH-K1314", "B01", LocalDateTime.now().minusHours(1), OrderStatus.FINISHED, new BigDecimal("16.00")));
    }

    private void seedAdminData() {
        alerts.add(new AlertEvent("AL2026050601", "Safety", "Person intrusion detected in transfer zone", "Emergency stop", "High"));
        alerts.add(new AlertEvent("AL2026050602", "Device", "AGV-04 battery below 20%", "Processing", "Medium"));
        alerts.add(new AlertEvent("AL2026050603", "Order", "Secondary plate recognition mismatch", "Pending review", "Medium"));

        pricingRules.add(new PricingRule("Workday hourly", "07:00-22:00", "First hour 6, then 4/hour", "Cap 48", "Active"));
        pricingRules.add(new PricingRule("Night package", "22:00-07:00", "12 flat rate", "Monthly pass exempt", "Active"));
        pricingRules.add(new PricingRule("VIP retrieval", "All day", "Base fee + 8", "Queue weight +40", "Active"));
        pricingRules.add(new PricingRule("EV charging", "All day", "1.2/kWh", "Auto release when full", "Active"));

        accessList.add(new AccessListItem("SH-A7686", "Whitelist", "Monthly member", "2026-12-31", "Auto pass"));
        accessList.add(new AccessListItem("SH-D5218", "Whitelist", "EV owner", "2026-09-01", "Charging priority"));
        accessList.add(new AccessListItem("SU-M9021", "Normal", "Temporary visitor", "Single order", "Supports contactless pay"));
        accessList.add(new AccessListItem("SH-B9001", "Blacklist", "Payment exception", "Manual review", "Entry blocked"));

        systemNodes.add(new SystemNodeStatus("Edge-Cam-01", "98ms", "South gate vision pre-processing node is healthy", "stable"));
        systemNodes.add(new SystemNodeStatus("PLC-Master-Controller", "12ms", "AGV fleet gateway heartbeat is stable", "stable"));
        systemNodes.add(new SystemNodeStatus("Redis-Sync-Cluster", "TIMEOUT", "Cache sync timeout, degraded strategy enabled", "warning"));
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

        deviceEvents.add(new DeviceEvent(
                "DV20260506001",
                "camera",
                "CAM-SOUTH-01",
                "PLATE_READ",
                "info",
                "OCR recognized SH-A7686 and forwarded metadata to the entry service",
                now.minusMinutes(4),
                true
        ));
        deviceEvents.add(new DeviceEvent(
                "DV20260506002",
                "gate",
                "GATE-IN-01",
                "LOOP_OCCUPIED",
                "info",
                "Induction loop detected a vehicle at the inbound barrier",
                now.minusMinutes(3),
                true
        ));
        deviceEvents.add(new DeviceEvent(
                "DV20260506003",
                "charger",
                "EVSE-01",
                "ENERGY_DELIVERY",
                "info",
                "Charging session for SH-D5218 reached 18.40 kWh",
                now.minusMinutes(2),
                false
        ));
    }

    private void updateSlot(String slotId, SlotStatus status) {
        findSlotById(slotId).ifPresent(slot -> slot.setStatus(status));
    }
}
