package com.parkvision.cps.dto.auth;

public record LoginResponse(
        String token,
        String username,
        String displayName,
        String role,
        long expiresInMinutes
) {
}
