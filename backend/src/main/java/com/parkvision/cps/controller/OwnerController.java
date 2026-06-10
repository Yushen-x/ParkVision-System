package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.customer.VehicleProfile;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.reservation.Reservation;
import com.parkvision.cps.dto.owner.OrderBill;
import com.parkvision.cps.dto.owner.OwnerProfile;
import com.parkvision.cps.dto.owner.OwnerWallet;
import com.parkvision.cps.dto.owner.RechargeRequest;
import com.parkvision.cps.dto.owner.ReservationRequest;
import com.parkvision.cps.service.OwnerService;
import com.parkvision.cps.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Owner self-service endpoints. All actions are scoped to the authenticated
 * owner via the JWT identity, so an owner can only act on their own data.
 */
@RestController
@RequestMapping("/api/owner")
public class OwnerController {
    private final OwnerService ownerService;
    private final ReservationService reservationService;

    public OwnerController(OwnerService ownerService, ReservationService reservationService) {
        this.ownerService = ownerService;
        this.reservationService = reservationService;
    }

    @GetMapping("/me")
    public ApiResponse<OwnerProfile> me(Authentication authentication) {
        return ApiResponse.ok(ownerService.profile(currentUser(authentication)));
    }

    @GetMapping("/vehicles")
    public ApiResponse<List<VehicleProfile>> vehicles(Authentication authentication) {
        return ApiResponse.ok(ownerService.vehicles(currentUser(authentication)));
    }

    @GetMapping("/orders")
    public ApiResponse<List<ParkingOrder>> orders(Authentication authentication) {
        return ApiResponse.ok(ownerService.orders(currentUser(authentication)));
    }

    @PostMapping("/entry")
    public ApiResponse<ParkingOrder> entry(Authentication authentication, @RequestParam String plateNo) {
        return ApiResponse.created(ownerService.entry(currentUser(authentication), plateNo));
    }

    @PostMapping("/orders/{orderNo}/retrieve")
    public ApiResponse<ParkingOrder> retrieve(Authentication authentication, @PathVariable String orderNo) {
        return ApiResponse.ok(ownerService.changeOwnOrder(currentUser(authentication), orderNo, OrderStatus.RETRIEVING));
    }

    @PostMapping("/orders/{orderNo}/touch-and-go")
    public ApiResponse<ParkingOrder> touchAndGo(Authentication authentication, @PathVariable String orderNo) {
        return ApiResponse.ok(ownerService.changeOwnOrder(currentUser(authentication), orderNo, OrderStatus.TOUCHING));
    }

    @PostMapping("/orders/{orderNo}/pay")
    public ApiResponse<ParkingOrder> pay(Authentication authentication, @PathVariable String orderNo) {
        return ApiResponse.ok(ownerService.changeOwnOrder(currentUser(authentication), orderNo, OrderStatus.FINISHED));
    }

    @GetMapping("/reservations")
    public ApiResponse<List<Reservation>> reservations(Authentication authentication) {
        return ApiResponse.ok(reservationService.listForOwner(currentUser(authentication)));
    }

    @PostMapping("/reservations")
    public ApiResponse<Reservation> createReservation(Authentication authentication, @Valid @RequestBody ReservationRequest request) {
        return ApiResponse.created(reservationService.create(
                currentUser(authentication), request.plateNo(), request.phone(), request.energyType()));
    }

    @PostMapping("/reservations/{id}/cancel")
    public ApiResponse<Reservation> cancelReservation(Authentication authentication, @PathVariable String id) {
        return ApiResponse.ok(reservationService.cancel(currentUser(authentication), id));
    }

    @PostMapping("/reservations/{id}/fulfill")
    public ApiResponse<Reservation> fulfillReservation(Authentication authentication, @PathVariable String id) {
        return ApiResponse.ok(reservationService.fulfill(currentUser(authentication), id));
    }

    @GetMapping("/wallet")
    public ApiResponse<OwnerWallet> wallet(Authentication authentication) {
        return ApiResponse.ok(ownerService.wallet(currentUser(authentication)));
    }

    @PostMapping("/wallet/recharge")
    public ApiResponse<OwnerWallet> recharge(Authentication authentication, @Valid @RequestBody RechargeRequest request) {
        return ApiResponse.ok(ownerService.recharge(currentUser(authentication), request.amount()));
    }

    @GetMapping("/orders/{orderNo}/bill")
    public ApiResponse<OrderBill> bill(Authentication authentication, @PathVariable String orderNo) {
        return ApiResponse.ok(ownerService.bill(currentUser(authentication), orderNo));
    }

    private String currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("AUTH_REQUIRED", "请先登录");
        }
        return authentication.getName();
    }
}
