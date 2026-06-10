package com.parkvision.cps.dto.admin;

/**
 * Partial update for an account. Null fields are left unchanged.
 */
public record UpdateUserRequest(
        String displayName,
        String role,
        String status
) {
}
