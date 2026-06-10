package com.parkvision.cps.domain.reservation;

import java.time.LocalDateTime;

/**
 * A held parking slot reservation. Lifecycle: HELD -> FULFILLED (car arrived,
 * order created) / CANCELLED (owner released) / EXPIRED (hold timed out).
 */
public record Reservation(
        String id,
        String plateNo,
        String phone,
        String energyType,
        String slotId,
        String status,
        String ownerId,
        String orderNo,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
}
