package com.parkvision.cps.security;

import com.parkvision.cps.domain.auth.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String INSECURE_DEFAULT =
            "parkvision-cps-demo-secret-key-please-override-in-production-0123456789";

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${parkvision.security.jwt.secret}") String secret,
            @Value("${parkvision.security.jwt.expiration-minutes:720}") long expirationMinutes
    ) {
        if (INSECURE_DEFAULT.equals(secret)) {
            log.warn("[SECURITY] 正在使用内置默认 JWT 密钥，生产环境必须通过环境变量 PARKVISION_JWT_SECRET 覆盖！");
        } else if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            log.warn("[SECURITY] JWT 密钥长度不足 32 字节，建议使用更长的随机密钥。");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String issueToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(expirationMinutes));
        return Jwts.builder()
                .subject(user.username())
                .claim("role", user.role())
                .claim("name", user.displayName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long expirationMinutes() {
        return expirationMinutes;
    }
}
