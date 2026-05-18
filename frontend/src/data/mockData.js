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
  chargingTurnover: "7.4 / day",
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
    type: "Safety",
    content: "Person intrusion detected in transfer zone",
    status: "Emergency stop",
    level: "High",
  },
  {
    alertNo: "AL2026050602",
    type: "Device",
    content: "AGV-04 battery below 20%",
    status: "Processing",
    level: "Medium",
  },
  {
    alertNo: "AL2026050603",
    type: "Order",
    content: "Secondary plate recognition mismatch",
    status: "Pending review",
    level: "Medium",
  },
];

export const mockPricingRules = [
  {
    name: "Workday hourly",
    timeRange: "07:00-22:00",
    method: "First hour 6, then 4/hour",
    extraPolicy: "Cap 48",
    status: "Active",
  },
  {
    name: "Night package",
    timeRange: "22:00-07:00",
    method: "12 flat rate",
    extraPolicy: "Monthly pass exempt",
    status: "Active",
  },
  {
    name: "VIP retrieval",
    timeRange: "All day",
    method: "Base fee + 8",
    extraPolicy: "Queue weight +40",
    status: "Active",
  },
  {
    name: "EV charging",
    timeRange: "All day",
    method: "1.2/kWh",
    extraPolicy: "Auto release when full",
    status: "Active",
  },
];

export const mockAccessList = [
  {
    plateNo: "SH-A7686",
    listType: "Whitelist",
    userType: "Monthly member",
    validUntil: "2026-12-31",
    remark: "Auto pass",
  },
  {
    plateNo: "SH-D5218",
    listType: "Whitelist",
    userType: "EV owner",
    validUntil: "2026-09-01",
    remark: "Charging priority",
  },
  {
    plateNo: "SU-M9021",
    listType: "Normal",
    userType: "Temporary visitor",
    validUntil: "Single order",
    remark: "Supports contactless pay",
  },
  {
    plateNo: "SH-B9001",
    listType: "Blacklist",
    userType: "Payment exception",
    validUntil: "Manual review",
    remark: "Entry blocked",
  },
];

export const mockSystemNodes = [
  {
    name: "Edge-Cam-01",
    latency: "98ms",
    detail: "South gate vision pre-processing node is healthy and forwarding OCR metadata",
    level: "stable",
  },
  {
    name: "PLC-Master-Controller",
    latency: "12ms",
    detail: "Barrier controller and AGV fleet gateway heartbeats are stable",
    level: "stable",
  },
  {
    name: "Redis-Sync-Cluster",
    latency: "28ms",
    detail: "Operational cache and report fan-out are synchronized",
    level: "stable",
  },
];

export const mockAgvs = [
  { id: "AGV-01", x: 10, y: 12, loaded: false, task: "Patrolling Zone A", batteryPct: 91, mode: "IDLE", velocityMps: 0.42, lastCommand: "patrol" },
  { id: "AGV-02", x: 45, y: 32, loaded: true, task: "Carrying SH-A7686", batteryPct: 74, mode: "CARRYING", velocityMps: 0.86, lastCommand: "deliver" },
  { id: "AGV-03", x: 72, y: 58, loaded: false, task: "Heading to shallow buffer", batteryPct: 68, mode: "TRANSIT", velocityMps: 0.65, lastCommand: "relocate" },
  { id: "AGV-04", x: 28, y: 76, loaded: false, task: "Charging standby", batteryPct: 19, mode: "CHARGING", velocityMps: 0, lastCommand: "dock" },
];

