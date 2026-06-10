package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.dto.parking.ParkingSlotResponse;
import com.parkvision.cps.service.ParkingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ParkingController {
    private final ParkingService parkingService;

    public ParkingController(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    @GetMapping("/slots")
    public ApiResponse<List<ParkingSlotResponse>> slots() {
        return ApiResponse.ok(parkingService.slots());
    }

    @PostMapping("/slots/{slotId}/clear")
    public ApiResponse<ParkingSlotResponse> clearSlot(@PathVariable String slotId) {
        return ApiResponse.ok(parkingService.clearSlot(slotId));
    }
}
