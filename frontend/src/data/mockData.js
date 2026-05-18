const baseOrders = [
  {
    orderNo: "PV20260506001",
    plateNo: "SH-A7686",
    slotId: "E06",
    entryTime: "2026-05-18T08:12:00",
    status: "PARKED",
    amount: 18,
  },
  {
    orderNo: "PV20260506002",
    plateNo: "SH-D5218",
    slotId: "C05",
    entryTime: "2026-05-18T06:32:00",
    status: "RETRIEVING",
    amount: 42.5,
  },
  {
    orderNo: "PV20260506003",
    plateNo: "SU-M9021",
    slotId: "A09",
    entryTime: "2026-05-18T07:08:00",
    status: "TOUCHING",
    amount: 25,
  },
  {
    orderNo: "PV20260506004",
    plateNo: "SH-K1314",
    slotId: "B01",
    entryTime: "2026-05-18T09:10:00",
    status: "FINISHED",
    amount: 16,
  },
];

export const mockSummary = {
  occupancyRate: 68,
  trafficTotal: 414,
  agvOnline: "4/4",
  alertCount: 3,
  revenue: 102,
  avgWait: "04:12",
  chargingTurnover: "7.4 次/日",
};

export const mockForecast = {
  history: [12, 18, 16, 22, 35, 48, 52, 42, 36, 58, 64, 49],
  prediction: [61, 57, 49, 43, 38, 32],
};

export const mockVisionResult = {
  requestId: "edge-423188",
  cameraId: "CAM-SOUTH-01",
  plate: "SH-A7686",
  confidence: 0.982,
  intrusion: false,
  action: "ALLOW_ENTRY_AND_CREATE_ORDER",
};

export function createMockSlots() {
  const statusCycle = ["empty", "occupied", "occupied", "empty", "charging", "buffer", "occupied", "empty"];
  const slots = Array.from({ length: 72 }, (_, index) => ({
    id: `${String.fromCharCode(65 + Math.floor(index / 12))}${String(index % 12 + 1).padStart(2, "0")}`,
    status: index === 64 ? "maintenance" : statusCycle[index % statusCycle.length],
    layer: index < 24 ? "Shallow" : index < 48 ? "Mid" : "Deep",
    available: statusCycle[index % statusCycle.length] === "empty",
    renderColor: "cyan",
  }));

  setSlotStatus(slots, "A09", "buffer");
  setSlotStatus(slots, "B01", "empty");
  setSlotStatus(slots, "C05", "charging");
  setSlotStatus(slots, "E06", "occupied");
  return slots;
}

export function createMockOrders() {
  return baseOrders.map((order) => ({ ...order }));
}

export const mockAlerts = [
  {
    alertNo: "AL2026050601",
    type: "安全",
    content: "交接区检测到人员入侵",
    status: "急停中",
    level: "高",
  },
  {
    alertNo: "AL2026050602",
    type: "设备",
    content: "AGV-04 电量低于 20%",
    status: "处理中",
    level: "中",
  },
  {
    alertNo: "AL2026050603",
    type: "订单",
    content: "二次车牌识别结果不一致",
    status: "待复核",
    level: "中",
  },
];

export const mockPricingRules = [
  {
    name: "工作日阶梯计费",
    timeRange: "07:00-22:00",
    method: "首小时 6 元，之后 4 元/小时",
    extraPolicy: "封顶 48 元",
    status: "启用",
  },
  {
    name: "夜间包时",
    timeRange: "22:00-07:00",
    method: "夜间统一 12 元",
    extraPolicy: "月卡免收",
    status: "启用",
  },
  {
    name: "VIP 优先取车",
    timeRange: "全天",
    method: "基础费 + 8 元",
    extraPolicy: "队列权重 +40",
    status: "启用",
  },
  {
    name: "新能源充电",
    timeRange: "全天",
    method: "1.2 元/千瓦时",
    extraPolicy: "充满自动释放",
    status: "启用",
  },
];

export const mockAccessList = [
  {
    plateNo: "SH-A7686",
    listType: "白名单",
    userType: "月卡用户",
    validUntil: "2026-12-31",
    remark: "自动放行",
  },
  {
    plateNo: "SH-D5218",
    listType: "白名单",
    userType: "新能源车主",
    validUntil: "2026-09-01",
    remark: "充电优先",
  },
  {
    plateNo: "SU-M9021",
    listType: "普通名单",
    userType: "临时访客",
    validUntil: "单次订单",
    remark: "支持无接触支付",
  },
  {
    plateNo: "SH-B9001",
    listType: "黑名单",
    userType: "支付异常",
    validUntil: "人工复核",
    remark: "禁止入场",
  },
];

