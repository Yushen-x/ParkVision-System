package com.parkvision.cps.dto.vision;

public record VisionRequest(String cameraId, String plateNo, String imageUrl, boolean simulateIntrusion) {
}
