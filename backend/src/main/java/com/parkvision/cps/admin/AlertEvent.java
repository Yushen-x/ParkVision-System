package com.parkvision.cps.admin;

public record AlertEvent(String alertNo, String type, String content, String status, String level) {
}
