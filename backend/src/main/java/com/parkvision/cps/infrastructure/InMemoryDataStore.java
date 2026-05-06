package com.parkvision.cps.infrastructure;

import com.parkvision.cps.admin.AlertEvent;
import com.parkvision.cps.admin.PricingRule;
import com.parkvision.cps.dispatch.AgvUnit;
import com.parkvision.cps.dispatch.DispatchTask;
import com.parkvision.cps.order.OrderStatus;
import com.parkvision.cps.order.ParkingOrder;
import com.parkvision.cps.parking.ParkingSlot;
import com.parkvision.cps.parking.SlotStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryDataStore {
    private final List<ParkingSlot> slots = new ArrayList<>();
    private final List<ParkingOrder> orders = new ArrayList<>();
    private final List<AlertEvent> alerts = new ArrayList<>();
    private final List<PricingRule> pricingRules = new ArrayList<>();
    private final List<AgvUnit> agvs = new ArrayList<>();
    private final List<DispatchTask> queue = new ArrayList<>();

    public InMemoryDataStore() {
        seedSlots();
        seedOrders();
        seedAdminData();
        seedDispatchData();
    }

    public List<ParkingSlot> slots() {
        return slots;
    }

    public List<ParkingOrder> orders() {
        return orders;
    }

    public List<AlertEvent> alerts() {
        return alerts;
    }

    public List<PricingRule> pricingRules() {
        return pricingRules;
    }

    public List<AgvUnit> agvs() {
        return agvs;
    }

    public List<DispatchTask> queue() {
        return queue;
    }

    public Optional<ParkingSlot> firstEmptySlot() {
        return slots.stream().filter(slot -> slot.getStatus() == SlotStatus.EMPTY).findFirst();
    }

    public Optional<ParkingSlot> firstDeepOccupiedSlot() {
        return slots.stream()
                .filter(slot -> "深层".equals(slot.getLayer()))
                .filter(slot -> slot.getStatus() == SlotStatus.OCCUPIED)
                .findFirst();
    }

    public void addOrder(ParkingOrder order) {
        orders.add(0, order);
    }

    public void addQueueTask(DispatchTask task) {
        queue.add(0, task);
    }

    private void seedSlots() {
        SlotStatus[] statuses = {
                SlotStatus.EMPTY, SlotStatus.OCCUPIED, SlotStatus.OCCUPIED, SlotStatus.EMPTY,
                SlotStatus.CHARGING, SlotStatus.BUFFER, SlotStatus.OCCUPIED, SlotStatus.EMPTY
        };
        for (int index = 0; index < 72; index++) {
            String id = "%s%02d".formatted((char) ('A' + index / 12), index % 12 + 1);
            String layer = index < 24 ? "浅层" : index < 48 ? "中层" : "深层";
            SlotStatus status = index == 64 ? SlotStatus.MAINTENANCE : statuses[index % statuses.length];
            slots.add(new ParkingSlot(id, layer, status));
        }
    }

    private void seedOrders() {
        orders.add(new ParkingOrder("PV20260506001", "沪A·P7686", "B18", LocalDateTime.now().minusHours(2), OrderStatus.PARKED, new BigDecimal("18.00")));
        orders.add(new ParkingOrder("PV20260506002", "沪D·E5218", "C05", LocalDateTime.now().minusHours(4), OrderStatus.RETRIEVING, new BigDecimal("42.50")));
        orders.add(new ParkingOrder("PV20260506003", "苏E·M9021", "A09", LocalDateTime.now().minusHours(3), OrderStatus.TOUCHING, new BigDecimal("25.00")));
        orders.add(new ParkingOrder("PV20260506004", "浙A·K1314", "A01", LocalDateTime.now().minusHours(1), OrderStatus.FINISHED, new BigDecimal("16.00")));
    }

    private void seedAdminData() {
        alerts.add(new AlertEvent("AL2026050601", "安全", "交接区人员闯入", "已急停", "高"));
        alerts.add(new AlertEvent("AL2026050602", "设备", "AGV-04 电量低于 20%", "处理中", "中"));
        alerts.add(new AlertEvent("AL2026050603", "订单", "车牌二次识别不一致", "待复核", "中"));
        pricingRules.add(new PricingRule("工作日阶梯计费", "07:00-22:00", "首小时 ¥6，之后 ¥4/小时", "封顶 ¥48", "启用"));
        pricingRules.add(new PricingRule("夜间包干", "22:00-07:00", "¥12 包干", "月卡减免", "启用"));
        pricingRules.add(new PricingRule("VIP 加急取车", "全天", "基础费用 + ¥8", "队列权重 +40", "启用"));
    }

    private void seedDispatchData() {
        agvs.add(new AgvUnit("AGV-01", 10, 12, false, "巡航至 A 区"));
        agvs.add(new AgvUnit("AGV-02", 45, 32, true, "搬运沪A·P7686"));
        agvs.add(new AgvUnit("AGV-03", 72, 58, false, "前往浅层缓存区"));
        agvs.add(new AgvUnit("AGV-04", 28, 76, false, "充电待命"));
        queue.add(new DispatchTask("沪A·P7686", "普通取车", "FIFO", "04:12", false));
        queue.add(new DispatchTask("沪D·E5218", "新能源腾桩", "充电完成", "03:40", false));
        queue.add(new DispatchTask("苏E·M9021", "临时取物", "Touch", "02:10", false));
        queue.add(new DispatchTask("沪B·V7780", "预约出库", "预约", "01:58", false));
    }
}
