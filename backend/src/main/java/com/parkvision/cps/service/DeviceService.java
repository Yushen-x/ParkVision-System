package com.parkvision.cps.service;

import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.SystemNodeStatus;
import com.parkvision.cps.domain.device.CameraDevice;
import com.parkvision.cps.domain.device.ChargingStation;
import com.parkvision.cps.domain.device.DeviceEvent;
import com.parkvision.cps.domain.device.GateDevice;
import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.dto.device.DeviceOverview;
import com.parkvision.cps.dto.vision.VisionResult;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class DeviceService {
    private final ParkVisionRepository repository;
    private final boolean simulationEnabled;
    private final Random random = new Random();

    public DeviceService(
            ParkVisionRepository repository,
            @Value("${parkvision.devices.simulation-enabled:true}") boolean simulationEnabled
    ) {
        this.repository = repository;
        this.simulationEnabled = simulationEnabled;
    }

    public DeviceOverview overview() {
        return new DeviceOverview(
                repository.findCameraDevices(),
                repository.findGateDevices(),
                repository.findChargingStations(),
                repository.findDeviceEvents().stream().limit(10).toList()
        );
    }

    public boolean emergencyActive() {
        return repository.findGateDevices().stream().anyMatch(GateDevice::estopArmed)
                || repository.findCameraDevices().stream().anyMatch(CameraDevice::intrusionState);
    }

    public boolean setEmergency(boolean active) {
        LocalDateTime now = LocalDateTime.now();

        repository.findCameraDevices().forEach(camera ->
                repository.saveCameraDevice(new CameraDevice(
                        camera.cameraId(),
                        camera.profile(),
                        camera.codec(),
                        camera.streamUrl(),
                        camera.fps(),
                        camera.bitrateKbps(),
                        camera.status(),
                        camera.lastPlate(),
                        now,
                        camera.tamperAlarm(),
                        active,
                        active
                                ? "Emergency intrusion override is active for the handoff safety ROI"
                                : "Safety ROI is clear and the edge camera has resumed normal operation"
                ))
        );

        repository.findGateDevices().forEach(gate ->
                repository.saveGateDevice(new GateDevice(
                        gate.gateId(),
                        gate.protocol(),
                        gate.endpoint(),
                        gate.coilAddress(),
                        gate.queueDepth(),
                        active ? "LOCKDOWN" : nextGateState(gate.queueDepth(), false),
                        gate.loopOccupied(),
                        active,
                        active ? "ESTOP_ACTIVE" : "RECOVERY_READY",
                        now,
                        active
                                ? "Safety lock engaged. PLC relay output is inhibited until manual clearance."
                                : "Barrier controller returned to automatic mode after safety review."
                ))
        );

        updateSystemNodes(active);
        String eventId = eventId("EST");
        repository.saveDeviceEvent(new DeviceEvent(
                eventId,
                "safety",
                "HandoffZone",
                active ? "ESTOP_ACTIVE" : "ESTOP_RELEASED",
                active ? "critical" : "info",
                active
                        ? "Emergency stop latched from the operator console and propagated to the PLC layer"
                        : "Emergency stop released and field devices returned to automatic service",
                now,
                false
        ));

        if (active) {
            repository.saveAlert(new AlertEvent(
                    "AL" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                    "Safety",
                    "Operator-triggered emergency stop is active across the handoff corridor",
                    "Emergency stop",
                    "High"
            ));
        }

        return active;
    }

    public void recordVisionInference(VisionResult result) {
        LocalDateTime now = LocalDateTime.now();
        CameraDevice camera = repository.findCameraDeviceById(result.cameraId())
                .orElseGet(() -> repository.findCameraDevices().stream().findFirst().orElse(null));
        if (camera != null) {
            repository.saveCameraDevice(new CameraDevice(
                    camera.cameraId(),
                    camera.profile(),
                    camera.codec(),
                    camera.streamUrl(),
                    camera.fps(),
                    camera.bitrateKbps(),
                    "ONLINE",
                    result.plate(),
                    now,
                    false,
                    result.intrusion(),
                    result.intrusion()
                            ? "Person intrusion was detected inside the handoff ROI"
                            : "Latest plate OCR result was accepted by the edge inference pipeline"
            ));
        }

        repository.saveDeviceEvent(new DeviceEvent(
                eventId("VIS"),
                "camera",
                result.cameraId(),
                result.intrusion() ? "INTRUSION_DETECTED" : "PLATE_READ",
                result.intrusion() ? "high" : "info",
                result.intrusion()
                        ? "Camera " + result.cameraId() + " detected a safety intrusion and requested ESTOP"
                        : "Camera " + result.cameraId() + " recognized plate " + result.plate() + " with confidence " + result.confidence(),
                now,
                false
        ));

        if (result.intrusion()) {
            setEmergency(true);
        } else {
            repository.findGateDevices().stream()
                    .filter(gate -> gate.gateId().contains("IN"))
                    .findFirst()
                    .ifPresent(gate -> repository.saveGateDevice(new GateDevice(
                            gate.gateId(),
                            gate.protocol(),
                            gate.endpoint(),
                            gate.coilAddress(),
                            gate.queueDepth(),
                            nextGateState(gate.queueDepth(), false),
                            true,
                            false,
                            "OCR_OK",
                            now,
                            "Inbound PLC accepted the OCR decision and is ready for barrier release"
                    )));
            updateSystemNodes(false);
        }
    }

    public void recordEntry(ParkingOrder order) {
        LocalDateTime now = LocalDateTime.now();
        repository.findGateDevices().stream()
                .filter(gate -> gate.gateId().contains("IN"))
                .findFirst()
                .ifPresent(gate -> repository.saveGateDevice(new GateDevice(
                        gate.gateId(),
                        gate.protocol(),
                        gate.endpoint(),
                        gate.coilAddress(),
                        Math.max(0, gate.queueDepth() - 1),
                        "OPEN",
                        true,
                        false,
                        "ACCESS_GRANTED",
                        now,
                        "Inbound barrier opened after OCR verification and slot reservation"
                )));

        repository.saveDeviceEvent(new DeviceEvent(
                eventId("ENT"),
                "gate",
                "GATE-IN-01",
                "ORDER_CREATED",
                "info",
                "Entry workflow created order " + order.getOrderNo() + " for plate " + order.getPlateNo(),
                now,
                false
        ));
        updateSystemNodes(false);
    }

    public void recordDispatchTask(DispatchTask task) {
        LocalDateTime now = LocalDateTime.now();
        repository.findGateDevices().stream()
                .filter(gate -> gate.gateId().contains("OUT"))
                .findFirst()
                .ifPresent(gate -> repository.saveGateDevice(new GateDevice(
                        gate.gateId(),
                        gate.protocol(),
                        gate.endpoint(),
                        gate.coilAddress(),
                        Math.min(6, gate.queueDepth() + 1),
                        "PREPARE_OPEN",
                        gate.loopOccupied(),
                        emergencyActive(),
                        task.isVip() ? "VIP_RELEASE" : "QUEUE_RELEASE",
                        now,
                        "Outbound barrier is synchronized with the latest AGV dispatch intent"
                )));

        repository.saveDeviceEvent(new DeviceEvent(
                eventId("DSP"),
                "dispatch",
                task.isVip() ? "VIP-QUEUE" : "STANDARD-QUEUE",
                task.isVip() ? "VIP_INSERT" : "TASK_QUEUED",
                task.isVip() ? "medium" : "info",
                task.getType() + " queued for plate " + task.getPlateNo(),
                now,
                false
        ));
        updateSystemNodes(emergencyActive());
    }

    public void recordOrderClosed(ParkingOrder order) {
        LocalDateTime now = LocalDateTime.now();
        repository.findGateDevices().stream()
                .filter(gate -> gate.gateId().contains("OUT"))
                .findFirst()
                .ifPresent(gate -> repository.saveGateDevice(new GateDevice(
                        gate.gateId(),
                        gate.protocol(),
                        gate.endpoint(),
                        gate.coilAddress(),
                        Math.max(0, gate.queueDepth() - 1),
                        "OPEN",
                        false,
                        emergencyActive(),
                        "EXIT_CONFIRMED",
                        now,
                        "Outbound barrier confirmed payment and vehicle release"
                )));

        repository.findChargingStations().stream()
                .filter(station -> order.getPlateNo().equals(station.vehiclePlate()))
                .findFirst()
                .ifPresent(station -> repository.saveChargingStation(new ChargingStation(
                        station.chargerId(),
                        station.protocol(),
                        station.endpoint(),
                        "Available",
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        station.sessionKwh(),
                        null,
                        "Idle",
                        now,
                        "Charging session closed after the owner completed payment and exit"
                )));

        repository.saveDeviceEvent(new DeviceEvent(
                eventId("PAY"),
                "order",
                order.getOrderNo(),
                "EXIT_CONFIRMED",
                "info",
                "Order " + order.getOrderNo() + " was closed and the release gate was confirmed",
                now,
                false
        ));
        updateSystemNodes(emergencyActive());
    }

    @Scheduled(fixedDelayString = "${parkvision.devices.simulation-interval-ms:4000}")
    public void tick() {
        if (!simulationEnabled || emergencyActive()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<AgvUnit> agvs = repository.findAgvUnits();
        for (int index = 0; index < agvs.size(); index++) {
            AgvUnit agv = agvs.get(index);
            int dx = index % 2 == 0 ? 3 : -2;
            int dy = index % 2 == 0 ? -2 : 3;
            agv.setX(wrap(agv.getX() + dx, 8, 88));
            agv.setY(wrap(agv.getY() + dy, 10, 84));
            agv.setBatteryPct(agv.getMode().equals("CHARGING") ? Math.min(100, agv.getBatteryPct() + 1) : Math.max(12, agv.getBatteryPct() - 1));
            agv.setVelocityMps(agv.isLoaded() ? 0.86 : agv.getMode().equals("CHARGING") ? 0.00 : 0.58);
            if (agv.getBatteryPct() <= 20) {
                agv.setMode("CHARGING");
                agv.setTask("Battery recovery cycle");
                agv.setLastCommand("dock");
            } else if (agv.isLoaded()) {
                agv.setMode("CARRYING");
                agv.setLastCommand("deliver");
            } else {
                agv.setMode("TRANSIT");
                agv.setLastCommand("navigate");
            }
            repository.saveAgvUnit(agv);
        }

        repository.findGateDevices().forEach(gate -> {
            int queueDepth = clamp(gate.queueDepth() + random.nextInt(3) - 1, 0, 5);
            boolean loopOccupied = queueDepth > 0 || random.nextBoolean();
            repository.saveGateDevice(new GateDevice(
                    gate.gateId(),
                    gate.protocol(),
                    gate.endpoint(),
                    gate.coilAddress(),
                    queueDepth,
                    nextGateState(queueDepth, false),
                    loopOccupied,
                    false,
                    queueDepth >= 4 ? "QUEUE_BUILDUP" : gate.lastDecision(),
                    now,
                    queueDepth >= 4
                            ? "Queue buildup detected. The PLC can request a pre-dispatch release window."
                            : gate.detail()
            ));
        });

        repository.findChargingStations().forEach(station -> {
            if ("Charging".equalsIgnoreCase(station.connectorStatus()) && station.vehiclePlate() != null) {
                BigDecimal session = station.sessionKwh().add(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal power = new BigDecimal("10.80");
                String status = session.compareTo(new BigDecimal("22.00")) >= 0 ? "Finishing" : "Charging";
                repository.saveChargingStation(new ChargingStation(
                        station.chargerId(),
                        station.protocol(),
                        station.endpoint(),
                        status,
                        power,
                        session,
                        station.vehiclePlate(),
                        "Accepted",
                        now,
                        "OCPP telemetry heartbeat received from the active charging connector"
                ));
            } else {
                repository.saveChargingStation(new ChargingStation(
                        station.chargerId(),
                        station.protocol(),
                        station.endpoint(),
                        station.connectorStatus(),
                        station.powerKw(),
                        station.sessionKwh(),
                        station.vehiclePlate(),
                        station.authStatus(),
                        now,
                        station.detail()
                ));
            }
        });

        repository.findCameraDevices().forEach(camera -> repository.saveCameraDevice(new CameraDevice(
                camera.cameraId(),
                camera.profile(),
                camera.codec(),
                camera.streamUrl(),
                camera.fps(),
                camera.bitrateKbps(),
                "ONLINE",
                camera.lastPlate(),
                now,
                false,
                false,
                camera.detail()
        )));

        updateSystemNodes(false);
    }

    private void updateSystemNodes(boolean emergency) {
        repository.saveSystemNode(new SystemNodeStatus(
                "Edge-Cam-01",
                emergency ? "ALARM" : (90 + random.nextInt(18)) + "ms",
                emergency
                        ? "Camera safety ROI escalated and the edge node is holding dispatch release"
                        : "South gate vision pre-processing node is healthy and forwarding OCR metadata",
                emergency ? "warning" : "stable"
        ));
        repository.saveSystemNode(new SystemNodeStatus(
                "PLC-Master-Controller",
                emergency ? "LOCK" : (12 + random.nextInt(6)) + "ms",
                emergency
                        ? "PLC master has inhibited field outputs because ESTOP is active"
                        : "Barrier controller and AGV fleet gateway heartbeats are stable",
                emergency ? "warning" : "stable"
        ));
        repository.saveSystemNode(new SystemNodeStatus(
                "Redis-Sync-Cluster",
                emergency ? "DEGRADED" : (24 + random.nextInt(10)) + "ms",
                emergency
                        ? "Event fan-out is paused while the safety workflow is under review"
                        : "Operational cache and report fan-out are synchronized",
                emergency ? "warning" : "stable"
        ));
    }

    private String eventId(String prefix) {
        return prefix + System.currentTimeMillis();
    }

    private String nextGateState(int queueDepth, boolean emergency) {
        if (emergency) {
            return "LOCKDOWN";
        }
        if (queueDepth >= 3) {
            return "OPEN";
        }
        return queueDepth == 0 ? "CLOSED" : "READY";
    }

    private int wrap(int value, int min, int max) {
        if (value > max) {
            return min;
        }
        if (value < min) {
            return max;
        }
        return value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
