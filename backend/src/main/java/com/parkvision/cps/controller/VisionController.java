package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.dto.vision.VisionRequest;
import com.parkvision.cps.dto.vision.VisionResult;
import com.parkvision.cps.service.VisionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/edge/vision")
public class VisionController {
    private final VisionService visionService;

    public VisionController(VisionService visionService) {
        this.visionService = visionService;
    }

    @PostMapping("/infer")
    public ApiResponse<VisionResult> infer(@RequestBody(required = false) VisionRequest request) {
        return ApiResponse.ok(visionService.infer(request));
    }
}
