package com.parkvision.cps.repository;

import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.domain.admin.SystemNodeStatus;
import com.parkvision.cps.domain.device.CameraDevice;
import com.parkvision.cps.domain.device.ChargingStation;
import com.parkvision.cps.domain.device.DeviceEvent;
import com.parkvision.cps.domain.device.GateDevice;
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
    private final List<CameraDevice> cameraDevices = new ArrayList<>();
    private final List<GateDevice> gateDevices = new ArrayList<>();
    private final List<ChargingStation> chargingStations = new ArrayList<>();
    private final List<DeviceEvent> deviceEvents = new ArrayList<>();

    public FallbackParkVisionRepository() {
        seedParkingSlots();
        seedOrders();
        seedAdminData();
        seedDispatchData();
        seedDeviceData();
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
    public ParkingSlot saveSlot(ParkingSlot slot) {
        slots.removeIf(existing -> existing.getId().equals(slot.getId()));
        slots.add(slot);
        return slot;
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
    public AlertEvent saveAlert(AlertEvent alert) {
        alerts.removeIf(existing -> existing.alertNo().equals(alert.alertNo()));
        alerts.add(0, alert);
        return alert;
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
    public SystemNodeStatus saveSystemNode(SystemNodeStatus node) {
        systemNodes.removeIf(existing -> existing.name().equals(node.name()));
        systemNodes.add(node);
        return node;
    }

    @Override
    public List<AgvUnit> findAgvUnits() {
        return Collections.unmodifiableList(agvUnits);
    }

    @Override
    public Optional<AgvUnit> findAgvById(String agvId) {
        return agvUnits.stream().filter(agv -> agv.getId().equals(agvId)).findFirst();
    }

    @Override
    public AgvUnit saveAgvUnit(AgvUnit agv) {
        agvUnits.removeIf(existing -> existing.getId().equals(agv.getId()));
        agvUnits.add(agv);
        return agv;
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

    @Override
    public List<CameraDevice> findCameraDevices() {
        return Collections.unmodifiableList(cameraDevices);
    }

    @Override
    public Optional<CameraDevice> findCameraDeviceById(String cameraId) {
        return cameraDevices.stream().filter(camera -> camera.cameraId().equals(cameraId)).findFirst();
    }

    @Override
    public CameraDevice saveCameraDevice(CameraDevice camera) {
        cameraDevices.removeIf(existing -> existing.cameraId().equals(camera.cameraId()));
        cameraDevices.add(camera);
        return camera;
    }

    @Override
    public List<GateDevice> findGateDevices() {
        return Collections.unmodifiableList(gateDevices);
    }

    @Override
    public Optional<GateDevice> findGateDeviceById(String gateId) {
        return gateDevices.stream().filter(gate -> gate.gateId().equals(gateId)).findFirst();
    }

    @Override
    public GateDevice saveGateDevice(GateDevice gate) {
        gateDevices.removeIf(existing -> existing.gateId().equals(gate.gateId()));
        gateDevices.add(gate);
        return gate;
    }

    @Override
    public List<ChargingStation> findChargingStations() {
        return Collections.unmodifiableList(chargingStations);
    }

    @Override
    public Optional<ChargingStation> findChargingStationById(String chargerId) {
        return chargingStations.stream().filter(station -> station.chargerId().equals(chargerId)).findFirst();
    }

    @Override
    public ChargingStation saveChargingStation(ChargingStation station) {
        chargingStations.removeIf(existing -> existing.chargerId().equals(station.chargerId()));
        chargingStations.add(station);
        return station;
    }

    @Override
    public List<DeviceEvent> findDeviceEvents() {
        return Collections.unmodifiableList(deviceEvents);
    }

    @Override
    public DeviceEvent saveDeviceEvent(DeviceEvent event) {
        deviceEvents.removeIf(existing -> existing.eventId().equals(event.eventId()));
        deviceEvents.add(0, event);
        return event;
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
        LocalDateTime now = LocalDateTime.now();
        addSeedOrder("PV20260506001", "SH-A7686", "E06", now.minusHours(2).minusMinutes(5), OrderStatus.PARKED, "18.00");
        addSeedOrder("PV20260506002", "SH-D5218", "C05", now.minusHours(4).minusMinutes(5), OrderStatus.RETRIEVING, "42.50");
        addSeedOrder("PV20260506003", "SU-M9021", "A09", now.minusHours(3).minusMinutes(12), OrderStatus.TOUCHING, "25.00");
        addSeedOrder("PV20260506004", "SH-K1314", "B01", now.minusHours(1).minusMinutes(8), OrderStatus.FINISHED, "16.00");
        addSeedOrder("PV20260506005", "SH-P3308", "D04", now.minusHours(5).minusMinutes(18), OrderStatus.PARKED, "26.00");
        addSeedOrder("PV20260506006", "SH-R1188", "F08", now.minusHours(8).minusMinutes(30), OrderStatus.FINISHED, "38.00");
        addSeedOrder("PV20260506007", "SH-L5521", "C02", now.minusHours(2).minusMinutes(48), OrderStatus.PARKED, "14.00");
        addSeedOrder("PV20260506008", "SH-T6502", "D11", now.minusHours(3).minusMinutes(40), OrderStatus.PAYING, "28.00");
        addSeedOrder("PV20260506009", "SH-N4820", "B07", now.minusHours(10).minusMinutes(5), OrderStatus.FINISHED, "44.00");
        addSeedOrder("PV20260506010", "SH-C8871", "A04", now.minusHours(6).minusMinutes(15), OrderStatus.ABNORMAL, "12.00");
        addSeedOrder("PV20260506011", "SH-G6205", "F03", now.minusHours(1).minusMinutes(55), OrderStatus.PARKED, "12.00");
        addSeedOrder("PV20260506012", "SH-D9082", "E02", now.minusHours(7).minusMinutes(20), OrderStatus.PARKED, "51.80");
        addSeedOrder("PV20260506013", "SU-Q1137", "C09", now.minusHours(12).minusMinutes(2), OrderStatus.FINISHED, "52.00");
        addSeedOrder("PV20260506014", "SH-M4401", "D06", now.minusHours(2).minusMinutes(36), OrderStatus.RETRIEVING, "21.00");
        addSeedOrder("PV20260506015", "SH-V7780", "E11", now.minusMinutes(88), OrderStatus.PARKED, "8.00");
        addSeedOrder("PV20260506016", "SH-X2204", "B10", now.minusHours(9).minusMinutes(44), OrderStatus.FINISHED, "40.00");
        addSeedOrder("PV20260506017", "SH-H3819", "F01", now.minusHours(1).minusMinutes(38), OrderStatus.PARKED, "10.00");
        addSeedOrder("PV20260506018", "SH-Y6608", "C12", now.minusHours(3).minusMinutes(2), OrderStatus.TOUCHING, "23.00");
        addSeedOrder("PV20260506019", "SH-D3015", "E04", now.minusHours(5).minusMinutes(25), OrderStatus.PARKED, "47.60");
        addSeedOrder("PV20260506020", "SU-A7005", "A12", now.minusHours(11).minusMinutes(18), OrderStatus.FINISHED, "36.00");
        addSeedOrder("PV20260506021", "SH-U1140", "D08", now.minusHours(2).minusMinutes(58), OrderStatus.PAYING, "19.00");
        addSeedOrder("PV20260506022", "SH-W3099", "F10", now.minusMinutes(96), OrderStatus.PARKED, "9.00");
        addSeedOrder("PV20260506023", "SH-J5520", "B03", now.minusHours(6).minusMinutes(52), OrderStatus.FINISHED, "31.00");
        addSeedOrder("PV20260506024", "SH-Z9907", "C07", now.minusHours(4).minusMinutes(8), OrderStatus.PARKED, "22.00");
    }

    private void seedAdminData() {
        alerts.add(new AlertEvent("AL20260518001", "安全", "交接区检测到人员入侵", "急停中", "高"));
        alerts.add(new AlertEvent("AL20260518002", "设备", "AGV-04 电量低于 20%", "处理中", "中"));
        alerts.add(new AlertEvent("AL20260518003", "订单", "二次车牌识别结果不一致", "待复核", "中"));
        alerts.add(new AlertEvent("AL20260518004", "闸机", "入场道闸排队深度超过调度阈值", "已开启", "中"));
        alerts.add(new AlertEvent("AL20260518005", "充电", "EVSE-03 连接器唤醒超时后已重试握手", "已恢复", "低"));
        alerts.add(new AlertEvent("AL20260518006", "视觉", "北侧摄像头码率低于预期边缘配置", "监控中", "低"));
        alerts.add(new AlertEvent("AL20260518007", "计费", "一笔已完成订单等待结算回调确认", "处理中", "中"));
        alerts.add(new AlertEvent("AL20260518008", "调度", "A04 异常车位释放需要人工复核", "已升级", "高"));

        pricingRules.add(new PricingRule("工作日阶梯计费", "07:00-22:00", "首小时 6 元，之后 4 元/小时", "封顶 48 元", "启用"));
        pricingRules.add(new PricingRule("夜间包时", "22:00-07:00", "夜间统一 12 元", "月卡免收", "启用"));
        pricingRules.add(new PricingRule("VIP 优先取车", "全天", "基础费 + 8 元", "队列权重 +40", "启用"));
        pricingRules.add(new PricingRule("新能源充电", "全天", "1.2 元/千瓦时", "充满自动释放", "启用"));

        accessList.add(new AccessListItem("SH-A7686", "白名单", "月卡用户", "2026-12-31", "自动放行"));
        accessList.add(new AccessListItem("SH-D5218", "白名单", "新能源车主", "2026-09-01", "充电优先"));
        accessList.add(new AccessListItem("SU-M9021", "普通名单", "临时访客", "单次订单", "支持无接触支付"));
        accessList.add(new AccessListItem("SH-B9001", "黑名单", "支付异常", "人工复核", "禁止入场"));
        accessList.add(new AccessListItem("SH-P3308", "白名单", "企业账户", "2026-11-15", "高峰入场优先"));
        accessList.add(new AccessListItem("SH-D9082", "白名单", "新能源月卡", "2026-10-20", "充电费用优惠"));
        accessList.add(new AccessListItem("SH-V7780", "白名单", "预约取车用户", "2026-08-31", "支持提前预约出场"));
        accessList.add(new AccessListItem("SH-T6502", "普通名单", "散客访客", "单次订单", "已开通自助缴费"));
        accessList.add(new AccessListItem("SH-C8871", "观察名单", "异常复核", "人工复核", "路线放行需人工确认"));
        accessList.add(new AccessListItem("SH-H3819", "白名单", "夜间套餐用户", "2026-12-01", "自动应用夜间封顶"));
        accessList.add(new AccessListItem("SU-A7005", "普通名单", "临时访客", "单次订单", "已申请发票"));
        accessList.add(new AccessListItem("SH-X2204", "黑名单", "多次超时离场", "人工复核", "出场需确认"));

        systemNodes.add(new SystemNodeStatus("Edge-Cam-01", "98ms", "南门视觉预处理节点运行正常，正在转发 OCR 元数据", "stable"));
        systemNodes.add(new SystemNodeStatus("PLC-Master-Controller", "12ms", "道闸控制器与 AGV 网关心跳稳定", "stable"));
        systemNodes.add(new SystemNodeStatus("Redis-Sync-Cluster", "31ms", "运营缓存与报表分发数据已同步", "stable"));
        systemNodes.add(new SystemNodeStatus("Billing-Service", "44ms", "发票与结算投影任务按计划完成", "stable"));
        systemNodes.add(new SystemNodeStatus("Device-Simulator", "21ms", "摄像头、闸机、充电桩与 AGV 遥测模拟循环正常", "stable"));
    }

    private void seedDispatchData() {
        agvUnits.add(new AgvUnit("AGV-01", 10, 12, false, "A 区巡检", 91, "IDLE", 0.42, "patrol"));
        agvUnits.add(new AgvUnit("AGV-02", 45, 32, true, "搬运车辆 SH-A7686", 74, "CARRYING", 0.86, "deliver"));
        agvUnits.add(new AgvUnit("AGV-03", 72, 58, false, "前往浅层缓冲区", 68, "TRANSIT", 0.65, "relocate"));
        agvUnits.add(new AgvUnit("AGV-04", 28, 76, false, "充电待命", 19, "CHARGING", 0.00, "dock"));

        dispatchQueue.add(new DispatchTask("SH-A7686", "标准取车", "先到先取", "04:12", false));
        dispatchQueue.add(new DispatchTask("SH-D5218", "充电车位放行", "充电完成", "03:40", false));
        dispatchQueue.add(new DispatchTask("SU-M9021", "临停取物", "临取", "02:10", false));
        dispatchQueue.add(new DispatchTask("SH-V7780", "预约出场", "预约", "01:58", false));
        dispatchQueue.add(new DispatchTask("SH-P3308", "高峰预调度移位", "预调度", "00:48", true));
        dispatchQueue.add(new DispatchTask("SH-M4401", "VIP 优先取车", "VIP", "00:30", true));
        dispatchQueue.add(new DispatchTask("SH-D9082", "充电枪释放", "新能源", "02:42", false));
        dispatchQueue.add(new DispatchTask("SH-T6502", "支付确认等待", "支付", "01:24", false));
        dispatchQueue.add(new DispatchTask("SH-C8871", "人工复核转运", "风控", "05:36", false));
        dispatchQueue.add(new DispatchTask("SH-H3819", "夜间套餐出场", "夜间", "03:06", false));
    }

    private void seedDeviceData() {
        LocalDateTime now = LocalDateTime.now();
        cameraDevices.add(new CameraDevice(
                "CAM-SOUTH-01",
                "ONVIF Profile T",
                "H.265",
                "rtsp://10.10.1.21:554/Streaming/Channels/101",
                25,
                4096,
                "ONLINE",
                "SH-A7686",
                now.minusSeconds(12),
                false,
                false,
                "南门边缘摄像头，支持车牌 OCR 和交接区入侵检测"
        ));
        cameraDevices.add(new CameraDevice(
                "CAM-HANDOFF-02",
                "ONVIF Profile T",
                "H.264",
                "rtsp://10.10.1.33:554/Streaming/Channels/101",
                20,
                3072,
                "ONLINE",
                "SU-M9021",
                now.minusSeconds(7),
                false,
                false,
                "交接区安全摄像头，支持人员入侵告警"
        ));
        cameraDevices.add(new CameraDevice(
                "CAM-NORTH-03",
                "ONVIF Profile T",
                "H.265",
                "rtsp://10.10.1.46:554/Streaming/Channels/101",
                25,
                3584,
                "ONLINE",
                "SH-P3308",
                now.minusSeconds(15),
                false,
                false,
                "北侧坡道摄像头，用于出场队列观察和二次车牌识别"
        ));

        gateDevices.add(new GateDevice(
                "GATE-IN-01",
                "Modbus/TCP",
                "10.10.20.11:502",
                "00017",
                2,
                "OPEN",
                true,
                false,
                "ACCESS_GRANTED",
                now.minusSeconds(5),
                "入场道闸，带地感线圈和 PLC 继电器控制"
        ));
        gateDevices.add(new GateDevice(
                "GATE-OUT-01",
                "Modbus/TCP",
                "10.10.20.12:502",
                "00021",
                1,
                "CLOSED",
                false,
                false,
                "READY",
                now.minusSeconds(8),
                "出场交接闸机，与 AGV 放行窗口同步"
        ));
        gateDevices.add(new GateDevice(
                "GATE-SERVICE-01",
                "Modbus/TCP",
                "10.10.20.19:502",
                "00025",
                0,
                "CLOSED",
                false,
                false,
                "MAINTENANCE_READY",
                now.minusSeconds(11),
                "服务通道闸机，用于人工复核和维护绕行"
        ));

        chargingStations.add(new ChargingStation(
                "EVSE-01",
                "OCPP 1.6J",
                "ws://10.10.30.41:9000/ocpp/EVSE-01",
                "Charging",
                new BigDecimal("11.00"),
                new BigDecimal("18.40"),
                "SH-D5218",
                "Accepted",
                now.minusSeconds(10),
                "C05 优先车位交流充电桩"
        ));
        chargingStations.add(new ChargingStation(
                "EVSE-02",
                "OCPP 1.6J",
                "ws://10.10.30.42:9000/ocpp/EVSE-02",
                "Available",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                "Idle",
                now.minusSeconds(4),
                "交接区附近交流充电桩，适合短停补能"
        ));
        chargingStations.add(new ChargingStation(
                "EVSE-03",
                "OCPP 1.6J",
                "ws://10.10.30.43:9000/ocpp/EVSE-03",
                "Preparing",
                new BigDecimal("2.40"),
                new BigDecimal("0.60"),
                "SH-D3015",
                "Authorizing",
                now.minusSeconds(9),
                "快周转充电桩，等待连接器锁止确认"
        ));

        String[][] eventSpecs = {
                {"camera", "CAM-SOUTH-01", "PLATE_READ", "info", "OCR 识别车牌 SH-A7686，并已转发入场服务"},
                {"gate", "GATE-IN-01", "LOOP_OCCUPIED", "info", "入场道闸地感线圈检测到车辆"},
                {"charger", "EVSE-01", "ENERGY_DELIVERY", "info", "SH-D5218 的充电会话已达到 18.40 千瓦时"},
                {"agv", "AGV-02", "TASK_ASSIGNED", "info", "AGV-02 已接收 SH-A7686 的载车取车任务"},
                {"camera", "CAM-HANDOFF-02", "ROI_CLEAR", "info", "行人离开后，交接区安全 ROI 已清空"},
                {"gate", "GATE-OUT-01", "RELEASE_READY", "info", "出场闸机已与下一个交接车位同步"},
                {"agv", "AGV-04", "BATTERY_LOW", "medium", "AGV-04 电量低于阈值，已进入充电待命"},
                {"charger", "EVSE-03", "AUTHORIZE_START", "info", "EVSE-03 已为 SH-D3015 启动授权握手"},
                {"camera", "CAM-NORTH-03", "SECONDARY_READ", "info", "北侧坡道摄像头确认出场车牌 SH-P3308"},
                {"gate", "GATE-IN-01", "QUEUE_BUILDUP", "medium", "入场排队深度超过预调度规划阈值"},
                {"dispatch", "VIP-QUEUE", "VIP_INSERT", "medium", "SH-M4401 的 VIP 取车任务已插入队首"},
                {"order", "PV20260506010", "MANUAL_REVIEW", "high", "订单 PV20260506010 正等待人工放行审批"},
                {"charger", "EVSE-01", "SESSION_UPDATE", "info", "当前充电会话电量增加 0.8 千瓦时"},
                {"agv", "AGV-03", "RELOCATE_START", "info", "AGV-03 已从深层车道启动预调度移位"},
                {"camera", "CAM-HANDOFF-02", "PERSON_DETECTED", "low", "受保护 ROI 外记录到一次短时行人识别"},
                {"gate", "GATE-SERVICE-01", "SERVICE_READY", "info", "服务通道闸机空闲，可用于异常路线"},
                {"dispatch", "PRE-DISPATCH", "TASK_QUEUED", "info", "预测驱动的移位任务已为 SH-P3308 入队"},
                {"billing", "INVOICE-SVC", "SETTLEMENT_PENDING", "medium", "一笔已完成订单等待结算回调确认"},
                {"charger", "EVSE-02", "HEARTBEAT", "info", "空闲充电桩 EVSE-02 已发送正常心跳帧"},
                {"camera", "CAM-SOUTH-01", "BITRATE_DIP", "low", "码率短暂低于首选边缘配置，随后恢复"},
                {"agv", "AGV-01", "PATROL_TICK", "info", "AGV-01 已完成 A 区周界巡检"},
                {"gate", "GATE-OUT-01", "EXIT_CONFIRMED", "info", "出场闸机已确认交接放行完成"},
                {"charger", "EVSE-03", "LOCK_RETRY", "medium", "EVSE-03 连接器锁止重试一次后成功"},
                {"dispatch", "RISK-QUEUE", "HOLD_ACTIVE", "high", "人工复核转运在工作人员确认前保持拦截"}
        };
        for (int index = 0; index < eventSpecs.length; index++) {
            String[] spec = eventSpecs[index];
            deviceEvents.add(new DeviceEvent(
                    "DV20260518%03d".formatted(index + 1),
                    spec[0],
                    spec[1],
                    spec[2],
                    spec[3],
                    spec[4],
                    now.minusMinutes(eventSpecs.length - index),
                    index < 18
            ));
        }
    }

    private void updateSlot(String slotId, SlotStatus status) {
        findSlotById(slotId).ifPresent(slot -> slot.setStatus(status));
    }

    private void addSeedOrder(String orderNo, String plateNo, String slotId, LocalDateTime entryTime, OrderStatus status, String amount) {
        orders.add(new ParkingOrder(orderNo, plateNo, slotId, entryTime, status, new BigDecimal(amount)));
        updateSlotForOrder(slotId, plateNo, status);
    }

    private void updateSlotForOrder(String slotId, String plateNo, OrderStatus status) {
        findSlotById(slotId).ifPresent(slot -> {
            switch (status) {
                case PARKED -> slot.setStatus(plateNo.startsWith("SH-D") ? SlotStatus.CHARGING : SlotStatus.OCCUPIED);
                case RETRIEVING, TOUCHING, PAYING -> slot.setStatus(SlotStatus.BUFFER);
                case FINISHED -> slot.setStatus(SlotStatus.EMPTY);
                case ABNORMAL -> slot.setStatus(SlotStatus.OCCUPIED);
            }
        });
    }
}
