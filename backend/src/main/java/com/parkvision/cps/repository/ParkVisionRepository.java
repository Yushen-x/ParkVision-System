package com.parkvision.cps.repository;

import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.ParkingSlot;

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
