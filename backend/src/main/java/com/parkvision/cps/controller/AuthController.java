package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.dto.auth.LoginRequest;
import com.parkvision.cps.dto.auth.LoginResponse;
import com.parkvision.cps.dto.auth.RegisterRequest;
import com.parkvision.cps.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.username(), request.password()));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.created(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.failed("AUTH_REQUIRED", "未登录");
        }
        String role = authentication.getAuthorities().stream()
                .map(Object::toString)
                .filter(value -> value.startsWith("ROLE_"))
                .map(value -> value.substring("ROLE_".length()).toLowerCase())
                .findFirst()
                .orElse("");
        return ApiResponse.ok(Map.of(
                "username", authentication.getName(),
                "role", role
        ));
    }
}
