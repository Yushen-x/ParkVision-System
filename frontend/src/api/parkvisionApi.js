import {
  buildMockReport,
  createMockAdminOrders,
  createMockOrders,
  createMockSlots,
  mockAccessList,
  mockAgvs,
  mockAlerts,
  mockForecast,
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

export const parkvisionApi = {
  probeBackend() {
    return request("/dashboard/summary");
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
  getAdminOrders() {
    return withFallback(() => request("/admin/orders"), createMockAdminOrders);
  },
  getAlerts() {
    return withFallback(() => request("/admin/alerts"), mockAlerts);
  },
  getPricingRules() {
    return withFallback(() => request("/admin/pricing-rules"), mockPricingRules);
  },
  getAccessList() {
    return withFallback(() => request("/admin/access-list"), mockAccessList);
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
};
