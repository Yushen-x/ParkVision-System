package com.parkvision.cps.domain.admin;

public record AccessListItem(String plateNo, String listType, String userType, String validUntil, String remark) {
}
