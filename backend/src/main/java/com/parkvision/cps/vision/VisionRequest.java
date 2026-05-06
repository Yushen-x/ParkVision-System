package com.parkvision.cps.vision;

public record VisionRequest(String cameraId, String imageUrl, boolean simulateIntrusion) {
}
