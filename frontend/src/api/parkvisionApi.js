import {
  buildMockBillingComponents,
  mockAdminOverview,
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
  buildMockIndoorRoute,
  buildMockPricingPreview,
  mockPricingRules,
  mockQueue,
  mockSummary,
  mockSystemNodes,
  mockVisionResult,
} from "../data/mockData";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api";
const TOKEN_KEY = "pv-token";

export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY) || "";
  } catch {
    return "";
  }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* storage blocked */
  }
}

export function clearToken() {
  setToken("");
}

function authHeaders() {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...authHeaders(), ...(options.headers || {}) },
    ...options,
  });

  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(payload?.message || `API ${path} failed with ${response.status}`);
    error.code = payload?.code || null;
    error.status = response.status;
    error.isApiError = true;
    throw error;
  }
  if (!payload) {
    throw new Error(`API ${path} returned a non-JSON or empty response`);
  }

  return payload?.data ?? payload;
}

async function withFallback(fetcher, fallback) {
  try {
    return await fetcher();
  } catch {
    return typeof fallback === "function" ? fallback() : structuredClone(fallback);
  }
}

function withQuery(path, params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, value);
    }
  });
  const qs = search.toString();
  return qs ? `${path}?${qs}` : path;
}

function includesIgnoreCase(value, keyword) {
  if (!keyword) return true;
  return String(value ?? "").toLowerCase().includes(String(keyword).toLowerCase());
}

function matchesExact(value, expected) {
  if (!expected) return true;
  return String(value ?? "").toLowerCase() === String(expected).toLowerCase();
}

function matchesDateRange(value, dateFrom, dateTo) {
  if (!dateFrom && !dateTo) return true;
  if (!value) return false;
  const current = new Date(value);
  if (Number.isNaN(current.getTime())) return false;
  if (dateFrom) {
    const from = new Date(`${dateFrom}T00:00:00`);
    if (!Number.isNaN(from.getTime()) && current < from) return false;
  }
  if (dateTo) {
    const to = new Date(`${dateTo}T23:59:59`);
    if (!Number.isNaN(to.getTime()) && current > to) return false;
  }
  return true;
}

function filterAdminOrdersFallback(params = {}) {
  const adminRows = createMockAdminOrders();
  return createMockOrders()
    .filter((row) =>
      matchesExact(row.status, params.status) &&
      matchesDateRange(row.entryTime, params.dateFrom, params.dateTo) &&
      [row.orderNo, row.plateNo, row.slotId].some((value) => includesIgnoreCase(value, params.keyword)),
    )
    .map((row) => adminRows.find((adminRow) => adminRow.orderNo === row.orderNo))
    .filter(Boolean);
}

function filterAlertsFallback(params = {}) {
  return mockAlerts.filter((row) =>
    matchesExact(row.level, params.level) &&
    matchesExact(row.status, params.status) &&
    [row.alertNo, row.type, row.content].some((value) => includesIgnoreCase(value, params.keyword)),
  );
}

function filterCustomerVehiclesFallback(params = {}) {
  return mockCustomerVehicles.filter((row) =>
    matchesExact(row.energyType, params.energyType) &&
    matchesExact(row.memberLevel, params.memberLevel) &&
    [row.ownerId, row.ownerName, row.plateNo, row.phoneMasked].some((value) => includesIgnoreCase(value, params.keyword)),
  );
}

function filterPaymentsFallback(params = {}) {
  return mockPayments.filter((row) =>
    matchesExact(row.status, params.status) &&
    matchesExact(row.method, params.method) &&
    matchesDateRange(row.paidAt, params.dateFrom, params.dateTo) &&
    [row.paymentNo, row.orderNo, row.plateNo].some((value) => includesIgnoreCase(value, params.keyword)),
  );
}

