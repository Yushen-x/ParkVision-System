package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.auth.AppUser;
import com.parkvision.cps.domain.customer.CustomerAccount;
import com.parkvision.cps.domain.customer.VehicleProfile;
import com.parkvision.cps.dto.auth.LoginResponse;
import com.parkvision.cps.dto.auth.RegisterRequest;
import com.parkvision.cps.repository.AuthUserRepository;
import com.parkvision.cps.repository.ParkVisionRepository;
import com.parkvision.cps.security.JwtService;
import com.parkvision.cps.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AuthService {
    private final AuthUserRepository authUserRepository;
    private final ParkVisionRepository repository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;

    public AuthService(
            AuthUserRepository authUserRepository,
            ParkVisionRepository repository,
            PasswordHasher passwordHasher,
            JwtService jwtService
    ) {
        this.authUserRepository = authUserRepository;
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
    }

    public LoginResponse login(String username, String rawPassword) {
        AppUser user = authUserRepository.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new BusinessException("AUTH_FAILED", "账号或密码不正确"));

        if (!user.active()) {
            throw new BusinessException("ACCOUNT_FORBIDDEN", "账号已被冻结，请联系管理员");
        }
        if (!passwordHasher.matches(rawPassword, user.salt(), user.passwordHash())) {
            throw new BusinessException("AUTH_FAILED", "账号或密码不正确");
        }

        authUserRepository.touchLastLogin(user.username());
        String token = jwtService.issueToken(user);
        return new LoginResponse(token, user.username(), user.displayName(), user.role(), jwtService.expirationMinutes());
    }

    /**
     * Owner self-registration: creates the login account, a customer profile and
     * an initial bound vehicle in a single transaction, then signs the new owner in.
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String plate = request.plateNo() == null ? "" : request.plateNo().trim().toUpperCase();
        String displayName = request.displayName() == null ? "" : request.displayName().trim();
        if (username.isEmpty() || displayName.isEmpty() || plate.isEmpty()) {
            throw new BusinessException("INVALID_INPUT", "用户名、姓名、车牌均不能为空");
        }
        if (authUserRepository.existsByUsername(username)) {
            throw new BusinessException("USERNAME_TAKEN", "该用户名已被注册");
        }
        if (repository.findVehicleProfiles().stream().anyMatch(v -> v.plateNo().equalsIgnoreCase(plate))) {
            throw new BusinessException("PLATE_TAKEN", "该车牌已被登记");
        }

        LocalDateTime now = LocalDateTime.now();
        String ownerId = "CUS" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        boolean ev = "EV".equalsIgnoreCase(request.energyType());

        repository.saveCustomerAccount(new CustomerAccount(
                ownerId,
                displayName,
                maskPhone(request.phone()),
                "STANDARD",
                "ACTIVE",
                BigDecimal.ZERO,
                now
        ));
        repository.saveVehicleProfile(new VehicleProfile(
                plate,
                ownerId,
                "PASSENGER",
                ev ? "EV" : "FUEL",
                "TEMPORARY",
                "ALLOW",
                now
        ));

        String salt = passwordHasher.newSalt();
        String hash = passwordHasher.hash(request.password(), salt);
        authUserRepository.insert(new AppUser(
                null, username, displayName, "owner", hash, salt, "ACTIVE", null, now, ownerId
        ));

        AppUser created = authUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("REGISTER_FAILED", "注册失败，请重试"));
        authUserRepository.touchLastLogin(username);
        String token = jwtService.issueToken(created);
        return new LoginResponse(token, created.username(), created.displayName(), created.role(), jwtService.expirationMinutes());
    }

    private String maskPhone(String phone) {
        String value = phone == null ? "" : phone.trim();
        if (value.length() >= 11) {
            return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
        }
        if (value.isEmpty()) {
            return "—";
        }
        return value;
    }
}
