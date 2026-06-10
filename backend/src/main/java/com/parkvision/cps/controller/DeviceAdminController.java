package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.dto.device.DeviceOverview;
import com.parkvision.cps.service.DeviceService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only device health control (ONLINE / OFFLINE / MAINTENANCE).
 * Mapped under /api/admin so the security chain restricts it to ADMIN.
 */
@RestController
@RequestMapping("/api/admin/devices")
public class DeviceAdminController {
    private final DeviceService deviceService;

    public DeviceAdminController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/{type}/{deviceId}/status")
    public ApiResponse<DeviceOverview> setStatus(
            @PathVariable String type,
            @PathVariable String deviceId,
            @RequestParam String status
    ) {
        return ApiResponse.ok(deviceService.setDeviceStatus(type, deviceId, status));
    }
}
