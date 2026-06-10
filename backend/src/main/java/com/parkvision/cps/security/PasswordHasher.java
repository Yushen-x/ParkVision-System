package com.parkvision.cps.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Salted SHA-256 password hashing, matching the security design (passwd_hash + salt).
 */
@Component
public class PasswordHasher {
    private final SecureRandom secureRandom = new SecureRandom();

    public String newSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public String hash(String rawPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    public boolean matches(String rawPassword, String salt, String expectedHash) {
        String actual = hash(rawPassword, salt);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expectedHash == null ? new byte[0] : expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}
