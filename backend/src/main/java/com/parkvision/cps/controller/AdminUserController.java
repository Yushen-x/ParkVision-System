package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.dto.admin.AppUserRow;
import com.parkvision.cps.dto.admin.CreateUserRequest;
import com.parkvision.cps.dto.admin.ResetPasswordRequest;
import com.parkvision.cps.dto.admin.UpdateUserRequest;
import com.parkvision.cps.service.AccountAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administrator account management. Secured to ROLE_ADMIN by SecurityConfig
 * (/api/admin/** requires the ADMIN role).
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AccountAdminService accountAdminService;

    public AdminUserController(AccountAdminService accountAdminService) {
        this.accountAdminService = accountAdminService;
    }

    @GetMapping
    public ApiResponse<List<AppUserRow>> list() {
        return ApiResponse.ok(accountAdminService.list());
    }

    @PostMapping
    public ApiResponse<AppUserRow> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.created(accountAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AppUserRow> update(@PathVariable long id, @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(accountAdminService.update(id, request));
    }

    @PostMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable long id, @Valid @RequestBody ResetPasswordRequest request) {
        accountAdminService.resetPassword(id, request.password());
        return ApiResponse.ok(null);
    }
}
