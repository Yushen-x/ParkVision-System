import { computed, reactive } from "vue";
import { parkvisionApi, setToken, clearToken } from "../api/parkvisionApi";
import {
  mockAdminOverview,
  buildMockBillingComponents,
  buildMockIndoorRoute,
  buildMockPricingPreview,
  buildMockReport,
  createMockAdminOrders,
  createMockOrders,
  createMockSlots,
  mockAccessList,
  mockAgvs,
  mockAlerts,
  mockCustomerVehicles,
  mockDeviceOverview,
  mockForecast,
  mockPayments,
  mockPricingRules,
  mockQueue,
  mockSummary,
  mockSystemNodes,
  mockVisionResult,
  toAdminOrderRow,
} from "../data/mockData";

const fallbackPlates = ["SH-A7686", "SH-D5218", "SU-M9021", "SH-K1314", "SH-V7780"];

const AUTH_KEY = "pv-auth";
function loadAuth() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_KEY)) || null;
  } catch {
    return null;
  }
}

// Persistent overrides that survive the 5s poll (which rebuilds slots/alerts).
const alertStatusOverride = {};

export const state = reactive({
  onlineMode: "Checking backend",
  emergency: false,
  loading: false,
  twinSignal: { scenario: "", seq: 0 },
  auth: { user: loadAuth() },
  owner: { profile: null, vehicles: [], orders: [], wallet: null },
  adminUsers: [],
  auditLogs: [],
  reservations: [],
  activePlate: mockVisionResult.plate,
  summary: { ...mockSummary },
  forecast: structuredClone(mockForecast),
  events: [
    ["系统上线", "运营首页与实时数据源已初始化。"],
    ["视觉边缘节点", "最新车牌 OCR 结果为 SH-A7686，置信度 0.982。"],
    ["调度中心", "AGV-03 正在前往浅层缓冲车道。"],
  ],
  slots: createMockSlots(),
  agvs: structuredClone(mockAgvs),
  orders: createMockOrders(),
  adminOrders: createMockAdminOrders(),
  adminOverview: { ...mockAdminOverview },
  adminFilters: {
    orderStatus: "",
    orderKeyword: "",
    orderDateFrom: "",
    orderDateTo: "",
    alertLevel: "",
    alertStatus: "",
    alertKeyword: "",
    profileEnergyType: "",
    profileMemberLevel: "",
    profileKeyword: "",
    paymentStatus: "",
    paymentMethod: "",
    paymentKeyword: "",
    paymentDateFrom: "",
    paymentDateTo: "",
  },
  alerts: structuredClone(mockAlerts),
  selectedAlertNo: "AL2026050601",
  adminAlertDetail: null,
  pricingRules: structuredClone(mockPricingRules),
  accessList: structuredClone(mockAccessList),
  customerVehicles: structuredClone(mockCustomerVehicles),
  selectedCustomerOwnerId: "CUS0001",
  adminCustomerDetail: null,
  payments: structuredClone(mockPayments),
  billingComponents: buildMockBillingComponents(),
  selectedBillingOrderNo: "PV20260506004",
  selectedAdminOrderNo: "PV20260506004",
  adminOrderDetail: null,
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
    billing: false,
    detail: false,
    customerDetail: false,
    alertDetail: false,
    register: false,
    account: false,
    ownerData: false,
  },
});

export const getters = {
  freeCount: computed(() => state.slots.filter((slot) => slot.status === "empty").length),
  occupiedCount: computed(() => state.slots.filter((slot) => slot.status !== "empty").length),
  currentOrder: computed(
    () => state.orders.find((order) => order.status !== "FINISHED") || state.orders[0] || null,
  ),
  isAuthenticated: computed(() => Boolean(state.auth.user)),
  isOwner: computed(() => state.auth.user?.role === "owner"),
  activeReservations: computed(() => state.reservations.filter((item) => item.status === "HELD")),
  // Owner-scoped views: derived from the owner's own orders pulled from /api/owner.
  ownerOrders: computed(() => state.owner.orders),
  ownerActiveOrder: computed(
    () => state.owner.orders.find((order) => order.status !== "FINISHED") || null,
  ),
  ownerHistory: computed(() => state.owner.orders.filter((order) => order.status === "FINISHED")),
};

// --- Auth (fixed accounts; role is derived from the account) ----------------
export const ACCOUNTS = [
  { username: "admin", password: "admin123", role: "admin", displayName: "运营管理员" },
  { username: "owner", password: "owner123", role: "owner", displayName: "张车主" },
];

function persistUser(user) {
  state.auth.user = user;
  try {
    localStorage.setItem(AUTH_KEY, JSON.stringify(user));
  } catch {
    /* storage blocked */
  }
  addEvent("用户登录", `${user.displayName}（${user.role === "admin" ? "管理员" : "车主"}）已登录系统。`);
}

