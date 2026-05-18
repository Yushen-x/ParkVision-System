import { computed, reactive } from "vue";
import { parkvisionApi } from "../api/parkvisionApi";
import {
  buildMockIndoorRoute,
  buildMockPricingPreview,
  buildMockReport,
  createMockAdminOrders,
  createMockOrders,
  createMockSlots,
  mockAccessList,
  mockAgvs,
  mockAlerts,
  mockDeviceOverview,
  mockForecast,
  mockPricingRules,
  mockQueue,
  mockSummary,
  mockSystemNodes,
  mockVisionResult,
  toAdminOrderRow,
} from "../data/mockData";

const fallbackPlates = ["SH-A7686", "SH-D5218", "SU-M9021", "SH-K1314", "SH-V7780"];

export const state = reactive({
  onlineMode: "Checking backend",
  emergency: false,
  loading: false,
  activePlate: mockVisionResult.plate,
  summary: { ...mockSummary },
  forecast: structuredClone(mockForecast),
  events: [
    ["系统上线", "运营首页和本地兜底数据源已初始化。"],
    ["视觉边缘节点", "最新车牌 OCR 结果为 SH-A7686，置信度 0.982。"],
    ["调度中心", "AGV-03 正在前往浅层缓冲车道。"],
  ],
  slots: createMockSlots(),
  agvs: structuredClone(mockAgvs),
  orders: createMockOrders(),
  adminOrders: createMockAdminOrders(),
  alerts: structuredClone(mockAlerts),
  pricingRules: structuredClone(mockPricingRules),
  accessList: structuredClone(mockAccessList),
  systemNodes: structuredClone(mockSystemNodes),
  queue: structuredClone(mockQueue),
  devices: structuredClone(mockDeviceOverview),
  pricingPreview: buildMockPricingPreview(),
  indoorRoute: buildMockIndoorRoute(),
  ownerTimeline: [
    ["车辆已入库", "AGV 已将车辆放入 E06 车位。"],
    ["计费已启动", "入场流程结束后，动态停车费开始计算。"],
    ["车主服务就绪", "取车、临停取物和 VIP 优先取车均可使用。"],
  ],
  visionResult: { ...mockVisionResult },
  adminReport: buildMockReport(),
  busy: {
    entry: false,
    preDispatch: false,
    vision: false,
    ownerAction: false,
    report: false,
  },
});

export const getters = {
  freeCount: computed(() => state.slots.filter((slot) => slot.status === "empty").length),
  occupiedCount: computed(() => state.slots.filter((slot) => slot.status !== "empty").length),
  currentOrder: computed(
    () => state.orders.find((order) => order.status !== "FINISHED") || state.orders[0] || null,
  ),
};

export async function hydrate() {
  state.loading = true;
  state.onlineMode = "Connecting";

  try {
    await parkvisionApi.probeBackend();
    state.onlineMode = "Backend connected";
  } catch {
    state.onlineMode = "Fallback mode";
  }

  const [forecast, operational, admin] = await Promise.all([
    parkvisionApi.getForecast(),
    fetchOperationalData(),
    fetchAdminData(),
  ]);

  state.forecast = forecast;
  applyOperationalData(operational);
  applyAdminData(admin);
  state.loading = false;
}

export async function refreshCore() {
  const operational = await fetchOperationalData();
  applyOperationalData(operational);
}

export async function refreshAdminData() {
  const admin = await fetchAdminData();
  applyAdminData(admin);
}

export async function pollRealtime() {
  try {
    await refreshCore();
  } catch {
    state.onlineMode = "Fallback mode";
  }
}

export function addEvent(title, detail) {
  state.events.unshift([title, detail]);
  state.events = state.events.slice(0, 12);
}

