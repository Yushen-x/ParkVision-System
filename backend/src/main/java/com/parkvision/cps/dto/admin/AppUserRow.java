package com.parkvision.cps.dto.admin;

/**
 * Back-office view of an account. Never carries the password hash or salt.
 */
public record AppUserRow(
        long id,
        String username,
        String displayName,
        String role,
        String status,
        String ownerId,
        String lastLogin,
        String createdAt
) {
}
