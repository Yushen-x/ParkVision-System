package com.parkvision.cps.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6, message = "密码至少 6 位") String password,
        @NotBlank String displayName,
        @NotBlank String role
) {
}
