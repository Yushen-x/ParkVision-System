package com.parkvision.cps.domain.device;

import java.time.LocalDateTime;

public record DeviceEvent(
        String eventId,
        String deviceType,
        String deviceId,
        String eventCode,
        String severity,
        String message,
        LocalDateTime eventTime,
        boolean acknowledged
) {
}
