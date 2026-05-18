package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.domain.admin.SystemNodeStatus;
import com.parkvision.cps.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final AdminService adminService;

    public SystemController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/nodes")
    public ApiResponse<List<SystemNodeStatus>> nodes() {
        return ApiResponse.ok(adminService.systemNodes());
    }
}