export const parkvisionApi = {
  async login(username, password) {
    try {
      const response = await fetch(`${API_BASE}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      const payload = await response.json().catch(() => null);
      if (response.ok && payload?.success && payload?.data?.token) {
        return { ok: true, data: payload.data };
      }
      return { ok: false, reason: "invalid", message: payload?.message || "账号或密码不正确" };
    } catch {
      return { ok: false, reason: "network" };
    }
  },
  async register(payload = {}) {
    try {
      const response = await fetch(`${API_BASE}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const body = await response.json().catch(() => null);
      if (response.ok && body?.success && body?.data?.token) {
        return { ok: true, data: body.data };
      }
      return { ok: false, reason: "invalid", message: body?.message || "注册失败" };
    } catch {
      return { ok: false, reason: "network", message: "无法连接服务器" };
    }
  },
  probeBackend() {
    return request("/dashboard/summary");
  },
  // --- Owner self-service (scoped to the signed-in owner) -------------------
  getOwnerProfile() {
    return request("/owner/me");
  },
  getOwnerVehicles() {
    return request("/owner/vehicles");
  },
  getOwnerOrders() {
    return request("/owner/orders");
  },
  ownerEntry(plateNo) {
    return request(withQuery("/owner/entry", { plateNo }), { method: "POST" });
  },
  getReservations() {
    return request("/owner/reservations");
  },
  createReservation(body) {
    return request("/owner/reservations", { method: "POST", body: JSON.stringify(body || {}) });
  },
  cancelReservation(id) {
    return request(`/owner/reservations/${encodeURIComponent(id)}/cancel`, { method: "POST" });
  },
  fulfillReservation(id) {
    return request(`/owner/reservations/${encodeURIComponent(id)}/fulfill`, { method: "POST" });
  },
  ownerRetrieve(orderNo) {
    return request(`/owner/orders/${orderNo}/retrieve`, { method: "POST" });
  },
  ownerTouch(orderNo) {
    return request(`/owner/orders/${orderNo}/touch-and-go`, { method: "POST" });
  },
  ownerPay(orderNo) {
    return request(`/owner/orders/${orderNo}/pay`, { method: "POST" });
  },
  getOwnerWallet() {
    return request("/owner/wallet");
  },
  ownerRecharge(amount) {
    return request("/owner/wallet/recharge", { method: "POST", body: JSON.stringify({ amount }) });
  },
  getOwnerBill(orderNo) {
    return request(`/owner/orders/${encodeURIComponent(orderNo)}/bill`);
  },
  // --- Admin account management (ROLE_ADMIN) -------------------------------
  listUsers() {
    return request("/admin/users");
  },
  createUser(body) {
    return request("/admin/users", { method: "POST", body: JSON.stringify(body || {}) });
  },
  updateUser(id, body) {
    return request(`/admin/users/${id}`, { method: "PUT", body: JSON.stringify(body || {}) });
  },
  resetUserPassword(id, password) {
    return request(`/admin/users/${id}/password`, { method: "POST", body: JSON.stringify({ password }) });
  },
  // --- Admin customer/vehicle profile writes ------------------------------
  upsertCustomerVehicle(body) {
    return request("/admin/customer-vehicles", { method: "POST", body: JSON.stringify(body || {}) });
  },
  deleteCustomerVehicle(plateNo) {
    return request(`/admin/customer-vehicles/${encodeURIComponent(plateNo)}`, { method: "DELETE" });
  },
  // --- Admin alert lifecycle ----------------------------------------------
  acknowledgeAlert(alertNo) {
    return request(`/admin/alerts/${encodeURIComponent(alertNo)}/ack`, { method: "POST" });
  },
  resolveAlert(alertNo) {
    return request(`/admin/alerts/${encodeURIComponent(alertNo)}/resolve`, { method: "POST" });
  },
  getAdminOverview() {
    return withFallback(() => request("/admin/overview"), mockAdminOverview);
  },
  getSummary() {
    return withFallback(() => request("/dashboard/summary"), mockSummary);
  },
  getForecast() {
    return withFallback(() => request("/forecast/traffic"), mockForecast);
  },
  getSlots() {
    return withFallback(() => request("/slots"), createMockSlots);
  },
  getOrders() {
    return withFallback(() => request("/orders"), createMockOrders);
  },
  getAdminOrders(params = {}) {
    return withFallback(() => request(withQuery("/admin/orders", params)), () => filterAdminOrdersFallback(params));
  },
  getAlerts(params = {}) {
    return withFallback(() => request(withQuery("/admin/alerts", params)), () => filterAlertsFallback(params));
  },
  getAdminAlertDetail(alertNo) {
    return request(`/admin/alerts/${alertNo}/detail`);
  },
  getPricingRules() {
    return withFallback(() => request("/admin/pricing-rules"), mockPricingRules);
  },
  createPricingRule(body) {
    return request("/admin/pricing-rules", { method: "POST", body: JSON.stringify(body || {}) });
  },
  updatePricingRule(id, body) {
    return request(`/admin/pricing-rules/${encodeURIComponent(id)}`, { method: "PUT", body: JSON.stringify(body || {}) });
  },
  deletePricingRule(id) {
    return request(`/admin/pricing-rules/${encodeURIComponent(id)}`, { method: "DELETE" });
  },
  createPricingRule(body) {
    return request("/admin/pricing-rules", { method: "POST", body: JSON.stringify(body || {}) });
  },
  updatePricingRule(id, body) {
    return request(`/admin/pricing-rules/${encodeURIComponent(id)}`, { method: "PUT", body: JSON.stringify(body || {}) });
  },
  deletePricingRule(id) {
    return request(`/admin/pricing-rules/${encodeURIComponent(id)}`, { method: "DELETE" });
  },
  getAccessList() {
    return withFallback(() => request("/admin/access-list"), mockAccessList);
  },
  getCustomerVehicles(params = {}) {
    return withFallback(() => request(withQuery("/admin/customer-vehicles", params)), () => filterCustomerVehiclesFallback(params));
  },
  getAdminCustomerDetail(ownerId) {
    return request(`/admin/customers/${ownerId}/detail`);
  },
  getPayments(params = {}) {
    return withFallback(() => request(withQuery("/admin/payments", params)), () => filterPaymentsFallback(params));
  },
  getBillingComponents(orderNo) {
    return withFallback(() => request(`/admin/orders/${orderNo}/billing-components`), () => buildMockBillingComponents(orderNo));
  },
  getAdminOrderDetail(orderNo) {
    return request(`/admin/orders/${orderNo}/detail`);
  },
  getSystemNodes() {
    return withFallback(() => request("/system/nodes"), mockSystemNodes);
  },
  getQueue() {
    return withFallback(() => request("/dispatch/queue"), mockQueue);
  },
  getAgvs() {
    return withFallback(() => request("/dispatch/agvs"), mockAgvs);
  },
  getDevicesOverview() {
    return withFallback(() => request("/devices/overview"), mockDeviceOverview);
  },
  getPricingPreview(orderNo) {
    return withFallback(() => request(withQuery("/pricing/preview", { orderNo })), () => buildMockPricingPreview());
  },
  getIndoorRoute(orderNo) {
    return withFallback(() => request(withQuery("/navigation/indoor", { orderNo })), () => buildMockIndoorRoute());
  },
  getReport(query) {
    return withFallback(
      () =>
        request("/admin/report", {
          method: "POST",
          body: JSON.stringify({ query }),
        }),
      () => buildMockReport(query),
    );
  },
  simulateEntry(plateNo) {
    return request(withQuery("/orders/entry", { plateNo }), { method: "POST" });
  },
  retrieveOrder(orderNo) {
    return request(`/orders/${orderNo}/retrieve`, { method: "POST" });
  },
  touchOrder(orderNo) {
    return request(`/orders/${orderNo}/touch-and-go`, { method: "POST" });
  },
  payOrder(orderNo) {
    return request(`/orders/${orderNo}/pay`, { method: "POST" });
  },
  triggerPreDispatch() {
    return request("/dispatch/pre-dispatch", { method: "POST" });
  },
  triggerVip(orderNo) {
    return request(withQuery("/dispatch/vip", { orderNo }), { method: "POST" });
  },
  inferVision(body) {
    return withFallback(
      () =>
        request("/edge/vision/infer", {
          method: "POST",
          body: JSON.stringify(body || {}),
        }),
      mockVisionResult,
    );
  },
  gateVision(body) {
    return request("/edge/vision/gate-entry", {
      method: "POST",
      body: JSON.stringify(body || {}),
    });
  },
  getRecognitions(params = {}) {
    return request(withQuery("/admin/recognitions", params));
  },
  setEmergency(active) {
    return request(withQuery("/devices/emergency", { active }), { method: "POST" });
  },
  setDeviceStatus(type, id, status) {
    return request(withQuery(`/admin/devices/${type}/${encodeURIComponent(id)}/status`, { status }), { method: "POST" });
  },
  getAuditLogs(limit = 100) {
    return request(withQuery("/admin/audit-logs", { limit }));
  },
};
