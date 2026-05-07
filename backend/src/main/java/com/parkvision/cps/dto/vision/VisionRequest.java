package com.parkvision.cps.dto.vision;

public record VisionRequest(String cameraId, String imageUrl, boolean simulateIntrusion) {
}
