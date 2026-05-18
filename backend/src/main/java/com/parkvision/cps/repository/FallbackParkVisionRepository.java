package com.parkvision.cps.repository;

import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.domain.admin.SystemNodeStatus;
import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.ParkingSlot;
import com.parkvision.cps.domain.parking.SlotStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "parkvision.persistence", name = "mode", havingValue = "fallback", matchIfMissing = true)
public class FallbackParkVisionRepository implements ParkVisionRepository {
    private final List<ParkingSlot> slots = new ArrayList<>();
    private final List<ParkingOrder> orders = new ArrayList<>();
    private final List<AlertEvent> alerts = new ArrayList<>();
    private final List<PricingRule> pricingRules = new ArrayList<>();
    private final List<AccessListItem> accessList = new ArrayList<>();
    private final List<SystemNodeStatus> systemNodes = new ArrayList<>();
    private final List<AgvUnit> agvUnits = new ArrayList<>();
    private final List<DispatchTask> dispatchQueue = new ArrayList<>();

    public FallbackParkVisionRepository() {
        seedParkingSlots();
        seedOrders();
        seedAdminData();
        seedDispatchData();
    }

    @Override
    public List<ParkingSlot> findSlots() {
        return Collections.unmodifiableList(slots);
    }

    @Override
    public Optional<ParkingSlot> findSlotById(String slotId) {
        return slots.stream().filter(slot -> slot.getId().equals(slotId)).findFirst();
    }

    @Override
    public Optional<ParkingSlot> findFirstAvailableSlot() {
        return slots.stream().filter(slot -> slot.getStatus() == SlotStatus.EMPTY).findFirst();
    }

    @Override
    public Optional<ParkingSlot> findFirstDeepOccupiedSlot() {
        return slots.stream()
                .filter(slot -> "Deep".equals(slot.getLayer()))
                .filter(slot -> slot.getStatus() == SlotStatus.OCCUPIED)
                .findFirst();
    }

    @Override
    public List<ParkingOrder> findOrders() {
        return Collections.unmodifiableList(orders);
    }

    @Override
    public Optional<ParkingOrder> findOrderByNo(String orderNo) {
        return orders.stream().filter(order -> order.getOrderNo().equals(orderNo)).findFirst();
    }

    @Override
    public ParkingOrder saveOrder(ParkingOrder order) {
        orders.removeIf(existing -> existing.getOrderNo().equals(order.getOrderNo()));
        orders.add(0, order);
        return order;
    }

    @Override
    public List<AlertEvent> findAlerts() {
        return Collections.unmodifiableList(alerts);
    }

    @Override
    public List<PricingRule> findPricingRules() {
        return Collections.unmodifiableList(pricingRules);
    }

    @Override
    public List<AccessListItem> findAccessList() {
        return Collections.unmodifiableList(accessList);
    }

    @Override
    public List<SystemNodeStatus> findSystemNodes() {
        return Collections.unmodifiableList(systemNodes);
    }

    @Override
    public List<AgvUnit> findAgvUnits() {
        return Collections.unmodifiableList(agvUnits);
    }

    @Override
    public List<DispatchTask> findDispatchQueue() {
        return Collections.unmodifiableList(dispatchQueue);
    }

    @Override
    public DispatchTask enqueueDispatchTask(DispatchTask task) {
        dispatchQueue.add(0, task);
        return task;
    }

    private void seedParkingSlots() {
        SlotStatus[] statusCycle = {
                SlotStatus.EMPTY, SlotStatus.OCCUPIED, SlotStatus.OCCUPIED, SlotStatus.EMPTY,
                SlotStatus.CHARGING, SlotStatus.BUFFER, SlotStatus.OCCUPIED, SlotStatus.EMPTY
        };
        for (int index = 0; index < 72; index++) {
            String id = "%s%02d".formatted((char) ('A' + index / 12), index % 12 + 1);
            String layer = index < 24 ? "Shallow" : index < 48 ? "Mid" : "Deep";
            SlotStatus status = index == 64 ? SlotStatus.MAINTENANCE : statusCycle[index % statusCycle.length];
            slots.add(new ParkingSlot(id, layer, status));
        }

        updateSlot("A09", SlotStatus.BUFFER);
        updateSlot("B01", SlotStatus.EMPTY);
        updateSlot("C05", SlotStatus.CHARGING);
        updateSlot("E06", SlotStatus.OCCUPIED);
    }

