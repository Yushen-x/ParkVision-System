package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<ParkingOrder>> orders() {
        return ApiResponse.ok(orderService.listOrders());
    }

    @PostMapping("/entry")
    public ApiResponse<ParkingOrder> entry(@RequestParam(required = false) String plateNo) {
        ParkingOrder order = (plateNo == null || plateNo.isBlank())
                ? orderService.simulateEntry()
                : orderService.entryForPlate(plateNo);
        return ApiResponse.created(order);
    }

    @PostMapping("/{orderNo}/retrieve")
    public ApiResponse<ParkingOrder> retrieve(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.changeStatus(orderNo, OrderStatus.RETRIEVING));
    }

    @PostMapping("/{orderNo}/touch-and-go")
    public ApiResponse<ParkingOrder> touchAndGo(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.changeStatus(orderNo, OrderStatus.TOUCHING));
    }

    @PostMapping("/{orderNo}/touch-complete")
    public ApiResponse<ParkingOrder> completeTouch(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.changeStatus(orderNo, OrderStatus.PARKED));
    }

    @PostMapping("/{orderNo}/pay")
    public ApiResponse<ParkingOrder> pay(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.changeStatus(orderNo, OrderStatus.FINISHED));
    }
}
