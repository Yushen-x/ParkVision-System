package com.parkvision.cps.dto.vision;

public record RecognitionRow(
        String id,
        String time,
        String cameraId,
        String plateNo,
        double confidence,
        String energyType,
        String listType,
        String decision,
        String decisionLabel,
        String reason,
        String orderNo,
        boolean intrusion
) {
}
