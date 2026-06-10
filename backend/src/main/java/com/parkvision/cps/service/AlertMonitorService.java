package com.parkvision.cps.service;

import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.parking.ParkingSlot;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Observability: periodically inspects authoritative state (occupancy, AGV battery,
 * device/node health) and raises persisted alerts automatically — no manual trigger.
 * A per-rule cooldown prevents the same condition from spamming the alert log.
 */
@Service
public class AlertMonitorService {
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ParkVisionRepository repository;
    private final boolean enabled;
    private final int occupancyThreshold;
    private final int batteryThreshold;
    private final long cooldownMs;
    private final Map<String, Long> lastRaised = new ConcurrentHashMap<>();

    public AlertMonitorService(
            ParkVisionRepository repository,
            @Value("${parkvision.monitor.enabled:true}") boolean enabled,
            @Value("${parkvision.monitor.occupancy-threshold:90}") int occupancyThreshold,
            @Value("${parkvision.monitor.battery-threshold:15}") int batteryThreshold,
            @Value("${parkvision.monitor.cooldown-ms:300000}") long cooldownMs
    ) {
        this.repository = repository;
        this.enabled = enabled;
        this.occupancyThreshold = occupancyThreshold;
        this.batteryThreshold = batteryThreshold;
        this.cooldownMs = cooldownMs;
    }

    @Scheduled(fixedDelayString = "${parkvision.monitor.interval-ms:15000}")
    public void scan() {
        if (!enabled) {
            return;
        }

        List<ParkingSlot> slots = repository.findSlots();
        if (!slots.isEmpty()) {
            long occupied = slots.stream().filter(s -> s.getStatus() != SlotStatus.EMPTY).count();
            int rate = (int) Math.round(occupied * 100.0 / slots.size());
            if (rate >= occupancyThreshold) {
                raise("OCCUPANCY_HIGH", "运营",
                        "车位占用率已达 " + rate + "%，接近满库，建议启动预调度疏导", "中");
            }
        }

        repository.findAgvUnits().forEach(agv -> {
            if (agv.getBatteryPct() <= batteryThreshold) {
                raise("BATTERY_" + agv.getId(), "设备",
                        agv.getId() + " 电量仅 " + agv.getBatteryPct() + "%，已低于阈值，需尽快补能", "中");
            }
        });

        repository.findCameraDevices().forEach(c -> {
            if ("OFFLINE".equalsIgnoreCase(c.status())) {
                raise("OFFLINE_CAM_" + c.cameraId(), "设备",
                        "摄像头 " + c.cameraId() + " 处于离线状态，影响入场识别", "高");
            }
        });
        repository.findGateDevices().forEach(g -> {
            if ("OFFLINE".equalsIgnoreCase(g.gateState())) {
                raise("OFFLINE_GATE_" + g.gateId(), "设备",
                        "闸机 " + g.gateId() + " 处于离线状态，影响车辆放行", "高");
            }
        });
        repository.findChargingStations().forEach(s -> {
            if ("OFFLINE".equalsIgnoreCase(s.connectorStatus())) {
                raise("OFFLINE_CHG_" + s.chargerId(), "设备",
                        "充电桩 " + s.chargerId() + " 处于离线状态，新能源车位充电受影响", "中");
            }
        });

        repository.findSystemNodes().forEach(node -> {
            if ("warning".equalsIgnoreCase(node.level()) || "critical".equalsIgnoreCase(node.level())) {
                raise("NODE_" + node.name(), "系统",
                        "节点 " + node.name() + " 状态异常（" + node.detail() + "）", "高");
            }
        });
    }

    private void raise(String key, String type, String content, String level) {
        long now = System.currentTimeMillis();
        Long last = lastRaised.get(key);
        if (last != null && now - last < cooldownMs) {
            return;
        }
        lastRaised.put(key, now);
        String alertNo = "AL" + LocalDateTime.now().format(NO_FMT) + String.format("%03d", Math.abs(key.hashCode()) % 1000);
        repository.saveAlert(new AlertEvent(alertNo, type, content, "待处理", level));
    }
}