export const mockSystemNodes = [
  {
    name: "Edge-Cam-01",
    latency: "98ms",
    detail: "南门视觉预处理节点运行正常，正在转发 OCR 元数据",
    level: "stable",
  },
  {
    name: "PLC-Master-Controller",
    latency: "12ms",
    detail: "道闸控制器与 AGV 网关心跳稳定",
    level: "stable",
  },
  {
    name: "Redis-Sync-Cluster",
    latency: "28ms",
    detail: "运营缓存与报表分发数据已同步",
    level: "stable",
  },
];

export const mockAgvs = [
  { id: "AGV-01", x: 10, y: 12, loaded: false, task: "A 区巡检", batteryPct: 91, mode: "IDLE", velocityMps: 0.42, lastCommand: "patrol" },
  { id: "AGV-02", x: 45, y: 32, loaded: true, task: "搬运车辆 SH-A7686", batteryPct: 74, mode: "CARRYING", velocityMps: 0.86, lastCommand: "deliver" },
  { id: "AGV-03", x: 72, y: 58, loaded: false, task: "前往浅层缓冲区", batteryPct: 68, mode: "TRANSIT", velocityMps: 0.65, lastCommand: "relocate" },
  { id: "AGV-04", x: 28, y: 76, loaded: false, task: "充电待命", batteryPct: 19, mode: "CHARGING", velocityMps: 0, lastCommand: "dock" },
];

export const mockQueue = [
  { plateNo: "SH-A7686", type: "标准取车", tag: "先到先取", wait: "04:12", vip: false },
  { plateNo: "SH-D5218", type: "充电车位放行", tag: "充电完成", wait: "03:40", vip: false },
  { plateNo: "SU-M9021", type: "临停取物", tag: "临取", wait: "02:10", vip: false },
  { plateNo: "SH-V7780", type: "预约出场", tag: "预约", wait: "01:58", vip: false },
];

export const mockDeviceOverview = {
  cameras: [
    {
      cameraId: "CAM-SOUTH-01",
      profile: "ONVIF Profile T",
      codec: "H.265",
      streamUrl: "rtsp://10.10.1.21:554/Streaming/Channels/101",
      fps: 25,
      bitrateKbps: 4096,
      status: "ONLINE",
      lastPlate: "SH-A7686",
      lastSeen: "2026-05-18T10:11:52",
      tamperAlarm: false,
      intrusionState: false,
      detail: "南门边缘摄像头，支持车牌 OCR 和交接区入侵检测",
    },
    {
      cameraId: "CAM-HANDOFF-02",
      profile: "ONVIF Profile T",
      codec: "H.264",
      streamUrl: "rtsp://10.10.1.33:554/Streaming/Channels/101",
      fps: 20,
      bitrateKbps: 3072,
      status: "ONLINE",
      lastPlate: "SU-M9021",
      lastSeen: "2026-05-18T10:11:57",
      tamperAlarm: false,
      intrusionState: false,
      detail: "交接区安全摄像头，支持人员入侵告警",
    },
  ],
  gates: [
    {
      gateId: "GATE-IN-01",
      protocol: "Modbus/TCP",
      endpoint: "10.10.20.11:502",
      coilAddress: "00017",
      queueDepth: 2,
      gateState: "OPEN",
      loopOccupied: true,
      estopArmed: false,
      lastDecision: "ACCESS_GRANTED",
      lastSeen: "2026-05-18T10:11:59",
      detail: "入场道闸，带地感线圈和 PLC 继电器控制",
    },
    {
      gateId: "GATE-OUT-01",
      protocol: "Modbus/TCP",
      endpoint: "10.10.20.12:502",
      coilAddress: "00021",
      queueDepth: 1,
      gateState: "READY",
      loopOccupied: false,
      estopArmed: false,
      lastDecision: "READY",
      lastSeen: "2026-05-18T10:11:55",
      detail: "出场交接闸机，与 AGV 放行窗口同步",
    },
  ],
  chargers: [
    {
      chargerId: "EVSE-01",
      protocol: "OCPP 1.6J",
      endpoint: "ws://10.10.30.41:9000/ocpp/EVSE-01",
      connectorStatus: "Charging",
      powerKw: 11,
      sessionKwh: 18.4,
      vehiclePlate: "SH-D5218",
      authStatus: "Accepted",
      lastSeen: "2026-05-18T10:11:50",
      detail: "C05 优先车位交流充电桩",
    },
    {
      chargerId: "EVSE-02",
      protocol: "OCPP 1.6J",
      endpoint: "ws://10.10.30.42:9000/ocpp/EVSE-02",
      connectorStatus: "Available",
      powerKw: 0,
      sessionKwh: 0,
      vehiclePlate: null,
      authStatus: "Idle",
      lastSeen: "2026-05-18T10:11:58",
      detail: "交接区附近交流充电桩，适合短停补能",
    },
  ],
  events: [
    {
      eventId: "DV20260506001",
      deviceType: "camera",
      deviceId: "CAM-SOUTH-01",
      eventCode: "PLATE_READ",
      severity: "info",
      message: "OCR 识别车牌 SH-A7686，并已转发入场服务",
      eventTime: "2026-05-18T10:07:00",
      acknowledged: true,
    },
    {
      eventId: "DV20260506002",
      deviceType: "gate",
      deviceId: "GATE-IN-01",
      eventCode: "LOOP_OCCUPIED",
      severity: "info",
      message: "入场道闸地感线圈检测到车辆",
      eventTime: "2026-05-18T10:08:00",
      acknowledged: true,
    },
    {
      eventId: "DV20260506003",
      deviceType: "charger",
      deviceId: "EVSE-01",
      eventCode: "ENERGY_DELIVERY",
      severity: "info",
      message: "SH-D5218 的充电会话已达到 18.40 千瓦时",
      eventTime: "2026-05-18T10:09:00",
      acknowledged: false,
    },
  ],
};