export async function simulateEntry() {
  state.busy.entry = true;
  try {
    const order = await parkvisionApi.simulateEntry();
    addEvent("车辆入场", `${order.plateNo} 已分配到车位 ${order.slotId}。`);
    state.activePlate = order.plateNo;
    await refreshCore();
    await refreshAdminData();
  } catch {
    fallbackSimulateEntry();
  } finally {
    state.busy.entry = false;
  }
}

export async function triggerPreDispatch() {
  state.busy.preDispatch = true;
  try {
    const task = await parkvisionApi.triggerPreDispatch();
    addEvent("预调度入队", `${task.plateNo} 已进入预调度队列。`);
    await refreshCore();
  } catch {
    fallbackPreDispatch();
  } finally {
    state.busy.preDispatch = false;
  }
}

export async function enqueueVip(orderNo = getters.currentOrder.value?.orderNo) {
  state.busy.ownerAction = true;
  try {
    const task = await parkvisionApi.triggerVip(orderNo);
    addEvent("VIP 优先取车", `${task.plateNo} 已插入队首。`);
    pushOwnerTimeline("VIP 优先取车", "当前订单已创建优先 AGV 调度任务。");
    await refreshCore();
    await refreshAdminData();
  } catch {
    fallbackVip(orderNo);
  } finally {
    state.busy.ownerAction = false;
  }
}

export async function runVision(options = {}) {
  state.busy.vision = true;
  try {
    const result = await parkvisionApi.inferVision(options);
    state.visionResult = result;
    state.activePlate = result.plate;
    addEvent(
      "视觉推理完成",
      result.intrusion
        ? "交接区检测到入侵，需要复核。"
        : `${result.plate} 已通过 OCR，置信度 ${result.confidence}。`,
    );
    await refreshCore();
    return state.visionResult;
  } catch {
    return fallbackVision(options);
  } finally {
    state.busy.vision = false;
  }
}

export async function runOwnerAction(action, orderNo = getters.currentOrder.value?.orderNo) {
  if (!orderNo) return;
  state.busy.ownerAction = true;

  try {
    if (action === "retrieve") {
      await parkvisionApi.retrieveOrder(orderNo);
      pushOwnerTimeline("取车已启动", "AGV 取车任务已加入实时调度队列。");
      addEvent("车主请求", `订单 ${orderNo} 已提交取车请求。`);
    } else if (action === "touch") {
      await parkvisionApi.touchOrder(orderNo);
      pushOwnerTimeline("临停取物", "车辆已被调度到交接区，计费会话保持开启。");
      addEvent("车主请求", `订单 ${orderNo} 已提交临停取物请求。`);
    } else if (action === "pay") {
      await parkvisionApi.payOrder(orderNo);
      pushOwnerTimeline("支付完成", "订单已关闭，车位已释放。");
      addEvent("车主请求", `订单 ${orderNo} 已完成支付。`);
    }

    await refreshCore();
    await refreshAdminData();
  } catch {
    fallbackOwnerAction(action, orderNo);
  } finally {
    state.busy.ownerAction = false;
  }
}

export async function generateAdminReport(query) {
  state.busy.report = true;
  try {
    state.adminReport = await parkvisionApi.getReport(query);
    return state.adminReport;
  } finally {
    state.busy.report = false;
  }
}

export async function toggleEmergency() {
  const nextState = !state.emergency;
  try {
    await parkvisionApi.setEmergency(nextState);
    addEvent(
      nextState ? "紧急停车" : "急停解除",
      nextState
        ? "后端安全层已锁定调度放行。"
        : "安全锁已解除，现场设备恢复自动模式。",
    );
    await refreshCore();
  } catch {
    fallbackToggleEmergency(nextState);
  }
}

async function fetchOperationalData() {
  const [summary, slots, orders, queue, agvs, systemNodes, devices, pricingPreview, indoorRoute] = await Promise.all([
    parkvisionApi.getSummary(),
    parkvisionApi.getSlots(),
    parkvisionApi.getOrders(),
    parkvisionApi.getQueue(),
    parkvisionApi.getAgvs(),
    parkvisionApi.getSystemNodes(),
    parkvisionApi.getDevicesOverview(),
    parkvisionApi.getPricingPreview(),
    parkvisionApi.getIndoorRoute(),
  ]);

  return { summary, slots, orders, queue, agvs, systemNodes, devices, pricingPreview, indoorRoute };
}

