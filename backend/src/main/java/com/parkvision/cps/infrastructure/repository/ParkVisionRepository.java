package com.parkvision.cps.infrastructure.repository;

import com.parkvision.cps.admin.AlertEvent;
import com.parkvision.cps.admin.PricingRule;
import com.parkvision.cps.dispatch.AgvUnit;
import com.parkvision.cps.dispatch.DispatchTask;
import com.parkvision.cps.order.ParkingOrder;
import com.parkvision.cps.parking.ParkingSlot;

import java.util.List;
import java.util.Optional;

public interface ParkVisionRepository {
    List<ParkingSlot> findSlots();

    Optional<ParkingSlot> findFirstAvailableSlot();

    Optional<ParkingSlot> findFirstDeepOccupiedSlot();

    List<ParkingOrder> findOrders();

    Optional<ParkingOrder> findOrderByNo(String orderNo);

    ParkingOrder saveOrder(ParkingOrder order);

    List<AlertEvent> findAlerts();

    List<PricingRule> findPricingRules();

    List<AgvUnit> findAgvUnits();

    List<DispatchTask> findDispatchQueue();

    DispatchTask enqueueDispatchTask(DispatchTask task);
}
