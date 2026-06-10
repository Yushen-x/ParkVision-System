package com.parkvision.cps.service;

import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.repository.FallbackParkVisionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTest {

    private OrderService newOrderService(FallbackParkVisionRepository repository) {
        DeviceService deviceService = new DeviceService(repository, true);
        BillingService billingService = new BillingService(repository, new PricingService(repository));
        return new OrderService(repository, deviceService, billingService);
    }

    @Test
    void simulateEntryCreatesOrderAndOccupiesSlot() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        OrderService orderService = newOrderService(repository);

        int existingOrders = repository.findOrders().size();
        ParkingOrder order = orderService.simulateEntry();

        assertThat(repository.findOrders()).hasSize(existingOrders + 1);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARKED);
        assertThat(repository.findSlotById(order.getSlotId()))
                .get()
                .extracting(slot -> slot.getStatus())
                .isNotEqualTo(SlotStatus.EMPTY);
    }

    @Test
    void finishingOrderReleasesSlotAndCalculatesAmount() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        OrderService orderService = newOrderService(repository);

        ParkingOrder updatedOrder = orderService.changeStatus("PV20260506001", OrderStatus.FINISHED);

        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.FINISHED);
        assertThat(updatedOrder.getAmount()).isNotNull();
        assertThat(updatedOrder.getAmount().doubleValue()).isGreaterThan(0);
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo("PAID");
        assertThat(updatedOrder.getPaidAt()).isNotNull();
        assertThat(repository.findPaymentByOrderNo(updatedOrder.getOrderNo())).isPresent();
        assertThat(repository.findBillingComponentsByOrderNo(updatedOrder.getOrderNo())).isNotEmpty();
        assertThat(repository.findSlotById(updatedOrder.getSlotId()))
                .get()
                .extracting(slot -> slot.getStatus())
                .isEqualTo(SlotStatus.EMPTY);
    }
}