async function fetchAdminData() {
  const [adminOrders, alerts, pricingRules, accessList] = await Promise.all([
    parkvisionApi.getAdminOrders(),
    parkvisionApi.getAlerts(),
    parkvisionApi.getPricingRules(),
    parkvisionApi.getAccessList(),
  ]);

  return { adminOrders, alerts, pricingRules, accessList };
}

function applyOperationalData(data) {
  state.summary = data.summary;
  state.slots = data.slots;
  state.orders = data.orders;
  state.queue = data.queue;
  state.agvs = normalizeAgvs(data.agvs);
  state.systemNodes = data.systemNodes;
  state.devices = normalizeDevices(data.devices);
  state.pricingPreview = data.pricingPreview;
  state.indoorRoute = data.indoorRoute;
  syncVisionFromDevices();
  state.activePlate = getters.currentOrder.value?.plateNo || state.visionResult.plate || state.activePlate;
  state.emergency = deriveEmergencyState();
}

function applyAdminData(data) {
  state.adminOrders = data.adminOrders;
  state.alerts = data.alerts;
  state.pricingRules = data.pricingRules;
  state.accessList = data.accessList;
}

function syncVisionFromDevices() {
  const activeCamera =
    state.devices.cameras.find((camera) => camera.intrusionState) ||
    state.devices.cameras.find((camera) => camera.cameraId === state.visionResult.cameraId) ||
    state.devices.cameras[0];

  if (!activeCamera) return;

  state.visionResult = {
    ...state.visionResult,
    cameraId: activeCamera.cameraId,
    plate: activeCamera.lastPlate || state.visionResult.plate,
    intrusion: activeCamera.intrusionState,
    action: activeCamera.intrusionState ? "ESTOP_AND_REVIEW" : state.visionResult.action,
  };
}

function deriveEmergencyState() {
  return (
    state.devices.cameras.some((camera) => camera.intrusionState) ||
    state.devices.gates.some((gate) => gate.estopArmed) ||
    state.visionResult.intrusion
  );
}

function fallbackSimulateEntry() {
  const slot = state.slots.find((item) => item.status === "empty");
  if (!slot) return;

  const plateNo = fallbackPlates[Math.floor(Math.random() * fallbackPlates.length)];
  slot.status = plateNo.startsWith("SH-D") ? "charging" : "occupied";
  slot.available = false;

  const order = {
    orderNo: `PV${Date.now().toString().slice(-9)}`,
    plateNo,
    slotId: slot.id,
    entryTime: new Date().toISOString(),
    status: "PARKED",
    amount: 0,
  };

  state.orders.unshift(order);
  state.adminOrders.unshift(toAdminOrderRow(order));
  state.activePlate = plateNo;
  recomputeSummary();
  syncFallbackExperience(order);
  addEvent("车辆入场", `${plateNo} 已在本地兜底模式下分配到车位 ${slot.id}。`);
}

function fallbackPreDispatch() {
  const deepSlot = state.slots.find((item) => item.layer === "Deep" && item.status === "occupied");
  const activeOrder = state.orders.find((item) => item.status !== "FINISHED");
  if (deepSlot) {
    deepSlot.status = "buffer";
  }
  if (activeOrder) {
    state.queue.unshift({
      plateNo: activeOrder.plateNo,
      type: "高峰预调度移位",
      tag: "预调度",
      wait: "00:48",
      vip: true,
    });
    state.agvs[0] = {
      ...state.agvs[0],
      loaded: true,
      task: `移动车辆 ${activeOrder.plateNo}`,
      mode: "TRANSIT",
      velocityMps: 0.78,
      lastCommand: "relocate",
    };
  }
  recomputeSummary();
  syncFallbackExperience(activeOrder);
  addEvent("预调度入队", "本地兜底预调度已将深层车位车辆移入缓冲车道。");
}

