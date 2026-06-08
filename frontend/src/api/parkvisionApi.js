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

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });

  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(payload?.message || `API ${path} failed with ${response.status}`);
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
  const url = new URL(`${API_BASE}${path}`, window.location.origin);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      url.searchParams.set(key, value);
    }
  });
  return url.pathname + url.search;
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
  probeBackend() {
    return request("/dashboard/summary");
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
  simulateEntry() {
    return request("/orders/entry", { method: "POST" });
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
  setEmergency(active) {
    return request(withQuery("/devices/emergency", { active }), { method: "POST" });
  },
};