    private void seedOrders() {
        orders.add(new ParkingOrder("PV20260506001", "SH-A7686", "E06", LocalDateTime.now().minusHours(2), OrderStatus.PARKED, new BigDecimal("18.00")));
        orders.add(new ParkingOrder("PV20260506002", "SH-D5218", "C05", LocalDateTime.now().minusHours(4), OrderStatus.RETRIEVING, new BigDecimal("42.50")));
        orders.add(new ParkingOrder("PV20260506003", "SU-M9021", "A09", LocalDateTime.now().minusHours(3), OrderStatus.TOUCHING, new BigDecimal("25.00")));
        orders.add(new ParkingOrder("PV20260506004", "SH-K1314", "B01", LocalDateTime.now().minusHours(1), OrderStatus.FINISHED, new BigDecimal("16.00")));
    }

    private void seedAdminData() {
        alerts.add(new AlertEvent("AL2026050601", "Safety", "Person intrusion detected in transfer zone", "Emergency stop", "High"));
        alerts.add(new AlertEvent("AL2026050602", "Device", "AGV-04 battery below 20%", "Processing", "Medium"));
        alerts.add(new AlertEvent("AL2026050603", "Order", "Secondary plate recognition mismatch", "Pending review", "Medium"));

        pricingRules.add(new PricingRule("Workday hourly", "07:00-22:00", "First hour 6, then 4/hour", "Cap 48", "Active"));
        pricingRules.add(new PricingRule("Night package", "22:00-07:00", "12 flat rate", "Monthly pass exempt", "Active"));
        pricingRules.add(new PricingRule("VIP retrieval", "All day", "Base fee + 8", "Queue weight +40", "Active"));
        pricingRules.add(new PricingRule("EV charging", "All day", "1.2/kWh", "Auto release when full", "Active"));

        accessList.add(new AccessListItem("SH-A7686", "Whitelist", "Monthly member", "2026-12-31", "Auto pass"));
        accessList.add(new AccessListItem("SH-D5218", "Whitelist", "EV owner", "2026-09-01", "Charging priority"));
        accessList.add(new AccessListItem("SU-M9021", "Normal", "Temporary visitor", "Single order", "Supports contactless pay"));
        accessList.add(new AccessListItem("SH-B9001", "Blacklist", "Payment exception", "Manual review", "Entry blocked"));

        systemNodes.add(new SystemNodeStatus("Edge-Cam-01", "98ms", "South gate vision pre-processing node is healthy", "stable"));
        systemNodes.add(new SystemNodeStatus("PLC-Master-Controller", "12ms", "AGV fleet gateway heartbeat is stable", "stable"));
        systemNodes.add(new SystemNodeStatus("Redis-Sync-Cluster", "TIMEOUT", "Cache sync timeout, degraded strategy enabled", "warning"));
    }

    private void seedDispatchData() {
        agvUnits.add(new AgvUnit("AGV-01", 10, 12, false, "Patrolling Zone A"));
        agvUnits.add(new AgvUnit("AGV-02", 45, 32, true, "Carrying SH-A7686"));
        agvUnits.add(new AgvUnit("AGV-03", 72, 58, false, "Heading to shallow buffer"));
        agvUnits.add(new AgvUnit("AGV-04", 28, 76, false, "Charging standby"));

        dispatchQueue.add(new DispatchTask("SH-A7686", "Standard retrieval", "FIFO", "04:12", false));
        dispatchQueue.add(new DispatchTask("SH-D5218", "Charging bay release", "Charging done", "03:40", false));
        dispatchQueue.add(new DispatchTask("SU-M9021", "Touch-and-Go", "Touch", "02:10", false));
        dispatchQueue.add(new DispatchTask("SH-V7780", "Reserved outbound", "Booking", "01:58", false));
    }

    private void updateSlot(String slotId, SlotStatus status) {
        findSlotById(slotId).ifPresent(slot -> slot.setStatus(status));
    }
}
