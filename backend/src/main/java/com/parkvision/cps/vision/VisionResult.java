package com.parkvision.cps.vision;

public record VisionResult(
        String requestId,
        String cameraId,
        String plate,
        double confidence,
        boolean intrusion,
        String action
) {
}
