package com.parkvision.cps.service;

import com.parkvision.cps.dto.vision.VisionRequest;
import com.parkvision.cps.dto.vision.VisionResult;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class VisionService {
    private static final List<String> PLATES = List.of("SH-A7686", "SH-D5218", "SU-M9021", "SH-K1314", "SH-V7780");

    private final ParkVisionRepository repository;
    private final DeviceService deviceService;
    private final Random random = new Random();

    public VisionService(ParkVisionRepository repository, DeviceService deviceService) {
        this.repository = repository;
        this.deviceService = deviceService;
    }

    public VisionResult infer(VisionRequest request) {
        boolean intrusion = request != null && request.simulateIntrusion();
        String cameraId = request == null || request.cameraId() == null
                ? (intrusion ? "CAM-HANDOFF-02" : "CAM-SOUTH-01")
                : request.cameraId();
        String plate = repository.findOrders().stream().findFirst().map(order -> order.getPlateNo())
                .orElseGet(() -> PLATES.get(random.nextInt(PLATES.size())));
        double confidence = Math.round((0.94 + random.nextDouble() * 0.05) * 1000.0) / 1000.0;

        VisionResult result = new VisionResult(
                "edge-" + System.currentTimeMillis() % 1_000_000,
                cameraId,
                plate,
                confidence,
                intrusion,
                intrusion ? "ESTOP_AND_REVIEW" : "ALLOW_ENTRY_AND_CREATE_ORDER"
        );
        deviceService.recordVisionInference(result);
        return result;
    }
}
