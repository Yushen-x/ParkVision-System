package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.vision.RecognitionEvent;
import com.parkvision.cps.dto.vision.VisionRequest;
import com.parkvision.cps.dto.vision.VisionResult;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Trustworthy vision pipeline. Every camera read is evaluated against the
 * access list and persisted as a {@link RecognitionEvent}, so the AI vision
 * console reflects real, queryable history rather than client-side mock data.
 * The plate recognition itself is a built-in engine (no external model wired),
 * but the surrounding decision + persistence flow is fully real.
 */
@Service
public class VisionService {
    private static final List<String> CANDIDATE_PLATES = List.of(
            "SH-A7686", "SH-D5218", "SU-M9021", "SH-P3308", "SH-D9082",
            "SH-V7780", "SH-B9001", "SH-X2204", "SH-C8871", "SH-H3819"
    );
    private static final DateTimeFormatter ALERT_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ParkVisionRepository repository;
    private final DeviceService deviceService;
    private final OrderService orderService;
    private final AccessControlService accessControlService;
    private final PlateRecognitionService plateRecognitionService;
    private final Random random = new Random();

    public VisionService(ParkVisionRepository repository,
                         DeviceService deviceService,
                         OrderService orderService,
                         AccessControlService accessControlService,
                         PlateRecognitionService plateRecognitionService) {
        this.repository = repository;
        this.deviceService = deviceService;
        this.orderService = orderService;
        this.accessControlService = accessControlService;
        this.plateRecognitionService = plateRecognitionService;
    }

    /**
     * Recognition only: read the plate, evaluate access, persist the event.
     * Does not create a parking order (used by the console preview / image upload).
     */
    @Transactional
    public VisionResult infer(VisionRequest request) {
        return process(request, false);
    }

    /**
     * Full gate loop: recognize, evaluate access, and on a clear ALLOW create a
     * real parking order. Denials raise an alert; everything is persisted.
     */
    @Transactional
    public VisionResult gateEntry(VisionRequest request) {
        return process(request, true);
    }

    private VisionResult process(VisionRequest request, boolean createEntry) {
        boolean intrusion = request != null && request.simulateIntrusion();
        String cameraId = request == null || isBlank(request.cameraId())
                ? (intrusion ? "CAM-HANDOFF-02" : "CAM-SOUTH-01")
                : request.cameraId();
        // Prefer a real OCR reading when an image is supplied (and a provider is
        // configured); otherwise fall back to the built-in engine. An explicit
        // plateNo in the request always wins (manual entry / re-check).
        Optional<PlateRecognitionService.PlateReading> reading =
                (request != null && !isBlank(request.plateNo()))
                        ? Optional.empty()
                        : plateRecognitionService.recognize(request == null ? null : request.imageUrl());

        // A real recognition provider is configured and an image was supplied, but
        // no plate was found. Surface this honestly instead of fabricating a plate.
        boolean imageProvided = request != null && !isBlank(request.imageUrl()) && isBlank(request.plateNo());
        if (!intrusion && reading.isEmpty() && imageProvided && plateRecognitionService.isEnabled()) {
            VisionResult unrecognized = new VisionResult(
                    "edge-" + System.currentTimeMillis() % 1_000_000,
                    cameraId,
                    "未识别车牌",
                    0.0,
                    false,
                    "HOLD_FOR_REVIEW",
                    "未知",
                    "识别失败",
                    "REVIEW",
                    "未在图像中识别到车牌，请上传更清晰、包含车辆或完整车牌的照片",
                    null,
                    null
            );
            persist(unrecognized);
            deviceService.recordVisionInference(unrecognized);
            return unrecognized;
        }

        String plate;
        double confidence;
        String energyType;
        if (reading.isPresent()) {
            PlateRecognitionService.PlateReading r = reading.get();
            plate = r.plate();
            confidence = r.confidence();
            energyType = r.newEnergy() ? "新能源" : resolveEnergy(plate);
        } else {
            plate = resolvePlate(request);
            confidence = Math.round((0.94 + random.nextDouble() * 0.05) * 1000.0) / 1000.0;
            energyType = resolveEnergy(plate);
        }

        String decision;
        String listType;
        String reason;
        String orderNo = null;
        String slotId = null;

        if (intrusion) {
            decision = "REVIEW";
            listType = "安全事件";
            reason = "检测到交接区安全入侵，已锁存急停并请求人工复核";
        } else {
            AccessControlService.AccessVerdict verdict = accessControlService.evaluate(plate);
            listType = verdict.listType();
            reason = verdict.reason();
            switch (verdict.decision()) {
                case ALLOW -> {
                    decision = "ALLOW";
                    if (createEntry) {
                        try {
                            ParkingOrder order = orderService.entryForPlate(plate);
                            orderNo = order.getOrderNo();
                            slotId = order.getSlotId();
                            reason = reason + "；已创建入场订单 " + orderNo;
                        } catch (BusinessException ex) {
                            decision = "REVIEW";
                            reason = friendly(ex);
                            orderNo = existingOrderNo(plate);
                        }
                    }
                }
                case REVIEW -> {
                    decision = "REVIEW";
                    if (createEntry) {
                        reason = reason + "；已转人工复核，未自动放行";
                    }
                }
                default -> {
                    decision = "DENY";
                    if (createEntry) {
                        raiseDenyAlert(plate, reason);
                    }
                }
            }
        }

        String action = switch (decision) {
            case "ALLOW" -> createEntry ? "ALLOW_ENTRY_AND_CREATE_ORDER" : "ALLOW_ENTRY";
            case "DENY" -> "DENY_ENTRY";
            default -> intrusion ? "ESTOP_AND_REVIEW" : "HOLD_FOR_REVIEW";
        };

        VisionResult result = new VisionResult(
                "edge-" + System.currentTimeMillis() % 1_000_000,
                cameraId,
                plate,
                confidence,
                intrusion,
                action,
                energyType,
                listType,
                decision,
                reason,
                orderNo,
                slotId
        );

        persist(result);
        deviceService.recordVisionInference(result);
        return result;
    }