// API-first authentication (backend issues a JWT); falls back to the built-in
// accounts only when the backend is unreachable, so the system stays demoable offline.
export async function login({ username, password } = {}) {
  const name = String(username || "").trim();

  const result = await parkvisionApi.login(name, password);
  if (result.ok) {
    setToken(result.data.token);
    const user = {
      username: result.data.username,
      displayName: result.data.displayName,
      role: result.data.role,
      loginAt: new Date().toISOString(),
    };
    persistUser(user);
    void hydrate();
    return { ok: true, user };
  }

  if (result.reason === "invalid") {
    return { ok: false, error: result.message || "账号或密码不正确" };
  }

  // Network/backend unavailable -> local fallback against the built-in accounts.
  const account = ACCOUNTS.find((item) => item.username === name && item.password === password);
  if (!account) {
    return { ok: false, error: "账号或密码不正确" };
  }
  clearToken();
  const user = {
    username: account.username,
    displayName: account.displayName,
    role: account.role,
    loginAt: new Date().toISOString(),
  };
  persistUser(user);
  return { ok: true, user };
}

// Owner self-registration: backend creates the login + customer + vehicle and
// returns a JWT, so the new owner is signed straight in.
export async function register(payload = {}) {
  state.busy.register = true;
  try {
    const result = await parkvisionApi.register(payload);
    if (!result.ok) {
      return { ok: false, error: result.message || "注册失败" };
    }
    setToken(result.data.token);
    const user = {
      username: result.data.username,
      displayName: result.data.displayName,
      role: result.data.role,
      loginAt: new Date().toISOString(),
    };
    persistUser(user);
    void hydrate();
    return { ok: true, user };
  } finally {
    state.busy.register = false;
  }
}

// Pull the signed-in owner's own profile / vehicles / orders from the backend.
export async function loadOwnerData() {
  if (state.auth.user?.role !== "owner") return;
  state.busy.ownerData = true;
  try {
    const [profile, vehicles, orders, reservations, wallet] = await Promise.all([
      parkvisionApi.getOwnerProfile(),
      parkvisionApi.getOwnerVehicles(),
      parkvisionApi.getOwnerOrders(),
      parkvisionApi.getReservations(),
      parkvisionApi.getOwnerWallet().catch(() => null),
    ]);
    state.owner.profile = profile || null;
    state.owner.vehicles = Array.isArray(vehicles) ? vehicles : [];
    state.owner.orders = Array.isArray(orders) ? orders : [];
    state.owner.wallet = wallet || null;
    state.reservations = Array.isArray(reservations) ? reservations : [];
    const active = getters.ownerActiveOrder.value;
    if (active) state.activePlate = active.plateNo;
  } catch {
    /* backend unreachable: keep existing owner state */
  } finally {
    state.busy.ownerData = false;
  }
}

export function logout() {
  state.auth.user = null;
  state.owner = { profile: null, vehicles: [], orders: [], wallet: null };
  clearToken();
  try {
    localStorage.removeItem(AUTH_KEY);
  } catch {
    /* storage blocked */
  }
}

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
  if (state.auth.user?.role === "owner") {
    await loadOwnerData();
  }
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

// Fire a one-shot signal the digital twin watches to play a transfer animation.
export function signalTwin(scenario) {
  state.twinSignal.scenario = scenario;
  state.twinSignal.seq += 1;
}

/* ------------------------------------------------------------------ *
 * Realtime digital-twin stream (Server-Sent Events)                   *
 * The backend pushes authoritative state (slots / summary / queue /   *
 * AGV / emergency) sub-second, so the twin and dashboard reflect the  *
 * real database without waiting for the slower poll. EventSource has   *
 * built-in auto-reconnect; polling stays as a fallback while the       *
 * stream is unavailable.                                               *
 * ------------------------------------------------------------------ */
let twinStream = null;

function resolveTwinStreamUrl() {
  const base = import.meta.env.VITE_API_BASE_URL || "/api";
  return `${base.replace(/\/$/, "")}/twin/stream`;
}

function applyTwinSnapshot(snapshot) {
  if (!snapshot) return;
  if (snapshot.summary) state.summary = snapshot.summary;
  if (Array.isArray(snapshot.slots)) state.slots = snapshot.slots;
  if (Array.isArray(snapshot.agvs)) state.agvs = normalizeAgvs(snapshot.agvs);
  if (Array.isArray(snapshot.queue)) state.queue = snapshot.queue;
  reapplyReservations();
  if (typeof snapshot.emergency === "boolean") state.emergency = snapshot.emergency;
  state.activePlate = getters.currentOrder.value?.plateNo || state.activePlate;
}

function onTwinMessage(event) {
  try {
    applyTwinSnapshot(JSON.parse(event.data));
    state.onlineMode = "Realtime stream";
  } catch {
    /* ignore malformed frame */
  }
}