function fallbackVip(orderNo) {
  const order = state.orders.find((item) => item.orderNo === orderNo) || getters.currentOrder.value;
  if (!order) return;
  order.status = "RETRIEVING";
  syncOrderStatus(order);
  state.queue.unshift({
    plateNo: order.plateNo,
    type: "VIP 优先取车",
    tag: "VIP",
    wait: "00:30",
    vip: true,
  });
  state.agvs[0] = {
    ...state.agvs[0],
    loaded: true,
    task: `VIP 优先取车 ${order.plateNo}`,
    mode: "CARRYING",
    velocityMps: 0.92,
    lastCommand: "vip-priority",
  };
  recomputeSummary();
  syncFallbackExperience(order);
  pushOwnerTimeline("VIP 优先取车", "本地兜底调度已将订单插入队首。");
  addEvent("VIP 优先取车", `${order.plateNo} 已插入本地兜底队列队首。`);
}

function fallbackVision(options = {}) {
  const plate = fallbackPlates[Math.floor(Math.random() * fallbackPlates.length)];
  const intrusion = Boolean(options.simulateIntrusion);
  state.visionResult = {
    requestId: `edge-${Date.now().toString().slice(-6)}`,
    cameraId: options.cameraId || (intrusion ? "CAM-HANDOFF-02" : "CAM-SOUTH-01"),
    plate,
    confidence: Number((0.94 + Math.random() * 0.05).toFixed(3)),
    intrusion,
    action: intrusion ? "ESTOP_AND_REVIEW" : "ALLOW_ENTRY_AND_CREATE_ORDER",
  };
  state.activePlate = plate;
  state.emergency = intrusion;
  syncFallbackExperience(getters.currentOrder.value);
  addEvent(
    "视觉推理完成",
    intrusion ? "本地兜底安全规则已触发急停复核。" : `${plate} 已通过本地 OCR 推理。`,
  );
  return state.visionResult;
}

function fallbackOwnerAction(action, orderNo) {
  const order = state.orders.find((item) => item.orderNo === orderNo);
  if (!order) return;

  if (action === "retrieve") {
    order.status = "RETRIEVING";
    pushOwnerTimeline("取车已启动", "本地兜底调度已为当前订单创建取车任务。");
  } else if (action === "touch") {
    order.status = "TOUCHING";
    pushOwnerTimeline("临停取物", "本地兜底调度已将车辆送往交接区。");
  } else if (action === "pay") {
    order.status = "FINISHED";
    order.amount = calculateFallbackAmount(order);
    pushOwnerTimeline("支付完成", "本地兜底计费已关闭订单并释放车位。");
  }

  syncOrderStatus(order);
  state.adminOrders = state.orders.map(toAdminOrderRow);
  recomputeSummary();
  syncFallbackExperience(order);
  addEvent("车主请求", `订单 ${order.orderNo} 已在本地处理 ${action} 操作。`);
}

function fallbackToggleEmergency(nextState) {
  state.emergency = nextState;
  state.visionResult = {
    ...state.visionResult,
    intrusion: nextState,
    action: nextState ? "ESTOP_AND_REVIEW" : "ALLOW_ENTRY_AND_CREATE_ORDER",
  };
  syncFallbackExperience(getters.currentOrder.value);
  addEvent(
    nextState ? "紧急停车" : "急停解除",
    nextState
      ? "模拟安全事件触发后，调度画面已冻结。"
      : "安全锁已解除，AGV 运动恢复。",
  );
}

