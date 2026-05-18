import { beforeEach, describe, expect, it } from "vitest";
import {
  addEvent,
  simulateEntry,
  state,
  toggleEmergency,
} from "./parkingStore";
import {
  buildMockIndoorRoute as createIndoorRoute,
  buildMockPricingPreview as createPricingPreview,
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
} from "../data/mockData";

function resetState() {
  state.onlineMode = "Fallback mode";
  state.emergency = false;
  state.loading = false;
  state.activePlate = mockVisionResult.plate;
  state.summary = { ...mockSummary };
  state.forecast = structuredClone(mockForecast);
  state.events = [
    ["系统上线", "运营首页和本地兜底数据源已初始化。"],
    ["视觉边缘节点", "最新车牌 OCR 结果为 SH-A7686，置信度 0.982。"],
    ["调度中心", "AGV-03 正在前往浅层缓冲车道。"],
  ];
  state.slots = createMockSlots();
  state.agvs = structuredClone(mockAgvs);
  state.orders = createMockOrders();
  state.adminOrders = createMockAdminOrders();
  state.alerts = structuredClone(mockAlerts);
  state.pricingRules = structuredClone(mockPricingRules);
  state.accessList = structuredClone(mockAccessList);
  state.systemNodes = structuredClone(mockSystemNodes);
  state.queue = structuredClone(mockQueue);
  state.devices = structuredClone(mockDeviceOverview);
  state.pricingPreview = createPricingPreview();
  state.indoorRoute = createIndoorRoute();
  state.ownerTimeline = [
    ["车辆已入库", "AGV 已将车辆放入 E06 车位。"],
    ["计费已启动", "入场流程结束后，动态停车费开始计算。"],
    ["车主服务就绪", "取车、临停取物和 VIP 优先取车均可使用。"],
  ];
  state.visionResult = { ...mockVisionResult };
  state.adminReport = buildMockReport();
}

describe("parkingStore", () => {
  beforeEach(() => {
    resetState();
  });

  it("falls back to a local order when entry API is unavailable", async () => {
    const orderCount = state.orders.length;

    await simulateEntry();

    expect(state.orders).toHaveLength(orderCount + 1);
    expect(state.orders[0].status).toBe("PARKED");
    expect(state.slots.some((slot) => slot.available === false && slot.id === state.orders[0].slotId)).toBe(true);
  });

  it("toggles emergency state and appends a new event", async () => {
    const eventCount = state.events.length;

    await toggleEmergency();
    addEvent("人工备注", "操作员已确认急停状态。");

    expect(state.emergency).toBe(true);
    expect(state.visionResult.action).toBe("ESTOP_AND_REVIEW");
    expect(state.events.length).toBe(eventCount + 2);
  });
});
