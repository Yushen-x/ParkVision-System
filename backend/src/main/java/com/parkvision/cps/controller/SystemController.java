package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.config.DatabaseSeeder;
import com.parkvision.cps.domain.admin.SystemNodeStatus;
import com.parkvision.cps.service.AdminService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final AdminService adminService;
    private final JdbcTemplate jdbc;
    private final DatabaseSeeder seeder;

    public SystemController(AdminService adminService, JdbcTemplate jdbc, DatabaseSeeder seeder) {
        this.adminService = adminService;
        this.jdbc = jdbc;
        this.seeder = seeder;
    }

    @GetMapping("/nodes")
    public ApiResponse<List<SystemNodeStatus>> nodes() {
        return ApiResponse.ok(adminService.systemNodes());
    }

    @PostMapping("/reset")
    public ApiResponse<String> reset() throws Exception {
        String[] tables = {
                "parking_order", "parking_slot", "reservation", "customer_account",
                "vehicle_profile", "payment_transaction", "order_billing_component",
                "alert_event", "pricing_rule", "access_list_item", "recognition_event",
                "system_node_status", "agv_unit", "dispatch_task", "audit_log",
                "vision_camera", "gate_device", "charging_station", "device_event"
        };
        for (String table : tables) {
            jdbc.execute("TRUNCATE TABLE " + table);
        }
        seeder.run(null);
        return ApiResponse.ok("reset");
    }
}
