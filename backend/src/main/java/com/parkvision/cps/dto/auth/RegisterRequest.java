package com.parkvision.cps.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Owner self-registration payload. Creates an owner login plus the matching
 * customer account and an initial bound vehicle in one transaction.
 */
public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6, message = "密码至少 6 位") String password,
        @NotBlank String displayName,
        String phone,
        @NotBlank String plateNo,
        String energyType
) {
}
