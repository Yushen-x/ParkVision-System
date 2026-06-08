package com.parkvision.cps.dto.admin;

import java.util.List;

public record AdminAlertDetail(
        String alertNo,
        String type,
        String content,
        String status,
        String level,
        String recommendedAction,
        String escalationState,
        int relatedEventCount,
        int acknowledgedEventCount,
        List<DeviceEventSummary> deviceEvents,
        List<OrderHintSummary> relatedOrders
) {
    public record DeviceEventSummary(
            String eventId,
            String deviceType,
            String deviceId,
            String eventCode,
            String severity,
            String message,
            String eventTime,
            boolean acknowledged
    ) {
    }

    public record OrderHintSummary(
            String orderNo,
            String plateNo,
            String status,
            String paymentStatus,
            String slotId
    ) {
    }
}
