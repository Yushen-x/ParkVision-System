package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.domain.billing.PaymentTransaction;
import com.parkvision.cps.dto.admin.AdminAlertDetail;
import com.parkvision.cps.dto.admin.AdminBillingComponentRow;
import com.parkvision.cps.dto.admin.AdminCustomerDetail;
import com.parkvision.cps.dto.admin.AdminCustomerVehicleRow;
import com.parkvision.cps.dto.admin.AdminOrderDetail;
import com.parkvision.cps.dto.admin.AdminOverviewMetrics;
import com.parkvision.cps.dto.admin.AdminOrderRow;
import com.parkvision.cps.dto.admin.AdminReport;
import com.parkvision.cps.dto.admin.AdminReportRequest;
import com.parkvision.cps.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ApiResponse<List<AdminOrderRow>> orders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        return ApiResponse.ok(adminService.orderRows(status, keyword, dateFrom, dateTo));
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewMetrics> overview() {
        return ApiResponse.ok(adminService.overviewMetrics());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AlertEvent>> alerts(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(adminService.alerts(level, status, keyword));
    }

    @GetMapping("/alerts/{alertNo}/detail")
    public ApiResponse<AdminAlertDetail> alertDetail(@PathVariable String alertNo) {
        return ApiResponse.ok(adminService.alertDetail(alertNo));
    }

    @GetMapping("/pricing-rules")
    public ApiResponse<List<PricingRule>> pricingRules() {
        return ApiResponse.ok(adminService.pricingRules());
    }

    @GetMapping("/access-list")
    public ApiResponse<List<AccessListItem>> accessList() {
        return ApiResponse.ok(adminService.accessList());
    }

    @GetMapping("/customer-vehicles")
    public ApiResponse<List<AdminCustomerVehicleRow>> customerVehicles(
            @RequestParam(required = false) String energyType,
            @RequestParam(required = false) String memberLevel,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(adminService.customerVehicles(energyType, memberLevel, keyword));
    }

    @GetMapping("/customers/{ownerId}/detail")
    public ApiResponse<AdminCustomerDetail> customerDetail(@PathVariable String ownerId) {
        return ApiResponse.ok(adminService.customerDetail(ownerId));
    }

    @GetMapping("/payments")
    public ApiResponse<List<PaymentTransaction>> payments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        return ApiResponse.ok(adminService.payments(status, method, keyword, dateFrom, dateTo));
    }

    @GetMapping("/orders/{orderNo}/billing-components")
    public ApiResponse<List<AdminBillingComponentRow>> billingComponents(@PathVariable String orderNo) {
        return ApiResponse.ok(adminService.billingComponents(orderNo));
    }

    @GetMapping("/orders/{orderNo}/detail")
    public ApiResponse<AdminOrderDetail> orderDetail(@PathVariable String orderNo) {
        return ApiResponse.ok(adminService.orderDetail(orderNo));
    }

    @PostMapping("/report")
    public ApiResponse<AdminReport> report(@RequestBody(required = false) AdminReportRequest request) {
        return ApiResponse.ok(adminService.buildReport(request == null ? null : request.query()));
    }
}
