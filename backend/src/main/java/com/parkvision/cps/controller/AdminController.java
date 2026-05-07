package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ApiResponse<List<List<String>>> orders() {
        return ApiResponse.ok(adminService.orderTable());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<List<String>>> alerts() {
        return ApiResponse.ok(adminService.alertTable());
    }

    @GetMapping("/pricing-rules")
    public ApiResponse<List<PricingRule>> pricingRules() {
        return ApiResponse.ok(adminService.pricingRules());
    }
}