export function toAdminOrderRow(order) {
  return {
    orderNo: order.orderNo,
    plateNo: order.plateNo,
    event: eventOf(order.status),
    slotId: order.slotId,
    status: statusOf(order.status),
    amount: `￥${Number(order.amount || 0).toFixed(2)}`,
  };
}

export function createMockAdminOrders() {
  return createMockOrders().map(toAdminOrderRow);
}

export function buildMockReport(query = "最近 7 天 VIP 服务趋势") {
  return {
    query,
    summary:
      "当前 3 条实时告警、4 个调度排队任务、49 个占用车位、已确认收入 102 元。VIP 与预调度动作当前贡献 1 个优先任务。",
    labels: ["周一", "周二", "周三", "周四", "周五", "周六", "周日"],
    previousWeekRevenue: [120, 132, 101, 134, 290, 430, 410],
    currentWeekRevenue: [220, 182, 191, 234, 490, 530, 610],
  };
}

export function buildMockPricingPreview(order = baseOrders[0]) {
  return {
    orderNo: order.orderNo,
    plateNo: order.plateNo,
    pricingWindow: "工作日高峰时段",
    durationMinutes: 132,
    baseAmount: 14,
    peakMultiplier: 1.5,
    components: [
      { label: "基础停车费", formula: "3 小时阶梯计费", amount: 14, accent: "base" },
      { label: "高峰调节费", formula: "1.50 倍拥堵调节", amount: 7, accent: "peak" },
      { label: "新能源充电", formula: "1.20 元/千瓦时", amount: order.plateNo.startsWith("SH-D") ? 22.08 : 0, accent: "charging" },
      { label: "VIP 优先取车", formula: "固定调度优先费", amount: 5, accent: "vip" },
    ],
    totalAmount: order.plateNo.startsWith("SH-D") ? 48.08 : 26,
    explanation: "费用预览来自订单、调度优先级和实时充电桩遥测。",
  };
}

export function buildMockIndoorRoute(order = baseOrders[0]) {
  return {
    orderNo: order.orderNo,
    plateNo: order.plateNo,
    slotId: order.slotId,
    handoffZone: "A 区交接点",
    targetGate: "GATE-OUT-01",
    remainingMeters: 120,
    etaSeconds: 180,
    agvEtaSeconds: 96,
    walkingSpeedKph: 6,
    completedSegments: 2,
    nextInstruction: "沿入场车道直行后右转，前往交接走廊",
    status: "AGV-01 正在接近放行走廊",
    safetyMessage: "室内路线安全，已与实时 AGV 和闸机遥测同步。",
  };
}

function setSlotStatus(slots, slotId, status) {
  const slot = slots.find((item) => item.id === slotId);
  if (!slot) return;
  slot.status = status;
  slot.available = status === "empty";
}

function eventOf(status) {
  switch (status) {
    case "PARKED":
      return "车辆入场";
    case "RETRIEVING":
      return "取车请求";
    case "TOUCHING":
      return "临停取物";
    case "PAYING":
      return "待支付";
    case "FINISHED":
      return "完成离场";
    default:
      return "异常复核";
  }
}

function statusOf(status) {
  switch (status) {
    case "PARKED":
      return "停车中";
    case "RETRIEVING":
      return "调度中";
    case "TOUCHING":
      return "交接区等待";
    case "PAYING":
      return "等待支付";
    case "FINISHED":
      return "已关闭";
    default:
      return "需要复核";
  }
}
