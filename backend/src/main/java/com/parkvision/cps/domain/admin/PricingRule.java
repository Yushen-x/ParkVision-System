package com.parkvision.cps.domain.admin;

public record PricingRule(String name, String timeRange, String method, String extraPolicy, String status) {
}
