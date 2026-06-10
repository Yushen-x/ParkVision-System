package com.parkvision.cps.domain.vision;

import java.time.LocalDateTime;

/**
 * A persisted license-plate recognition event produced by the vision pipeline.
 * Every camera read — whether it resulted in entry, a deny, or a review — is
 * logged here so the AI vision console can show a trustworthy, queryable history
 * instead of client-side simulated data.
 */
public record RecognitionEvent(
        String id,
        String cameraId,
        String plateNo,
        double confidence,
        String energyType,
        String listType,
        String decision,
        String reason,
        String orderNo,
        boolean intrusion,
        LocalDateTime createdAt
) {
}
