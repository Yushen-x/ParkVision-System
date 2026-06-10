package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.ParkingSlot;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.domain.reservation.Reservation;
import com.parkvision.cps.repository.AuthUserRepository;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parkvision.cps.domain.dispatch.DispatchTask;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Slot reservation lifecycle, fully persisted: holding an empty slot, releasing
 * it, fulfilling it into a parking order, and auto-expiring stale holds.
 */
@Service
public class ReservationService {
    private static final long HOLD_MINUTES = 15;

    private final ParkVisionRepository repository;
    private final AuthUserRepository authUserRepository;
    private final OrderService orderService;
    private final DispatchService dispatchService;

    public ReservationService(ParkVisionRepository repository, AuthUserRepository authUserRepository,
                              OrderService orderService, DispatchService dispatchService) {
        this.repository = repository;
        this.authUserRepository = authUserRepository;
        this.orderService = orderService;
        this.dispatchService = dispatchService;
    }

    public List<Reservation> listForOwner(String username) {
        expireStale();
        String ownerId = ownerIdOf(username);
        return repository.findReservations().stream()
                .filter(item -> Objects.equals(item.ownerId(), ownerId))
                .sorted(Comparator.comparing(Reservation::createdAt).reversed())
                .toList();
    }

    @Transactional
    public Reservation create(String username, String plateRaw, String phone, String energyType) {
        String plate = plateRaw == null ? "" : plateRaw.trim().toUpperCase();
        if (plate.isEmpty()) {
            throw new BusinessException("INVALID_PLATE", "请填写车牌号");
        }
        boolean alreadyInside = repository.findOrders().stream()
                .anyMatch(order -> order.getPlateNo().equalsIgnoreCase(plate)
                        && order.getStatus() != OrderStatus.FINISHED);
        if (alreadyInside) {
            throw new BusinessException("ALREADY_PARKED", plate + " 已在场内，无需预约");
        }
        boolean alreadyHeld = repository.findReservations().stream()
                .anyMatch(item -> "HELD".equals(item.status()) && item.plateNo().equalsIgnoreCase(plate));
        if (alreadyHeld) {
            throw new BusinessException("ALREADY_HELD", plate + " 已有保留中的预约");
        }

        ParkingSlot slot = repository.findFirstAvailableSlot()
                .orElseThrow(() -> new BusinessException("NO_AVAILABLE_SLOT", "当前没有空闲车位可锁定"));
        slot.setStatus(SlotStatus.RESERVED);
        repository.saveSlot(slot);

        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = new Reservation(
                "RSV" + Long.toString(System.currentTimeMillis(), 36).toUpperCase(),
                plate,
                phone == null ? "" : phone.trim(),
                normalizeEnergy(energyType),
                slot.getId(),
                "HELD",
                ownerIdOf(username),
                null,
                now,
                now.plusMinutes(HOLD_MINUTES)
        );
        Reservation saved = repository.saveReservation(reservation);
        repository.enqueueDispatchTask(
                new DispatchTask(plate, "车位预约锁定", "预约", "15:00", false, slot.getId()));
        return saved;
    }

    @Transactional
    public Reservation cancel(String username, String id) {
        Reservation reservation = requireOwned(username, id);
        if (!"HELD".equals(reservation.status())) {
            throw new BusinessException("INVALID_STATE", "该预约无法取消");
        }
        releaseSlot(reservation.slotId());
        Reservation updated = withStatus(reservation, "CANCELLED", reservation.orderNo());
        return repository.saveReservation(updated);
    }

    @Transactional
    public Reservation fulfill(String username, String id) {
        Reservation reservation = requireOwned(username, id);
        if (!"HELD".equals(reservation.status())) {
            throw new BusinessException("INVALID_STATE", "该预约无法到场确认");
        }
        ParkingOrder order = orderService.admitToSlot(reservation.plateNo(), reservation.slotId());
        Reservation updated = withStatus(reservation, "FULFILLED", order.getOrderNo());
        return repository.saveReservation(updated);
    }

    /** Release holds that exceeded the 15-minute window and free their slots. */
    @Scheduled(fixedDelayString = "${parkvision.reservation.expiry-check-ms:60000}")
    @Transactional
    public void expireStale() {
        LocalDateTime now = LocalDateTime.now();
        repository.findReservations().stream()
                .filter(item -> "HELD".equals(item.status()) && item.expiresAt().isBefore(now))
                .forEach(item -> {
                    releaseSlot(item.slotId());
                    repository.saveReservation(withStatus(item, "EXPIRED", item.orderNo()));
                });
    }

    private void releaseSlot(String slotId) {
        repository.findSlotById(slotId).ifPresent(slot -> {
            if (slot.getStatus() == SlotStatus.RESERVED) {
                slot.setStatus(SlotStatus.EMPTY);
                repository.saveSlot(slot);
            }
        });
    }

    private Reservation requireOwned(String username, String id) {
        Reservation reservation = repository.findReservationById(id)
                .orElseThrow(() -> new BusinessException("RESERVATION_NOT_FOUND", "预约不存在: " + id));
        if (!Objects.equals(reservation.ownerId(), ownerIdOf(username))) {
            throw new BusinessException("RESERVATION_NOT_OWNED", "无法操作他人的预约");
        }
        return reservation;
    }

    private Reservation withStatus(Reservation reservation, String status, String orderNo) {
        return new Reservation(
                reservation.id(),
                reservation.plateNo(),
                reservation.phone(),
                reservation.energyType(),
                reservation.slotId(),
                status,
                reservation.ownerId(),
                orderNo,
                reservation.createdAt(),
                reservation.expiresAt()
        );
    }

    private String normalizeEnergy(String energyType) {
        if (energyType == null || energyType.isBlank()) {
            return "FUEL";
        }
        String value = energyType.trim().toUpperCase();
        return value.contains("EV") || value.contains("ELECTRIC") || value.contains("电") ? "EV" : "FUEL";
    }

    private String ownerIdOf(String username) {
        return authUserRepository.findByUsername(username == null ? "" : username.trim())
                .map(user -> user.ownerId())
                .orElse(null);
    }
}
