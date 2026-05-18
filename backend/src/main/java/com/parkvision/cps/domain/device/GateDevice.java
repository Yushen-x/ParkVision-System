package com.parkvision.cps.domain.device;

import java.time.LocalDateTime;

public record GateDevice(
        String gateId,
        String protocol,
        String endpoint,
        String coilAddress,
        int queueDepth,
        String gateState,
        boolean loopOccupied,
        boolean estopArmed,
        String lastDecision,
        LocalDateTime lastSeen,
        String detail
) {
}
