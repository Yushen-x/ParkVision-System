package com.parkvision.cps.domain.auth;

import java.time.LocalDateTime;

public record AppUser(
        Long id,
        String username,
        String displayName,
        String role,
        String passwordHash,
        String salt,
        String status,
        LocalDateTime lastLogin,
        LocalDateTime createdAt,
        String ownerId
) {
    public boolean active() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