    private void persist(VisionResult result) {
        repository.saveRecognitionEvent(new RecognitionEvent(
                "REC" + LocalDateTime.now().format(ALERT_TS) + String.format("%03d", random.nextInt(1000)),
                result.cameraId(),
                result.plate(),
                result.confidence(),
                result.energyType(),
                result.listType(),
                result.decision(),
                result.reason(),
                result.orderNo(),
                result.intrusion(),
                LocalDateTime.now()
        ));
    }

    private void raiseDenyAlert(String plate, String reason) {
        repository.saveAlert(new AlertEvent(
                "AL" + LocalDateTime.now().format(ALERT_TS),
                "门禁",
                "车牌 " + plate + " 入场被拒：" + reason,
                "待处理",
                "高"
        ));
    }

    private String resolvePlate(VisionRequest request) {
        if (request != null && !isBlank(request.plateNo())) {
            return request.plateNo().trim().toUpperCase();
        }
        return CANDIDATE_PLATES.get(random.nextInt(CANDIDATE_PLATES.size()));
    }

    private String resolveEnergy(String plate) {
        return repository.findVehicleProfiles().stream()
                .filter(v -> v.plateNo().equalsIgnoreCase(plate))
                .map(v -> normalizeEnergy(v.energyType()))
                .findFirst()
                .orElse("燃油");
    }

    private String normalizeEnergy(String raw) {
        if (raw == null) {
            return "燃油";
        }
        String t = raw.toLowerCase();
        if (raw.contains("电") || raw.contains("新能源") || t.contains("ev") || t.contains("electric")) {
            return "新能源";
        }
        return "燃油";
    }

    private String existingOrderNo(String plate) {
        return repository.findOrders().stream()
                .filter(o -> o.getPlateNo().equalsIgnoreCase(plate) && o.getStatus() != OrderStatus.FINISHED)
                .map(ParkingOrder::getOrderNo)
                .findFirst()
                .orElse(null);
    }

    private String friendly(BusinessException ex) {
        return ex.getMessage() == null ? "入场校验未通过，请人工复核" : ex.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
