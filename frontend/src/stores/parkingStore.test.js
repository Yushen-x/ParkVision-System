import { beforeEach, describe, expect, it } from "vitest";
import {
  addEvent,
  simulateEntry,
  state,
  toggleEmergency,
} from "./parkingStore";
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

function resetState() {
  state.onlineMode = "Fallback mode";
  state.emergency = false;
  state.loading = false;
  state.activePlate = mockVisionResult.plate;
  state.summary = { ...mockSummary };
  state.forecast = structuredClone(mockForecast);
  state.events = [
    ["System online", "Dashboard and fallback data source were initialized."],
    ["Vision edge node", "Latest plate OCR result is SH-A7686 with 0.982 confidence."],
    ["Dispatch center", "AGV-03 is heading to the shallow buffer lane."],
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
  state.ownerTimeline = [
    ["Vehicle stored", "AGV placed the vehicle in slot E06."],
    ["Billing active", "Dynamic parking fee started after the entry workflow closed."],
    ["Owner ready", "Retrieve, touch-and-go, and VIP pickup are available."],
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

  it("toggles emergency state and appends a new event", () => {
    const eventCount = state.events.length;

    toggleEmergency();
    addEvent("Manual note", "Operator confirmed the stop state.");

    expect(state.emergency).toBe(true);
    expect(state.visionResult.action).toBe("ESTOP_AND_REVIEW");
    expect(state.events.length).toBe(eventCount + 2);
  });
});
