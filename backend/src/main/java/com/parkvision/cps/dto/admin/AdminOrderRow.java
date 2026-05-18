package com.parkvision.cps.dto.admin;

public record AdminOrderRow(
        String orderNo,
        String plateNo,
        String event,
        String slotId,
        String status,
        String amount
) {
}
