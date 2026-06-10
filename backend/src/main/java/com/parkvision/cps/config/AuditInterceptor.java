package com.parkvision.cps.config;

import com.parkvision.cps.domain.admin.AuditLog;
import com.parkvision.cps.repository.ParkVisionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Records an audit entry for every mutating request (POST / PUT / PATCH / DELETE),
 * capturing the authenticated principal, target path and resulting status. This gives
 * a centralized, persisted operation trail without touching each controller.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {
    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final ParkVisionRepository repository;

    public AuditInterceptor(ParkVisionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!MUTATING.contains(request.getMethod())) {
            return;
        }
        String path = request.getRequestURI();
        // Skip the realtime/stream channels and auth probes that aren't business actions.
        if (path == null || path.startsWith("/api/twin/stream")) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())
                ? auth.getName() : "-";
        String role = auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority() : "-";

        try {
            repository.saveAuditLog(new AuditLog(
                    null, username, role, request.getMethod(), path,
                    response.getStatus(), clientIp(request), LocalDateTime.now()
            ));
        } catch (Exception ignored) {
            // Auditing must never break the request lifecycle.
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
