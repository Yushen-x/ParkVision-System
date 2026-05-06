import { mockSummary, mockOrders, mockAlerts, mockForecast } from "../data/mockData";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });

  if (!response.ok) {
    throw new Error(`API ${path} failed with ${response.status}`);
  }

  const payload = await response.json();
  return payload.data ?? payload;
}

async function withFallback(fetcher, fallback) {
  try {
    return await fetcher();
  } catch {
    return fallback;
  }
}

export const parkvisionApi = {
  getSummary() {
    return withFallback(() => request("/dashboard/summary"), mockSummary);
  },
  getOrders() {
    return withFallback(() => request("/admin/orders"), mockOrders);
  },
  getAlerts() {
    return withFallback(() => request("/admin/alerts"), mockAlerts);
  },
  getForecast() {
    return withFallback(() => request("/forecast/traffic"), mockForecast);
  },
  simulateEntry() {
    return request("/orders/entry", { method: "POST" });
  },
  triggerPreDispatch() {
    return request("/dispatch/pre-dispatch", { method: "POST" });
  },
  triggerVip() {
    return request("/dispatch/vip", { method: "POST" });
  },
  inferVision(body) {
    return request("/edge/vision/infer", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },
};