export function connectTwinStream() {
  if (typeof window === "undefined" || !("EventSource" in window)) return;
  if (twinStream) return;
  try {
    twinStream = new EventSource(resolveTwinStreamUrl());
  } catch {
    twinStream = null;
    return;
  }
  twinStream.addEventListener("twin", onTwinMessage);
  twinStream.onmessage = onTwinMessage;
  twinStream.onerror = () => {
    // EventSource reconnects automatically; show that we fell back to polling.
    if (state.onlineMode === "Realtime stream") state.onlineMode = "Backend connected";
  };
}

export function disconnectTwinStream() {
  if (twinStream) {
    try {
      twinStream.close();
    } catch {
      /* already closed */
    }
    twinStream = null;
  }
}

// --- Reservation -> hold -> entry 业务闭环（后端 + 数据库） -------------------
export async function createReservation({ plateNo, phone, energyType } = {}) {
  try {
    const reservation = await parkvisionApi.createReservation({ plateNo, phone, energyType });
    addEvent("车位预约", `${reservation.plateNo} 已锁定车位 ${reservation.slotId}，保留 15 分钟。`);
    await loadOwnerData();
    await refreshCore();
    return { ok: true, reservation };
  } catch (error) {
    addEvent("预约失败", error.message || "预约失败。");
    return { ok: false, error: error.message || "预约失败" };
  }
}

export async function cancelReservation(id) {
  try {
    const reservation = await parkvisionApi.cancelReservation(id);
    addEvent("预约取消", `${reservation.plateNo} 的预约已取消，车位 ${reservation.slotId} 释放。`);
    await loadOwnerData();
    await refreshCore();
    return { ok: true };
  } catch (error) {
    return { ok: false, error: error.message || "取消失败" };
  }
}

export async function fulfillReservation(id) {
  try {
    const reservation = await parkvisionApi.fulfillReservation(id);
    state.activePlate = reservation.plateNo;
    addEvent("预约到场", `${reservation.plateNo} 已到场，车位 ${reservation.slotId} 转为正式停车订单。`);
    await loadOwnerData();
    await refreshCore();
    signalTwin("storage");
    return { ok: true, reservation };
  } catch (error) {
    return { ok: false, error: error.message || "到场确认失败" };
  }
}

// Held reservations are now persisted server-side (slot status = reserved), so
// no client-side re-application is needed after a poll.
function reapplyReservations() {}

// Register an entry for a recognised plate (AI 视觉中枢 → 后端真实入场落库).
export async function registerEntry({ plateNo, energyType } = {}) {
  void energyType;
  try {
    const order = await parkvisionApi.simulateEntry(plateNo);
    state.activePlate = order.plateNo;
    addEvent("车辆入场", `${order.plateNo} 经车牌识别放行，自动分配车位 ${order.slotId}。`);
    await refreshCore();
    await refreshAdminData();
    signalTwin("storage");
    return { ok: true, order };
  } catch (error) {
    registerEntryLocal({ plateNo, energyType });
    return { ok: false, error: error.message || "入场失败" };
  }
}

function registerEntryLocal({ plateNo, energyType } = {}) {
  const slot = state.slots.find((item) => item.status === "empty");
  if (!slot) {
    addEvent("入场失败", "车位已满，无法分配新车位。");
    return null;
  }
  const plate = (plateNo || fallbackPlates[Math.floor(Math.random() * fallbackPlates.length)]).toUpperCase();
  slot.status = energyType === "Electric" || plate.includes("D") ? "charging" : "occupied";
  slot.available = false;

  const order = {
    orderNo: `PV${Date.now().toString().slice(-9)}`,
    plateNo: plate,
    slotId: slot.id,
    entryTime: new Date().toISOString(),
    status: "PARKED",
    amount: 0,
  };
  state.orders.unshift(order);
  state.adminOrders.unshift(toAdminOrderRow(order));
  state.activePlate = plate;
  recomputeSummary();
  syncFallbackExperience(order);
  signalTwin("storage");
  return order;
}

// --- Vehicle / customer CRUD (mock 数据库台账) ------------------------------
// Persist a customer vehicle profile through the backend; the local mutation is
// kept only as an offline fallback so the demo never breaks.
export async function upsertVehicle(vehicle = {}) {
  const plateNo = String(vehicle.plateNo || "").trim().toUpperCase();
  if (!plateNo) return { ok: false, error: "车牌不能为空" };
  state.busy.account = true;
  try {
    const row = await parkvisionApi.upsertCustomerVehicle({
      ownerId: vehicle.ownerId,
      ownerName: vehicle.ownerName,
      phone: vehicle.phone || vehicle.phoneMasked,
      plateNo,
      vehicleType: vehicle.vehicleType,
      energyType: vehicle.energyType,
      membershipType: vehicle.membershipType || vehicle.memberLevel,
      memberLevel: vehicle.memberLevel,
      accountStatus: vehicle.accountStatus,
      accessType: vehicle.accessType,
    });
    addEvent("档案保存", `已保存车辆 ${row.plateNo}（${row.ownerName}）。`);
    await refreshAdminData();
    return { ok: true, row };
  } catch (error) {
    upsertVehicleLocal({ ...vehicle, plateNo });
    return { ok: false, error: error.message || "保存失败，已暂存本地" };
  } finally {
    state.busy.account = false;
  }
}

