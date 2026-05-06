package com.parkvision.cps.dispatch;

import com.parkvision.cps.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {
    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @GetMapping("/queue")
    public ApiResponse<List<DispatchTask>> queue() {
        return ApiResponse.ok(dispatchService.queue());
    }

    @GetMapping("/agvs")
    public ApiResponse<List<AgvUnit>> agvs() {
        return ApiResponse.ok(dispatchService.agvs());
    }

    @PostMapping("/pre-dispatch")
    public ApiResponse<DispatchTask> preDispatch() {
        return ApiResponse.created(dispatchService.preDispatch());
    }

    @PostMapping("/vip")
    public ApiResponse<DispatchTask> vip() {
        return ApiResponse.created(dispatchService.vip());
    }
}
