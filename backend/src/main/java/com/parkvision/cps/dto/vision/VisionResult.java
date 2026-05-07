package com.parkvision.cps.dto.vision;

public record VisionResult(
        String requestId,
        String cameraId,
        String plate,
        double confidence,
        boolean intrusion,
        String action
) {
}
