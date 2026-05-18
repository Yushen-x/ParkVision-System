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
import java.util.UUID;

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
                                ? "交接区安全 ROI 的急停入侵覆盖已生效"
                                : "安全 ROI 已清空，边缘摄像头恢复正常运行"
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
                                ? "安全锁已生效，PLC 继电器输出在人工确认前保持禁止。"
                                : "安全复核结束后，道闸控制器已恢复自动模式。"
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
                        ? "操作台触发的急停已锁存，并下发到 PLC 层"
                        : "急停已解除，现场设备恢复自动服务",
                now,
                false
        ));

        if (active) {
            repository.saveAlert(new AlertEvent(
                    "AL" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                    "安全",
                    "操作员触发的急停已在交接走廊生效",
                    "急停中",
                    "高"
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
                            ? "交接区 ROI 内检测到人员入侵"
                            : "最新车牌 OCR 结果已被边缘推理流水线接受"
            ));
        }

        repository.saveDeviceEvent(new DeviceEvent(
                eventId("VIS"),
                "camera",
                result.cameraId(),
                result.intrusion() ? "INTRUSION_DETECTED" : "PLATE_READ",
                result.intrusion() ? "high" : "info",
                result.intrusion()
                        ? "摄像头 " + result.cameraId() + " 检测到安全入侵并请求急停"
                        : "摄像头 " + result.cameraId() + " 识别车牌 " + result.plate() + "，置信度 " + result.confidence(),
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
                            "入场 PLC 已接受 OCR 决策，准备放行道闸"
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
                        "OCR 校验和车位预留完成后，入场道闸已开启"
                )));

        repository.saveDeviceEvent(new DeviceEvent(
                eventId("ENT"),
                "gate",
                "GATE-IN-01",
                "ORDER_CREATED",
                "info",
                "入场流程已为车牌 " + order.getPlateNo() + " 创建订单 " + order.getOrderNo(),
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
                        "出场道闸已与最新 AGV 调度意图同步"
                )));

        repository.saveDeviceEvent(new DeviceEvent(
                eventId("DSP"),
                "dispatch",
                task.isVip() ? "VIP-QUEUE" : "STANDARD-QUEUE",
                task.isVip() ? "VIP_INSERT" : "TASK_QUEUED",
                task.isVip() ? "medium" : "info",
                task.getType() + " 已为车牌 " + task.getPlateNo() + " 入队",
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
                        "出场道闸已确认支付和车辆放行"
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
                        "车主完成支付和离场后，充电会话已关闭"
                )));

        repository.saveDeviceEvent(new DeviceEvent(
                eventId("PAY"),
                "order",
                order.getOrderNo(),
                "EXIT_CONFIRMED",
                "info",
                "订单 " + order.getOrderNo() + " 已关闭，放行闸机已确认",
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
                agv.setTask("电池恢复充电");
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
                            ? "检测到排队积压，PLC 可请求预调度放行窗口。"
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
                        "已从活跃充电枪接收 OCPP 遥测心跳"
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
                        ? "摄像头安全 ROI 已升级告警，边缘节点正在阻止调度放行"
                        : "南门视觉预处理节点运行正常，正在转发 OCR 元数据",
                emergency ? "warning" : "stable"
        ));
        repository.saveSystemNode(new SystemNodeStatus(
                "PLC-Master-Controller",
                emergency ? "LOCK" : (12 + random.nextInt(6)) + "ms",
                emergency
                        ? "急停生效，PLC 主控已禁止现场输出"
                        : "道闸控制器与 AGV 网关心跳稳定",
                emergency ? "warning" : "stable"
        ));
        repository.saveSystemNode(new SystemNodeStatus(
                "Redis-Sync-Cluster",
                emergency ? "DEGRADED" : (24 + random.nextInt(10)) + "ms",
                emergency
                        ? "安全流程复核期间，事件分发已暂停"
                        : "运营缓存与报表分发数据已同步",
                emergency ? "warning" : "stable"
        ));
    }

    private String eventId(String prefix) {
        return prefix + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
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
