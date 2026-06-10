package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.auth.AppUser;
import com.parkvision.cps.dto.admin.AppUserRow;
import com.parkvision.cps.dto.admin.CreateUserRequest;
import com.parkvision.cps.dto.admin.UpdateUserRequest;
import com.parkvision.cps.repository.AuthUserRepository;
import com.parkvision.cps.security.PasswordHasher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Administrator-facing account management: list / create / update / reset
 * password. All mutations persist to the {@code app_user} table.
 */
@Service
public class AccountAdminService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ROLES = Set.of("admin", "owner");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "FROZEN");

    private final AuthUserRepository authUserRepository;
    private final PasswordHasher passwordHasher;

    public AccountAdminService(AuthUserRepository authUserRepository, PasswordHasher passwordHasher) {
        this.authUserRepository = authUserRepository;
        this.passwordHasher = passwordHasher;
    }

    public List<AppUserRow> list() {
        return authUserRepository.findAll().stream().map(this::toRow).toList();
    }

    public AppUserRow create(CreateUserRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String displayName = request.displayName() == null ? "" : request.displayName().trim();
        String role = normalizeRole(request.role());
        if (username.isEmpty() || displayName.isEmpty()) {
            throw new BusinessException("INVALID_INPUT", "用户名和姓名不能为空");
        }
        if (authUserRepository.existsByUsername(username)) {
            throw new BusinessException("USERNAME_TAKEN", "该用户名已存在");
        }
        String salt = passwordHasher.newSalt();
        String hash = passwordHasher.hash(request.password(), salt);
        authUserRepository.insert(new AppUser(
                null, username, displayName, role, hash, salt, "ACTIVE", null, LocalDateTime.now(), null
        ));
        return toRow(authUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("CREATE_FAILED", "创建账号失败")));
    }

    public AppUserRow update(long id, UpdateUserRequest request) {
        AppUser user = require(id);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            authUserRepository.updateDisplayName(id, request.displayName().trim());
        }
        if (request.role() != null && !request.role().isBlank()) {
            authUserRepository.updateRole(id, normalizeRole(request.role()));
        }
        if (request.status() != null && !request.status().isBlank()) {
            String status = request.status().trim().toUpperCase();
            if (!STATUSES.contains(status)) {
                throw new BusinessException("INVALID_STATUS", "状态只能是 ACTIVE 或 FROZEN");
            }
            guardLastActiveAdmin(user, request.role(), status);
            authUserRepository.updateStatus(id, status);
        }
        return toRow(require(id));
    }

    public void resetPassword(long id, String newPassword) {
        require(id);
        String salt = passwordHasher.newSalt();
        String hash = passwordHasher.hash(newPassword, salt);
        authUserRepository.updatePassword(id, hash, salt);
    }

    private void guardLastActiveAdmin(AppUser target, String newRole, String newStatus) {
        boolean staysAdmin = (newRole == null || newRole.isBlank())
                ? "admin".equalsIgnoreCase(target.role())
                : "admin".equalsIgnoreCase(newRole.trim());
        boolean willBeInactive = "FROZEN".equals(newStatus);
        if (!"admin".equalsIgnoreCase(target.role()) || (staysAdmin && !willBeInactive)) {
            return;
        }
        long activeAdmins = authUserRepository.findAll().stream()
                .filter(u -> "admin".equalsIgnoreCase(u.role()) && u.active())
                .count();
        if (activeAdmins <= 1) {
            throw new BusinessException("LAST_ADMIN", "系统至少需要保留一个启用的管理员");
        }
    }

    private AppUser require(long id) {
        return authUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "账号不存在: " + id));
    }

    private String normalizeRole(String role) {
        String value = role == null ? "" : role.trim().toLowerCase();
        if (!ROLES.contains(value)) {
            throw new BusinessException("INVALID_ROLE", "角色只能是 admin 或 owner");
        }
        return value;
    }

    private AppUserRow toRow(AppUser user) {
        return new AppUserRow(
                user.id(),
                user.username(),
                user.displayName(),
                user.role(),
                user.status(),
                user.ownerId(),
                user.lastLogin() == null ? null : user.lastLogin().format(TIME),
                user.createdAt() == null ? null : user.createdAt().format(TIME)
        );
    }
}