export async function removeVehicle(plateNo) {
  if (!plateNo) return { ok: false, error: "缺少车牌" };
  state.busy.account = true;
  try {
    await parkvisionApi.deleteCustomerVehicle(plateNo);
    addEvent("档案删除", `已删除车辆 ${plateNo} 的客户档案。`);
    await refreshAdminData();
    return { ok: true };
  } catch (error) {
    removeVehicleLocal(plateNo);
    return { ok: false, error: error.message || "删除失败" };
  } finally {
    state.busy.account = false;
  }
}

function upsertVehicleLocal(vehicle = {}) {
  const plateNo = String(vehicle.plateNo || "").trim().toUpperCase();
  if (!plateNo) return null;
  const existing = state.customerVehicles.find((item) => item.plateNo === plateNo);
  if (existing) {
    Object.assign(existing, { ...vehicle, plateNo });
    return existing;
  }
  const row = {
    ownerId: vehicle.ownerId || `CUS${String(Date.now()).slice(-4)}`,
    ownerName: vehicle.ownerName || "新客户",
    phoneMasked: vehicle.phoneMasked || "138****0000",
    plateNo,
    energyType: vehicle.energyType || "Fuel",
    memberLevel: vehicle.memberLevel || "Standard",
    membershipType: vehicle.membershipType || vehicle.memberLevel || "Standard",
    accountStatus: vehicle.accountStatus || "Active",
    accessType: vehicle.accessType || "Allow",
    createdAt: new Date().toISOString(),
  };
  state.customerVehicles.unshift(row);
  return row;
}

function removeVehicleLocal(plateNo) {
  const index = state.customerVehicles.findIndex((item) => item.plateNo === plateNo);
  if (index < 0) return;
  state.customerVehicles.splice(index, 1);
}

// --- Admin account management (persisted to app_user) ----------------------
export async function loadAdminUsers() {
  state.busy.account = true;
  try {
    const rows = await parkvisionApi.listUsers();
    state.adminUsers = Array.isArray(rows) ? rows : [];
    return { ok: true };
  } catch (error) {
    state.adminUsers = [];
    return { ok: false, error: error.message || "无法加载账号列表" };
  } finally {
    state.busy.account = false;
  }
}

export async function createAdminUser(body = {}) {
  state.busy.account = true;
  try {
    const row = await parkvisionApi.createUser(body);
    addEvent("账号新增", `已创建账号 ${row.username}（${row.role === "admin" ? "管理员" : "车主"}）。`);
    await loadAdminUsers();
    return { ok: true, row };
  } catch (error) {
    return { ok: false, error: error.message || "创建账号失败" };
  } finally {
    state.busy.account = false;
  }
}

export async function updateAdminUser(id, body = {}) {
  state.busy.account = true;
  try {
    const row = await parkvisionApi.updateUser(id, body);
    addEvent("账号更新", `账号 ${row.username} 已更新。`);
    await loadAdminUsers();
    return { ok: true, row };
  } catch (error) {
    return { ok: false, error: error.message || "更新账号失败" };
  } finally {
    state.busy.account = false;
  }
}

export async function resetAdminUserPassword(id, password) {
  try {
    await parkvisionApi.resetUserPassword(id, password);
    addEvent("密码重置", `账号 #${id} 的登录密码已重置。`);
    return { ok: true };
  } catch (error) {
    return { ok: false, error: error.message || "重置密码失败" };
  }
}

// --- Alert acknowledge / resolve (persisted to alert_event) ----------------
export async function acknowledgeAlert(alertNo) {
  try {
    await parkvisionApi.acknowledgeAlert(alertNo);
    setAlertStatus(alertNo, "处理中", "告警确认");
    await refreshAdminData();
  } catch {
    setAlertStatus(alertNo, "处理中", "告警确认");
  }
}

export async function resolveAlert(alertNo) {
  try {
    await parkvisionApi.resolveAlert(alertNo);
    setAlertStatus(alertNo, "已恢复", "告警解除");
    await refreshAdminData();
  } catch {
    setAlertStatus(alertNo, "已恢复", "告警解除");
  }
}

function setAlertStatus(alertNo, status, title) {
  alertStatusOverride[alertNo] = status;
  const alert = state.alerts.find((item) => item.alertNo === alertNo);
  if (alert) alert.status = status;
  if (state.adminAlertDetail?.alertNo === alertNo) state.adminAlertDetail.status = status;
  addEvent(title, `告警 ${alertNo} 已标记为「${status}」。`);
}

