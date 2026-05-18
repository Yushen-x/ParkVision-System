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
                "A 区交接点",
                "GATE-OUT-01",
                remainingMeters,
                etaSeconds,
                agvEtaSeconds,
                6,
                completedSegments,
                nextInstruction(order.getStatus(), remainingMeters),
                statusLabel(order.getStatus(), leadAgv),
                deviceService.emergencyActive()
                        ? "安全复核正在进行，请等待工作人员确认后再进入交接区。"
                        : "室内路线安全，已与实时 AGV 和闸机遥测同步。"
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
            return "前往交接区，并在倒计时结束前完成物品拿取";
        }
        if (remainingMeters > 120) {
            return "沿入场车道直行后右转，前往交接走廊";
        }
        if (remainingMeters > 70) {
            return "沿主通道前进，并跟随地面指引灯前往 A 区";
        }
        return "交接区就在前方，请减速并等待闸机放行确认";
    }

    private String statusLabel(OrderStatus status, AgvUnit agv) {
        return switch (status) {
            case RETRIEVING -> agv.getId() + " 正在接近放行走廊";
            case TOUCHING -> "车辆已到达交接区，可进行临时取物";
            case FINISHED -> "订单已关闭，路线可重新分配";
            default -> "车辆仍在库内，室内路线处于待命状态";
        };
    }
}
