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
    ["System online", "Dashboard and fallback data source were initialized."],
    ["Vision edge node", "Latest plate OCR result is SH-A7686 with 0.982 confidence."],
    ["Dispatch center", "AGV-03 is heading to the shallow buffer lane."],
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
    ["Vehicle stored", "AGV placed the vehicle in slot E06."],
    ["Billing active", "Dynamic parking fee started after the entry workflow closed."],
    ["Owner ready", "Retrieve, touch-and-go, and VIP pickup are available."],
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
    addEvent("Vehicle admitted", `${order.plateNo} was assigned to slot ${order.slotId}.`);
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
    addEvent("Pre-dispatch queued", `${task.plateNo} was moved into the pre-dispatch queue.`);
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
    addEvent("VIP retrieval", `${task.plateNo} was inserted at the top of the queue.`);
    pushOwnerTimeline("VIP retrieval", "Priority AGV dispatch was created for the current order.");
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
      "Vision inference complete",
      result.intrusion
        ? "Intrusion detected in the handoff zone. Review is required."
        : `${result.plate} passed OCR with ${result.confidence} confidence.`,
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
      pushOwnerTimeline("Retrieve started", "An AGV retrieval job was added to the live dispatch queue.");
      addEvent("Owner request", `Retrieve request submitted for ${orderNo}.`);
    } else if (action === "touch") {
      await parkvisionApi.touchOrder(orderNo);
      pushOwnerTimeline("Touch-and-go", "The vehicle was routed to the handoff bay without closing the billing session.");
      addEvent("Owner request", `Touch-and-go request submitted for ${orderNo}.`);
    } else if (action === "pay") {
      await parkvisionApi.payOrder(orderNo);
      pushOwnerTimeline("Payment complete", "The order was closed and the slot was released.");
      addEvent("Owner request", `Payment completed for ${orderNo}.`);
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
      nextState ? "Emergency stop" : "Emergency cleared",
      nextState
        ? "Dispatch release was locked from the backend safety layer."
        : "Safety lock was released and field devices resumed automatic mode.",
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
  addEvent("Vehicle admitted", `${plateNo} was assigned to slot ${slot.id} in fallback mode.`);
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
      type: "Pre-dispatch relocation",
      tag: "PRE",
      wait: "00:48",
      vip: true,
    });
    state.agvs[0] = {
      ...state.agvs[0],
      loaded: true,
      task: `Relocating ${activeOrder.plateNo}`,
      mode: "TRANSIT",
      velocityMps: 0.78,
      lastCommand: "relocate",
    };
  }
  recomputeSummary();
  syncFallbackExperience(activeOrder);
  addEvent("Pre-dispatch queued", "Fallback pre-dispatch moved a deep slot vehicle into the buffer lane.");
}

function fallbackVip(orderNo) {
  const order = state.orders.find((item) => item.orderNo === orderNo) || getters.currentOrder.value;
  if (!order) return;
  order.status = "RETRIEVING";
  syncOrderStatus(order);
  state.queue.unshift({
    plateNo: order.plateNo,
    type: "VIP retrieval",
    tag: "VIP",
    wait: "00:30",
    vip: true,
  });
  state.agvs[0] = {
    ...state.agvs[0],
    loaded: true,
    task: `VIP pickup for ${order.plateNo}`,
    mode: "CARRYING",
    velocityMps: 0.92,
    lastCommand: "vip-priority",
  };
  recomputeSummary();
  syncFallbackExperience(order);
  pushOwnerTimeline("VIP retrieval", "Fallback dispatch inserted the order at the top of the queue.");
  addEvent("VIP retrieval", `${order.plateNo} was inserted at the top of the fallback queue.`);
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
    "Vision inference complete",
    intrusion ? "Fallback safety rule triggered an emergency review." : `${plate} passed fallback OCR inference.`,
  );
  return state.visionResult;
}

function fallbackOwnerAction(action, orderNo) {
  const order = state.orders.find((item) => item.orderNo === orderNo);
  if (!order) return;

  if (action === "retrieve") {
    order.status = "RETRIEVING";
    pushOwnerTimeline("Retrieve started", "Fallback dispatch created a retrieval job for the current order.");
  } else if (action === "touch") {
    order.status = "TOUCHING";
    pushOwnerTimeline("Touch-and-go", "Fallback dispatch routed the vehicle to the handoff bay.");
  } else if (action === "pay") {
    order.status = "FINISHED";
    order.amount = calculateFallbackAmount(order);
    pushOwnerTimeline("Payment complete", "Fallback billing closed the order and released the slot.");
  }

  syncOrderStatus(order);
  state.adminOrders = state.orders.map(toAdminOrderRow);
  recomputeSummary();
  syncFallbackExperience(order);
  addEvent("Owner request", `${action} was processed locally for ${order.orderNo}.`);
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
    nextState ? "Emergency stop" : "Emergency cleared",
    nextState
      ? "Dispatch visuals were frozen after a simulated safety event."
      : "Safety lock was released and AGV motion resumed.",
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
    chargingTurnover: "7.4 / day",
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
      ? { ...node, latency: "LOCK", level: "warning", detail: "Fallback safety lock is active across the control plane" }
      : state.emergency && node.name === "Edge-Cam-01"
        ? { ...node, latency: "ALARM", level: "warning", detail: "Fallback safety ROI escalated and blocked dispatch release" }
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
      ? "Fallback emergency stop is active across simulated field devices"
      : `Fallback state synced for ${activeOrder?.plateNo || "the active order"}`,
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
