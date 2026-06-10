package com.parkvision.cps.config;

import com.parkvision.cps.domain.auth.AppUser;
import com.parkvision.cps.repository.AuthUserRepository;
import com.parkvision.cps.security.PasswordHasher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds the default back-office and owner accounts on a fresh database, with
 * salted SHA-256 password hashes. Runs only when the auth table is empty so it
 * never overwrites credentials that an operator may have rotated.
 */
@Component
@Order(5)
public class AuthSeeder implements ApplicationRunner {
    private final AuthUserRepository authUserRepository;
    private final PasswordHasher passwordHasher;

    public AuthSeeder(AuthUserRepository authUserRepository, PasswordHasher passwordHasher) {
        this.authUserRepository = authUserRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (authUserRepository.count() > 0) {
            return;
        }
        List<SeedAccount> defaults = List.of(
                new SeedAccount("admin", "admin123", "admin", "运营管理员", null),
                // Linked to seeded customer CUS0001 (plate SH-A7686) so the owner has a real parked car.
                new SeedAccount("owner", "owner123", "owner", "张车主", "CUS0001")
        );
        defaults.forEach(this::seed);
    }

    private void seed(SeedAccount account) {
        String salt = passwordHasher.newSalt();
        String hash = passwordHasher.hash(account.rawPassword(), salt);
        authUserRepository.insert(new AppUser(
                null,
                account.username(),
                account.displayName(),
                account.role(),
                hash,
                salt,
                "ACTIVE",
                null,
                LocalDateTime.now(),
                account.ownerId()
        ));
    }

    private record SeedAccount(String username, String rawPassword, String role, String displayName, String ownerId) {
    }
}