export const mockQueue = [
  { plateNo: "SH-A7686", type: "Standard retrieval", tag: "FIFO", wait: "04:12", vip: false },
  { plateNo: "SH-D5218", type: "Charging bay release", tag: "Charging done", wait: "03:40", vip: false },
  { plateNo: "SU-M9021", type: "Touch-and-Go", tag: "Touch", wait: "02:10", vip: false },
  { plateNo: "SH-V7780", type: "Reserved outbound", tag: "Booking", wait: "01:58", vip: false },
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
      detail: "South gate edge camera with OCR and handoff-zone intrusion ROI",
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
      detail: "Transfer-bay safety camera with person intrusion alarm",
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
      detail: "Inbound barrier with loop detector and PLC relay control",
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
      detail: "Outbound handoff gate synchronized with AGV release window",
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
      detail: "AC charger in premium bay C05",
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
      detail: "AC charger near handoff zone for short dwell sessions",
    },
  ],
  events: [
    {
      eventId: "DV20260506001",
      deviceType: "camera",
      deviceId: "CAM-SOUTH-01",
      eventCode: "PLATE_READ",
      severity: "info",
      message: "OCR recognized SH-A7686 and forwarded metadata to the entry service",
      eventTime: "2026-05-18T10:07:00",
      acknowledged: true,
    },
    {
      eventId: "DV20260506002",
      deviceType: "gate",
      deviceId: "GATE-IN-01",
      eventCode: "LOOP_OCCUPIED",
      severity: "info",
      message: "Induction loop detected a vehicle at the inbound barrier",
      eventTime: "2026-05-18T10:08:00",
      acknowledged: true,
    },
    {
      eventId: "DV20260506003",
      deviceType: "charger",
      deviceId: "EVSE-01",
      eventCode: "ENERGY_DELIVERY",
      severity: "info",
      message: "Charging session for SH-D5218 reached 18.40 kWh",
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
    amount: `CNY ${Number(order.amount || 0).toFixed(2)}`,
  };
}

export function createMockAdminOrders() {
  return createMockOrders().map(toAdminOrderRow);
}

export function buildMockReport(query = "VIP service trend in the last 7 days") {
  return {
    query,
    summary:
      "3 live alerts, 4 queued dispatch tasks, 49 occupied slots, realized revenue 102 CNY. VIP and pre-dispatch actions currently account for 1 priority job.",
    labels: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
    previousWeekRevenue: [120, 132, 101, 134, 290, 430, 410],
    currentWeekRevenue: [220, 182, 191, 234, 490, 530, 610],
  };
}

export function buildMockPricingPreview(order = baseOrders[0]) {
  return {
    orderNo: order.orderNo,
    plateNo: order.plateNo,
    pricingWindow: "Workday peak window",
    durationMinutes: 132,
    baseAmount: 14,
    peakMultiplier: 1.5,
    components: [
      { label: "Base parking", formula: "3h ladder", amount: 14, accent: "base" },
      { label: "Peak shaping", formula: "1.50x congestion multiplier", amount: 7, accent: "peak" },
      { label: "EV charging", formula: "1.20 CNY/kWh", amount: order.plateNo.startsWith("SH-D") ? 22.08 : 0, accent: "charging" },
      { label: "VIP retrieval", formula: "Fixed dispatch priority surcharge", amount: 5, accent: "vip" },
    ],
    totalAmount: order.plateNo.startsWith("SH-D") ? 48.08 : 26,
    explanation: "Preview is generated from the order, dispatch priority, and live charger telemetry.",
  };
}

export function buildMockIndoorRoute(order = baseOrders[0]) {
  return {
    orderNo: order.orderNo,
    plateNo: order.plateNo,
    slotId: order.slotId,
    handoffZone: "Zone A handoff",
    targetGate: "GATE-OUT-01",
    remainingMeters: 120,
    etaSeconds: 180,
    agvEtaSeconds: 96,
    walkingSpeedKph: 6,
    completedSegments: 2,
    nextInstruction: "Keep straight past the inbound lane, then turn right toward the handoff corridor",
    status: "AGV-01 is approaching the release corridor",
    safetyMessage: "Indoor route is clear and synchronized with live AGV and gate telemetry.",
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
      return "Vehicle entry";
    case "RETRIEVING":
      return "Retrieve request";
    case "TOUCHING":
      return "Touch-and-go";
    case "PAYING":
      return "Pending payment";
    case "FINISHED":
      return "Completed exit";
    default:
      return "Exception review";
  }
}

function statusOf(status) {
  switch (status) {
    case "PARKED":
      return "Active parking";
    case "RETRIEVING":
      return "Dispatching";
    case "TOUCHING":
      return "At handoff bay";
    case "PAYING":
      return "Awaiting payment";
    case "FINISHED":
      return "Closed";
    default:
      return "Needs review";
  }
}
