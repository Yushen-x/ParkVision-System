package com.parkvision.cps.service;

import com.parkvision.cps.domain.device.CameraDevice;
import com.parkvision.cps.repository.FallbackParkVisionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AlertMonitorServiceTest {

    private CameraDevice offline(CameraDevice camera) {
        return new CameraDevice(
                camera.cameraId(), camera.profile(), camera.codec(), camera.streamUrl(),
                camera.fps(), camera.bitrateKbps(), "OFFLINE", camera.lastPlate(),
                LocalDateTime.now(), camera.tamperAlarm(), camera.intrusionState(), "测试离线"
        );
    }

    @Test
    void offlineDeviceRaisesAutoAlertOnce() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        // 5 minute cooldown so a second scan within the test does not duplicate.
        AlertMonitorService monitor = new AlertMonitorService(repository, true, 90, 15, 300000);

        CameraDevice camera = repository.findCameraDevices().get(0);
        repository.saveCameraDevice(offline(camera));

        long before = repository.findAlerts().stream()
                .filter(a -> a.content().contains("处于离线状态"))
                .count();

        monitor.scan();
        long afterFirst = repository.findAlerts().stream()
                .filter(a -> a.content().contains("处于离线状态"))
                .count();
        assertThat(afterFirst).isGreaterThan(before);

        // Second scan within cooldown should not raise another alert for the same device.
        monitor.scan();
        long afterSecond = repository.findAlerts().stream()
                .filter(a -> a.content().contains("处于离线状态"))
                .count();
        assertThat(afterSecond).isEqualTo(afterFirst);
    }

    @Test
    void highOccupancyRaisesAutoAlert() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        // Threshold 0 → any occupancy triggers, proving the rule fires and persists.
        AlertMonitorService monitor = new AlertMonitorService(repository, true, 0, 15, 300000);

        monitor.scan();

        assertThat(repository.findAlerts())
                .anyMatch(a -> a.content().contains("占用率"));
    }

    @Test
    void disabledMonitorDoesNothing() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        AlertMonitorService monitor = new AlertMonitorService(repository, false, 0, 100, 0);

        int before = repository.findAlerts().size();
        monitor.scan();
        assertThat(repository.findAlerts()).hasSize(before);
    }
}
