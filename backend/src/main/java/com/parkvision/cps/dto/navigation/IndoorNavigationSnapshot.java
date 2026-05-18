package com.parkvision.cps.dto.navigation;

public record IndoorNavigationSnapshot(
        String orderNo,
        String plateNo,
        String slotId,
        String handoffZone,
        String targetGate,
        int remainingMeters,
        int etaSeconds,
        int agvEtaSeconds,
        int walkingSpeedKph,
        int completedSegments,
        String nextInstruction,
        String status,
        String safetyMessage
) {
}
