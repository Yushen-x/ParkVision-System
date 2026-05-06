package com.parkvision.cps.vision;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class VisionService {
    private static final List<String> PLATES = List.of("沪A·P7686", "沪D·E5218", "苏E·M9021", "浙A·K1314", "沪B·V7780");

    private final Random random = new Random();

    public VisionResult infer(VisionRequest request) {
        boolean intrusion = request != null && request.simulateIntrusion();
        String cameraId = request == null || request.cameraId() == null ? "gate-A-01" : request.cameraId();
        String plate = PLATES.get(random.nextInt(PLATES.size()));
        double confidence = Math.round((0.94 + random.nextDouble() * 0.05) * 1000.0) / 1000.0;
        return new VisionResult(
                "edge-" + System.currentTimeMillis() % 1_000_000,
                cameraId,
                plate,
                confidence,
                intrusion,
                intrusion ? "ESTOP_AND_REVIEW" : "ALLOW_ENTRY_AND_CREATE_ORDER"
        );
    }
}