function syncOrderStatus(order) {
  const slot = state.slots.find((item) => item.id === order.slotId);
  if (!slot) return;

  if (order.status === "RETRIEVING" || order.status === "TOUCHING") {
    slot.status = "buffer";
    slot.available = false;
  } else if (order.status === "FINISHED") {
    slot.status = "empty";
    slot.available = true;
  } else if (order.status === "PARKED") {
    slot.status = order.plateNo.startsWith("SH-D") ? "charging" : "occupied";
    slot.available = false;
  }
}

function pushOwnerTimeline(title, detail) {
  state.ownerTimeline.unshift([title, detail]);
  state.ownerTimeline = state.ownerTimeline.slice(0, 8);
}

function recomputeSummary() {
  const occupied = state.slots.filter((slot) => slot.status !== "empty").length;
  const revenue = state.orders.reduce((sum, order) => sum + Number(order.amount || 0), 0);
  state.summary = {
    occupancyRate: Math.round((occupied / state.slots.length) * 100),
    trafficTotal: 410 + state.orders.length,
    agvOnline: `${state.agvs.length}/${state.agvs.length}`,
    alertCount: state.alerts.length,
    revenue: Math.round(revenue),
    avgWait: state.queue[0]?.wait || "03:30",
    chargingTurnover: "7.4 次/日",
  };
}

function calculateFallbackAmount(order) {
  const entryTime = new Date(order.entryTime).getTime();
  const hours = Math.max(1, Math.ceil((Date.now() - entryTime) / 3_600_000));
  let amount = 6 + Math.max(0, hours - 1) * 4;
  if (order.plateNo.startsWith("SH-D")) {
    amount += 12.5;
  }
  return Number(amount.toFixed(2));
}

function syncFallbackExperience(order = getters.currentOrder.value) {
  const activeOrder = order || state.orders[0];
  state.pricingPreview = buildMockPricingPreview(activeOrder);
  state.indoorRoute = buildMockIndoorRoute(activeOrder);
  state.systemNodes = structuredClone(mockSystemNodes).map((node) =>
    state.emergency && node.name !== "Edge-Cam-01"
      ? { ...node, latency: "锁定", level: "warning", detail: "本地兜底安全锁已在控制平面生效" }
      : state.emergency && node.name === "Edge-Cam-01"
        ? { ...node, latency: "告警", level: "warning", detail: "本地兜底安全区告警已升级并阻止调度放行" }
        : node,
  );

  const overview = structuredClone(mockDeviceOverview);
  overview.cameras = overview.cameras.map((camera, index) => ({
    ...camera,
    lastPlate: state.activePlate,
    intrusionState: state.emergency && index === 1,
  }));
  overview.gates = overview.gates.map((gate) => ({
    ...gate,
    estopArmed: state.emergency,
    gateState: state.emergency ? "LOCKDOWN" : gate.gateState,
  }));
  overview.events.unshift({
    eventId: `FB${Date.now()}`,
    deviceType: state.emergency ? "safety" : "dispatch",
    deviceId: state.emergency ? "HandoffZone" : "FallbackQueue",
    eventCode: state.emergency ? "ESTOP_ACTIVE" : "ORDER_SYNC",
    severity: state.emergency ? "critical" : "info",
    message: state.emergency
      ? "本地兜底急停已在模拟现场设备中生效"
      : `本地兜底状态已同步：${activeOrder?.plateNo || "当前订单"}`,
    eventTime: new Date().toISOString(),
    acknowledged: false,
  });
  state.devices = overview;
}

function normalizeAgvs(agvs) {
  return agvs.map((agv) => ({
    ...agv,
    loaded: agv.loaded ?? agv.load ?? false,
    batteryPct: agv.batteryPct ?? 100,
    mode: agv.mode ?? "IDLE",
    velocityMps: agv.velocityMps ?? 0,
    lastCommand: agv.lastCommand ?? "hold",
  }));
}

function normalizeDevices(overview) {
  return {
    cameras: overview?.cameras || [],
    gates: overview?.gates || [],
    chargers: overview?.chargers || [],
    events: overview?.events || [],
  };
}