export async function simulateEntry(plateNo) {
  state.busy.entry = true;
  try {
    const order = await parkvisionApi.simulateEntry(plateNo);
    addEvent("车辆入场", `${order.plateNo} 已分配到车位 ${order.slotId}。`);
    state.activePlate = order.plateNo;
    await refreshCore();
    await refreshAdminData();
    return { ok: true, order };
  } catch (error) {
    fallbackSimulateEntry();
    return { ok: false, error: error.message || "入场失败" };
  } finally {
    state.busy.entry = false;
    signalTwin("storage");
  }
}

// Owner self check-in for one of their own bound vehicles.
export async function ownerEntry(plateNo) {
  state.busy.entry = true;
  try {
    const order = await parkvisionApi.ownerEntry(plateNo);
    addEvent("车辆入场", `${order.plateNo} 已分配到车位 ${order.slotId}。`);
    state.activePlate = order.plateNo;
    await refreshCore();
    await loadOwnerData();
    signalTwin("storage");
    return { ok: true, order };
  } catch (error) {
    return { ok: false, error: error.message || "入场失败" };
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
    signalTwin("retrieve");
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
    signalTwin("retrieve");
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

export async function runOwnerAction(action, orderNo) {
  const isOwner = state.auth.user?.role === "owner";
  const targetNo =
    orderNo ||
    (isOwner ? getters.ownerActiveOrder.value?.orderNo : getters.currentOrder.value?.orderNo);
  if (!targetNo) return { ok: false, error: "没有可操作的订单" };
  state.busy.ownerAction = true;

  try {
    if (action === "retrieve") {
      await (isOwner ? parkvisionApi.ownerRetrieve(targetNo) : parkvisionApi.retrieveOrder(targetNo));
      pushOwnerTimeline("取车已启动", "AGV 取车任务已加入实时调度队列。");
      addEvent("车主请求", `订单 ${targetNo} 已提交取车请求。`);
    } else if (action === "touch") {
      await (isOwner ? parkvisionApi.ownerTouch(targetNo) : parkvisionApi.touchOrder(targetNo));
      pushOwnerTimeline("临停取物", "车辆已被调度到交接区，计费会话保持开启。");
      addEvent("车主请求", `订单 ${targetNo} 已提交临停取物请求。`);
    } else if (action === "pay") {
      await (isOwner ? parkvisionApi.ownerPay(targetNo) : parkvisionApi.payOrder(targetNo));
      pushOwnerTimeline("支付完成", "订单已关闭，余额已扣减，车位已释放。");
      addEvent("车主请求", `订单 ${targetNo} 已完成支付。`);
    }

    await refreshCore();
    if (isOwner) await loadOwnerData();
    else await refreshAdminData();
    return { ok: true };
  } catch (error) {
    if (error?.isApiError) {
      addEvent("操作失败", error.message || "操作未完成。");
      return { ok: false, error: error.message || "操作未完成" };
    }
    fallbackOwnerAction(action, targetNo);
    return { ok: false, error: error?.message || "网络异常，已本地处理" };
  } finally {
    state.busy.ownerAction = false;
    if (action === "retrieve") signalTwin("retrieve");
    else if (action === "touch") signalTwin("touch");
  }
}

export async function rechargeWallet(amount) {
  try {
    const wallet = await parkvisionApi.ownerRecharge(amount);
    state.owner.wallet = wallet || state.owner.wallet;
    if (state.owner.profile && wallet) state.owner.profile.balance = wallet.balance;
    addEvent("钱包充值", `充值 ${Number(amount).toFixed(2)} 元成功。`);
    return { ok: true, wallet };
  } catch (error) {
    return { ok: false, error: error?.message || "充值失败" };
  }
}

export async function loadOwnerBill(orderNo) {
  try {
    const bill = await parkvisionApi.getOwnerBill(orderNo);
    return { ok: true, bill };
  } catch (error) {
    return { ok: false, error: error?.message || "无法加载账单" };
  }
}

// --- Admin pricing rule CRUD (persisted to pricing_rule) -------------------
export async function createPricingRule(body = {}) {
  try {
    const rule = await parkvisionApi.createPricingRule(body);
    addEvent("计费规则新增", `已新增计费规则「${rule.name}」。`);
    await refreshAdminData();
    return { ok: true, rule };
  } catch (error) {
    return { ok: false, error: error?.message || "新增计费规则失败" };
  }
}

export async function updatePricingRule(id, body = {}) {
  try {
    const rule = await parkvisionApi.updatePricingRule(id, body);
    addEvent("计费规则更新", `计费规则「${rule.name}」已更新。`);
    await refreshAdminData();
    return { ok: true, rule };
  } catch (error) {
    return { ok: false, error: error?.message || "更新计费规则失败" };
  }
}

export async function deletePricingRule(id) {
  try {
    await parkvisionApi.deletePricingRule(id);
    addEvent("计费规则删除", `计费规则 ${id} 已删除。`);
    await refreshAdminData();
    return { ok: true };
  } catch (error) {
    return { ok: false, error: error?.message || "删除计费规则失败" };
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

export async function setDeviceStatus(type, id, status) {
  try {
    await parkvisionApi.setDeviceStatus(type, id, status);
    const labelMap = { ONLINE: "恢复在线", OFFLINE: "停用下线", MAINTENANCE: "进入维护" };
    addEvent("设备控制", `${id} 已${labelMap[status] || status}。`);
    await refreshCore();
    return { ok: true };
  } catch (error) {
    return { ok: false, error: error?.message || "设备控制失败" };
  }
}

export async function loadAuditLogs(limit = 100) {
  try {
    const logs = await parkvisionApi.getAuditLogs(limit);
    state.auditLogs = Array.isArray(logs) ? logs : [];
  } catch {
    state.auditLogs = [];
  }
  return state.auditLogs;
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
  const ordersParams = {
    status: state.adminFilters.orderStatus,
    keyword: state.adminFilters.orderKeyword,
    dateFrom: state.adminFilters.orderDateFrom,
    dateTo: state.adminFilters.orderDateTo,
  };
  const alertsParams = {
    level: state.adminFilters.alertLevel,
    status: state.adminFilters.alertStatus,
    keyword: state.adminFilters.alertKeyword,
  };
  const profilesParams = {
    energyType: state.adminFilters.profileEnergyType,
    memberLevel: state.adminFilters.profileMemberLevel,
    keyword: state.adminFilters.profileKeyword,
  };
  const paymentsParams = {
    status: state.adminFilters.paymentStatus,
    method: state.adminFilters.paymentMethod,
    keyword: state.adminFilters.paymentKeyword,
    dateFrom: state.adminFilters.paymentDateFrom,
    dateTo: state.adminFilters.paymentDateTo,
  };

  const [adminOrders, alerts, pricingRules, accessList, customerVehicles, payments, adminOverview] = await Promise.all([
    parkvisionApi.getAdminOrders(ordersParams),
    parkvisionApi.getAlerts(alertsParams),
    parkvisionApi.getPricingRules(),
    parkvisionApi.getAccessList(),
    parkvisionApi.getCustomerVehicles(profilesParams),
    parkvisionApi.getPayments(paymentsParams),
    parkvisionApi.getAdminOverview(),
  ]);

  return {
    adminOrders,
    alerts,
    pricingRules,
    accessList,
    customerVehicles,
    payments,
    adminOverview,
  };
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
  reapplyReservations();
  syncVisionFromDevices();
  state.activePlate = getters.currentOrder.value?.plateNo || state.visionResult.plate || state.activePlate;
  state.emergency = deriveEmergencyState();
}

function applyAdminData(data) {
  state.adminOrders = data.adminOrders;
  state.alerts = data.alerts.map((alert) =>
    alertStatusOverride[alert.alertNo] ? { ...alert, status: alertStatusOverride[alert.alertNo] } : alert,
  );
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
  addEvent("车辆入场", `${plateNo} 已自动分配到车位 ${slot.id}。`);
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
  addEvent("预调度入队", "预调度已将深层车位车辆移入缓冲车道。");
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
  pushOwnerTimeline("VIP 优先取车", "调度系统已将订单插入队首。");
  addEvent("VIP 优先取车", `${order.plateNo} 已插入调度队列队首。`);
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
    intrusion ? "安全规则已触发急停复核。" : `${plate} 已通过车牌识别。`,
  );
  return state.visionResult;
}

function fallbackOwnerAction(action, orderNo) {
  const order = state.orders.find((item) => item.orderNo === orderNo);
  if (!order) return;

  if (action === "retrieve") {
    order.status = "RETRIEVING";
    pushOwnerTimeline("取车已启动", "调度系统已为当前订单创建取车任务。");
  } else if (action === "touch") {
    order.status = "TOUCHING";
    pushOwnerTimeline("临停取物", "调度系统已将车辆送往交接区。");
  } else if (action === "pay") {
    order.status = "FINISHED";
    order.amount = calculateFallbackAmount(order);
    pushOwnerTimeline("支付完成", "计费系统已关闭订单并释放车位。");
  }

  syncOrderStatus(order);
  state.adminOrders = state.orders.map(toAdminOrderRow);
  recomputeSummary();
  syncFallbackExperience(order);
  addEvent("车主请求", `订单 ${order.orderNo} 的 ${action} 操作已处理。`);
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
      ? "安全事件触发后，调度画面已冻结。"
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
  const today = new Date().toDateString();
  const todayTraffic = state.orders.filter(
    (order) => order.entryTime && new Date(order.entryTime).toDateString() === today
  ).length;
  const agvOnline = state.agvs.filter(
    (agv) => Number(agv.batteryPct ?? agv.battery ?? 100) > 10 && String(agv.mode || "").toUpperCase() !== "OFFLINE"
  ).length;
  const chargingActive = state.slots.filter((slot) => slot.status === "charging").length;
  state.summary = {
    occupancyRate: state.slots.length ? Math.round((occupied / state.slots.length) * 100) : 0,
    trafficTotal: todayTraffic,
    agvOnline: `${agvOnline}/${state.agvs.length}`,
    alertCount: state.alerts.length,
    revenue: Math.round(revenue),
    avgWait: state.queue[0]?.wait || "00:00",
    chargingTurnover: `${chargingActive} 充电中`,
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
      ? { ...node, latency: "锁定", level: "warning", detail: "安全锁已在控制平面生效" }
      : state.emergency && node.name === "Edge-Cam-01"
        ? { ...node, latency: "告警", level: "warning", detail: "安全区告警已升级并阻止调度放行" }
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
      ? "急停已在现场设备中生效"
      : `运行状态已同步：${activeOrder?.plateNo || "当前订单"}`,
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

export async function updateAdminFilters(filters) {
  Object.assign(state.adminFilters, filters);
  await refreshAdminData();
}

export async function resetAdminFilters(tab) {
  if (tab === "orders") {
    state.adminFilters.orderStatus = "";
    state.adminFilters.orderKeyword = "";
    state.adminFilters.orderDateFrom = "";
    state.adminFilters.orderDateTo = "";
  } else if (tab === "alerts") {
    state.adminFilters.alertLevel = "";
    state.adminFilters.alertStatus = "";
    state.adminFilters.alertKeyword = "";
  } else if (tab === "profiles") {
    state.adminFilters.profileEnergyType = "";
    state.adminFilters.profileMemberLevel = "";
    state.adminFilters.profileKeyword = "";
  } else if (tab === "payments") {
    state.adminFilters.paymentStatus = "";
    state.adminFilters.paymentMethod = "";
    state.adminFilters.paymentKeyword = "";
    state.adminFilters.paymentDateFrom = "";
    state.adminFilters.paymentDateTo = "";
  }
  await refreshAdminData();
}

export async function loadAdminOrderDetail(orderNo) {
  state.selectedAdminOrderNo = orderNo;
  state.adminOrderDetail = null;
  state.busy.detail = true;
  try {
    state.adminOrderDetail = await parkvisionApi.getAdminOrderDetail(orderNo);
  } catch {
    state.adminOrderDetail = fallbackAdminOrderDetail(orderNo);
  } finally {
    state.busy.detail = false;
  }
}

export async function loadAdminCustomerDetail(ownerId) {
  state.selectedCustomerOwnerId = ownerId;
  state.adminCustomerDetail = null;
  state.busy.customerDetail = true;
  try {
    state.adminCustomerDetail = await parkvisionApi.getAdminCustomerDetail(ownerId);
  } catch {
    state.adminCustomerDetail = fallbackAdminCustomerDetail(ownerId);
  } finally {
    state.busy.customerDetail = false;
  }
}

export async function loadAdminAlertDetail(alertNo) {
  state.selectedAlertNo = alertNo;
  state.adminAlertDetail = null;
  state.busy.alertDetail = true;
  try {
    state.adminAlertDetail = await parkvisionApi.getAdminAlertDetail(alertNo);
  } catch {
    state.adminAlertDetail = fallbackAdminAlertDetail(alertNo);
  } finally {
    state.busy.alertDetail = false;
  }
}

export async function loadBillingComponents(orderNo) {
  state.selectedBillingOrderNo = orderNo;
  state.billingComponents = [];
  state.busy.billing = true;
  try {
    state.billingComponents = await parkvisionApi.getBillingComponents(orderNo);
  } catch {
    state.billingComponents = buildMockBillingComponents(orderNo);
  } finally {
    state.busy.billing = false;
  }
}

function fallbackAdminOrderDetail(orderNo) {
  const order = state.orders.find((item) => item.orderNo === orderNo);
  if (!order) return null;
  const orderRow = state.adminOrders.find((item) => item.orderNo === orderNo) || toAdminOrderRow(order);
  const profile = mockCustomerVehicles.find((item) => item.plateNo === order.plateNo);
  const payment = mockPayments.find((item) => item.orderNo === orderNo);
  const billingComponents = state.billingComponents && state.billingComponents[0]?.orderNo === orderNo
      ? state.billingComponents.map((item) => ({ ...item }))
      : buildMockBillingComponents(orderNo);
  const paidAt = order.paidAt || payment?.paidAt || null;
  const durationMinutes =
    order.durationMinutes ??
    computeDurationMinutes(order.entryTime, order.exitTime || paidAt);

  return {
    orderNo: order.orderNo,
    plateNo: order.plateNo,
    slotId: order.slotId,
    event: orderRow.event,
    status: orderRow.status,
    entryTime: normalizeAdminTime(order.entryTime),
    exitTime: normalizeAdminTime(order.exitTime || paidAt),
    durationMinutes,
    amount: Number(order.amount || 0),
    discountAmount: Number(order.discountAmount || 0),
    paymentStatus: order.paymentStatus || (payment ? "PAID" : "UNPAID"),
    paymentMethod: order.paymentMethod || payment?.method || null,
    paidAt: normalizeAdminTime(paidAt),
    customer: profile
      ? {
          ownerId: profile.ownerId,
          ownerName: profile.ownerName,
          phoneMasked: profile.phoneMasked,
          memberLevel: profile.memberLevel,
          accountStatus: profile.accountStatus,
          balance: null,
          createdAt: null,
        }
      : null,
    vehicle: profile
      ? {
          plateNo: profile.plateNo,
          ownerId: profile.ownerId,
          vehicleType: "PASSENGER",
          energyType: profile.energyType,
          membershipType: profile.membershipType,
          defaultAuthStatus: "ALLOW",
          createdAt: null,
        }
      : null,
    payment: payment
      ? {
          paymentNo: payment.paymentNo,
          amount: Number(payment.amount || 0),
          method: payment.method,
          status: payment.status,
          paidAt: normalizeAdminTime(payment.paidAt),
        }
      : null,
    billingComponents,
  };
}

function fallbackAdminCustomerDetail(ownerId) {
  const profile = mockCustomerVehicles.find((item) => item.ownerId === ownerId);
  if (!profile) return null;

  const vehicles = mockCustomerVehicles
    .filter((item) => item.ownerId === ownerId)
    .map((item) => ({
      plateNo: item.plateNo,
      ownerId: item.ownerId,
      vehicleType: "PASSENGER",
      energyType: item.energyType,
      membershipType: item.membershipType,
      defaultAuthStatus: "ALLOW",
      createdAt: item.createdAt || "2026-01-01T08:00:00Z",
    }));

  const recentOrders = state.orders
    .filter((order) => vehicles.some((v) => v.plateNo === order.plateNo))
    .slice(0, 5)
    .map((order) => {
      const orderRow = state.adminOrders.find((item) => item.orderNo === order.orderNo) || toAdminOrderRow(order);
      return {
        orderNo: order.orderNo,
        entryTime: normalizeAdminTime(order.entryTime),
        exitTime: normalizeAdminTime(order.exitTime || order.paidAt),
        amount: Number(order.amount || 0),
        status: orderRow.status,
      };
    });

  const lastPayment = mockPayments
    .filter((p) => p.ownerId === ownerId || recentOrders.some((o) => o.orderNo === p.orderNo))
    .sort((a, b) => new Date(b.paidAt) - new Date(a.paidAt))[0];

  return {
    ownerId: profile.ownerId,
    ownerName: profile.ownerName,
    phoneMasked: profile.phoneMasked,
    memberLevel: profile.memberLevel,
    accountStatus: profile.accountStatus,
    balance: 100.00,
    createdAt: profile.createdAt || "2026-01-01T08:00:00Z",
    lastPaymentAt: lastPayment ? normalizeAdminTime(lastPayment.paidAt) : "无",
    vehicles,
    recentOrders,
  };
}

function fallbackAdminAlertDetail(alertNo) {
  const alert = state.alerts.find((item) => item.alertNo === alertNo);
  if (!alert) return null;

  const deviceEvents = state.devices.events.slice(0, 2).map((event) => ({
    eventId: event.eventId,
    deviceId: event.deviceId,
    eventCode: event.eventCode,
    severity: event.severity || "warning",
    message: event.message,
    eventTime: normalizeAdminTime(event.eventTime),
  }));

  const relatedOrders = state.orders.slice(0, 1).map((order) => {
    const orderRow = state.adminOrders.find((item) => item.orderNo === order.orderNo) || toAdminOrderRow(order);
    return {
      orderNo: order.orderNo,
      plateNo: order.plateNo,
      slotId: order.slotId,
      status: orderRow.status,
      amount: order.amount,
    };
  });

  return {
    alertNo: alert.alertNo,
    type: alert.type,
    content: alert.content,
    status: alert.status,
    level: alert.level,
    recommendedAction: alert.type === "INTRUSION" ? "立即查看监控并指派保安前往" : "检查车位传感器及网络连接",
    deviceEvents,
    relatedOrders,
  };
}

function normalizeAdminTime(timeStr) {
  if (!timeStr) return "无";
  try {
    const d = new Date(timeStr);
    if (isNaN(d.getTime())) return timeStr;
    const pad = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  } catch {
    return timeStr;
  }
}

function computeDurationMinutes(entryTime, exitTime) {
  if (!entryTime) return 0;
  const start = new Date(entryTime).getTime();
  const end = exitTime ? new Date(exitTime).getTime() : Date.now();
  if (isNaN(start) || isNaN(end)) return 0;
  return Math.max(1, Math.ceil((end - start) / 60000));
}
