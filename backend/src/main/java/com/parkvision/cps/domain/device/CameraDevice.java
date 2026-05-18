package com.parkvision.cps.domain.device;

import java.time.LocalDateTime;

public record CameraDevice(
        String cameraId,
        String profile,
        String codec,
        String streamUrl,
        int fps,
        int bitrateKbps,
        String status,
        String lastPlate,
        LocalDateTime lastSeen,
        boolean tamperAlarm,
        boolean intrusionState,
        String detail
) {
}
