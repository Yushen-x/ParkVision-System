package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.dto.device.DeviceOverview;
import com.parkvision.cps.service.DeviceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/overview")
    public ApiResponse<DeviceOverview> overview() {
        return ApiResponse.ok(deviceService.overview());
    }

    @PostMapping("/emergency")
    public ApiResponse<Boolean> emergency(@RequestParam boolean active) {
        return ApiResponse.ok(deviceService.setEmergency(active));
    }
}
