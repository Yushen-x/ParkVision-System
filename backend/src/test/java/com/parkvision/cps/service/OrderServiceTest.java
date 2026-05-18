package com.parkvision.cps.service;

import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.SlotStatus;
import com.parkvision.cps.repository.FallbackParkVisionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTest {

    @Test
    void simulateEntryCreatesOrderAndOccupiesSlot() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        OrderService orderService = new OrderService(repository);

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
        OrderService orderService = new OrderService(repository);

        ParkingOrder updatedOrder = orderService.changeStatus("PV20260506001", OrderStatus.FINISHED);

        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.FINISHED);
        assertThat(updatedOrder.getAmount()).isNotNull();
        assertThat(updatedOrder.getAmount().doubleValue()).isGreaterThan(0);
        assertThat(repository.findSlotById(updatedOrder.getSlotId()))
                .get()
                .extracting(slot -> slot.getStatus())
                .isEqualTo(SlotStatus.EMPTY);
    }
}
