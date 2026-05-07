package com.parkvision.cps.parking;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.parking.dto.ParkingSlotResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
