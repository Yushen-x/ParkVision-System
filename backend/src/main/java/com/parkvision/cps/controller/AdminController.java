package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.dto.admin.AdminOrderRow;
import com.parkvision.cps.dto.admin.AdminReport;
import com.parkvision.cps.dto.admin.AdminReportRequest;
import com.parkvision.cps.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/orders")
    public ApiResponse<List<AdminOrderRow>> orders() {
        return ApiResponse.ok(adminService.orderRows());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AlertEvent>> alerts() {
        return ApiResponse.ok(adminService.alerts());
    }

    @GetMapping("/pricing-rules")
    public ApiResponse<List<PricingRule>> pricingRules() {
        return ApiResponse.ok(adminService.pricingRules());
    }

    @GetMapping("/access-list")
    public ApiResponse<List<AccessListItem>> accessList() {
        return ApiResponse.ok(adminService.accessList());
    }

    @PostMapping("/report")
    public ApiResponse<AdminReport> report(@RequestBody(required = false) AdminReportRequest request) {
        return ApiResponse.ok(adminService.buildReport(request == null ? null : request.query()));
    }
}
