package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.dto.navigation.IndoorNavigationSnapshot;
import com.parkvision.cps.service.NavigationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/navigation")
public class NavigationController {
    private final NavigationService navigationService;

    public NavigationController(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @GetMapping("/indoor")
    public ApiResponse<IndoorNavigationSnapshot> indoor(@RequestParam(required = false) String orderNo) {
        return ApiResponse.ok(navigationService.currentRoute(orderNo));
    }
}
