package com.parkvision.cps.service;

import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Resolves an access decision for a recognized plate against the persisted
 * allow/block list. This turns the vision pipeline into a trustworthy gate:
 * the same recognition can be allowed, denied, or flagged for review based on
 * real data instead of a client-side guess.
 */
@Service
public class AccessControlService {

    public enum Decision {
        ALLOW,
        DENY,
        REVIEW
    }

    public record AccessVerdict(Decision decision, String listType, String reason) {
    }

    private final ParkVisionRepository repository;

    public AccessControlService(ParkVisionRepository repository) {
        this.repository = repository;
    }

    public AccessVerdict evaluate(String plateNo) {
        Optional<AccessListItem> match = repository.findAccessListItem(plateNo);
        if (match.isEmpty()) {
            return new AccessVerdict(Decision.ALLOW, "临时车", "未在名单内，按临停车辆放行并计费");
        }

        AccessListItem item = match.get();
        String listType = item.listType() == null ? "" : item.listType();

        if (isBlacklist(listType)) {
            return new AccessVerdict(Decision.DENY, listType,
                    "黑名单拦截：" + safe(item.remark(), "禁止入场"));
        }

        if (isWatchlist(listType)) {
            return new AccessVerdict(Decision.REVIEW, listType,
                    "观察名单：" + safe(item.remark(), "需人工复核后放行"));
        }

        if (isExpired(item.validUntil())) {
            return new AccessVerdict(Decision.DENY, listType,
                    "通行权限已于 " + item.validUntil() + " 过期，请续费");
        }

        return new AccessVerdict(Decision.ALLOW, listType,
                safe(item.remark(), item.userType() + " 自动放行"));
    }

    private boolean isBlacklist(String listType) {
        String t = listType.toLowerCase();
        return listType.contains("黑名单") || t.contains("black") || t.contains("block") || t.contains("deny");
    }

    private boolean isWatchlist(String listType) {
        String t = listType.toLowerCase();
        return listType.contains("观察") || t.contains("watch") || t.contains("review");
    }

    private boolean isExpired(String validUntil) {
        if (validUntil == null || validUntil.isBlank()) {
            return false;
        }
        try {
            return LocalDate.parse(validUntil.trim()).isBefore(LocalDate.now());
        } catch (DateTimeParseException ex) {
            // Non-date validity (e.g. "单次订单"/"人工复核") is treated as non-expiring.
            return false;
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
