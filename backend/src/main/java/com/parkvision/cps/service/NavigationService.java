package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.ParkingSlot;
import com.parkvision.cps.dto.navigation.IndoorNavigationSnapshot;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;

@Service
public class NavigationService {
    private final ParkVisionRepository repository;
    private final DeviceService deviceService;

    public NavigationService(ParkVisionRepository repository, DeviceService deviceService) {
        this.repository = repository;
        this.deviceService = deviceService;
    }

    public IndoorNavigationSnapshot currentRoute(String orderNo) {
        ParkingOrder order = resolveOrder(orderNo);
        ParkingSlot slot = repository.findSlotById(order.getSlotId())
                .orElseThrow(() -> new BusinessException("SLOT_NOT_FOUND", "Slot not found: " + order.getSlotId()));
        AgvUnit leadAgv = repository.findAgvUnits().stream().findFirst()
                .orElseThrow(() -> new BusinessException("AGV_NOT_FOUND", "No AGV telemetry is available"));

        int baseMeters = switch (slot.getLayer()) {
            case "Deep" -> 148;
            case "Mid" -> 124;
            default -> 96;
        };
        int remainingMeters = Math.max(42, baseMeters - Math.round((leadAgv.getX() + leadAgv.getY()) / 3.2f));
        int etaSeconds = Math.max(45, remainingMeters * 3);
        int agvEtaSeconds = Math.max(30, Math.round(remainingMeters / Math.max(0.55f, (float) leadAgv.getVelocityMps()) * 0.6f));
        int completedSegments = switch (order.getStatus()) {
            case TOUCHING, FINISHED -> 3;
            case RETRIEVING -> 2;
            default -> 1;
        };

        return new IndoorNavigationSnapshot(
                order.getOrderNo(),
                order.getPlateNo(),
                order.getSlotId(),
                "Zone A handoff",
                "GATE-OUT-01",
                remainingMeters,
                etaSeconds,
                agvEtaSeconds,
                6,
                completedSegments,
                nextInstruction(order.getStatus(), remainingMeters),
                statusLabel(order.getStatus(), leadAgv),
                deviceService.emergencyActive()
                        ? "Safety review is active. Wait for staff clearance before entering the handoff zone."
                        : "Indoor route is clear and synchronized with live AGV and gate telemetry."
        );
    }

    private ParkingOrder resolveOrder(String orderNo) {
        if (orderNo != null && !orderNo.isBlank()) {
            return repository.findOrderByNo(orderNo)
                    .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found: " + orderNo));
        }
        return repository.findOrders().stream()
                .filter(order -> order.getStatus() != OrderStatus.FINISHED)
                .findFirst()
                .orElseGet(() -> repository.findOrders().stream().findFirst()
                        .orElseThrow(() -> new BusinessException("NO_ORDER", "No order is available for navigation")));
    }

    private String nextInstruction(OrderStatus status, int remainingMeters) {
        if (status == OrderStatus.TOUCHING) {
            return "Proceed to the handoff bay and collect temporary items before the timer expires";
        }
        if (remainingMeters > 120) {
            return "Keep straight past the inbound lane, then turn right toward the handoff corridor";
        }
        if (remainingMeters > 70) {
            return "Stay on the main aisle and follow the illuminated path markers to Zone A";
        }
        return "The handoff bay is ahead. Slow down and prepare for gate release confirmation";
    }

    private String statusLabel(OrderStatus status, AgvUnit agv) {
        return switch (status) {
            case RETRIEVING -> agv.getId() + " is approaching the release corridor";
            case TOUCHING -> "Vehicle is parked at the handoff bay for temporary access";
            case FINISHED -> "Order is closed and the route can be reused";
            default -> "Vehicle is still stored and the indoor route is in standby";
        };
    }
}
