package com.parkvision.cps.domain.admin;

import java.time.LocalDateTime;

/**
 * An operation audit record: who performed which mutating request, where it hit,
 * and the resulting HTTP status. Captured centrally for every admin write action.
 */
public record AuditLog(
        Long id,
        String username,
        String role,
        String method,
        String path,
        int status,
        String ip,
        LocalDateTime createdAt
) {
}
