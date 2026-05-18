import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "../..");
const outputDir = path.join(projectRoot, "docs", "testing", "assets", "data");
const baseUrl = process.env.PARKVISION_BASE_URL || "http://localhost:8080";

async function ensureDir(dir) {
  await mkdir(dir, { recursive: true });
}

async function request(endpoint, options = {}) {
  const response = await fetch(`${baseUrl}${endpoint}`, {
    method: options.method || "GET",
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  const text = await response.text();
  let json = null;

  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = null;
  }

  return {
    ok: response.ok,
    status: response.status,
    headers: Object.fromEntries(response.headers.entries()),
    json,
    text,
  };
}

function unwrap(result) {
  return result?.json?.data ?? null;
}

function expectResult({ id, title, category, expected, actual, pass, evidence }) {
  return { id, title, category, expected, actual, pass, evidence };
}

function findOrderByNo(orders, orderNo) {
  return orders.find((order) => order.orderNo === orderNo) || null;
}

function findSlotById(slots, slotId) {
  return slots.find((slot) => slot.id === slotId) || null;
}

async function snapshotCore() {
  const [health, summary, forecast, slots, orders, queue, agvs, devices, systemNodes, pricingPreview, indoorRoute, adminOrders, alerts, pricingRules, accessList] =
    await Promise.all([
      request("/actuator/health"),
      request("/api/dashboard/summary"),
      request("/api/forecast/traffic"),
      request("/api/slots"),
      request("/api/orders"),
      request("/api/dispatch/queue"),
      request("/api/dispatch/agvs"),
      request("/api/devices/overview"),
      request("/api/system/nodes"),
      request("/api/pricing/preview"),
      request("/api/navigation/indoor"),
      request("/api/admin/orders"),
      request("/api/admin/alerts"),
      request("/api/admin/pricing-rules"),
      request("/api/admin/access-list"),
    ]);

  return {
    health: health.json || health.text,
    summary: unwrap(summary),
    forecast: unwrap(forecast),
    slots: unwrap(slots),
    orders: unwrap(orders),
    queue: unwrap(queue),
    agvs: unwrap(agvs),
    devices: unwrap(devices),
    systemNodes: unwrap(systemNodes),
    pricingPreview: unwrap(pricingPreview),
    indoorRoute: unwrap(indoorRoute),
    adminOrders: unwrap(adminOrders),
    alerts: unwrap(alerts),
    pricingRules: unwrap(pricingRules),
    accessList: unwrap(accessList),
  };
}

async function main() {
  await ensureDir(outputDir);

  const startedAt = new Date().toISOString();
  const baseline = await snapshotCore();
  const functional = [];
  const negative = [];
  const evidence = {};

  functional.push(
    expectResult({
      id: "F-00",
      title: "服务健康检查",
      category: "functional",
      expected: "后端健康状态返回 UP，核心业务接口可访问",
      actual: baseline.health?.status || "UNKNOWN",
      pass: baseline.health?.status === "UP" && Array.isArray(baseline.orders),
      evidence: {
        health: baseline.health,
        orderCount: baseline.orders?.length || 0,
      },
    }),
  );

  const entryBeforeCount = baseline.orders.length;
  const entryBeforeEmptySlots = baseline.slots.filter((slot) => slot.status === "empty").length;
  const entryResponse = await request("/api/orders/entry", { method: "POST" });
  const ordersAfterEntry = unwrap(await request("/api/orders"));
  const slotsAfterEntry = unwrap(await request("/api/slots"));
  const createdEntry = unwrap(entryResponse);
  evidence.createdEntry = createdEntry;

  functional.push(
    expectResult({
      id: "F-01",
      title: "模拟入场创建订单并占用车位",
      category: "functional",
      expected: "订单数 +1，新增订单状态为 PARKED，对应车位不再为空",
      actual: {
        orderCountBefore: entryBeforeCount,
        orderCountAfter: ordersAfterEntry.length,
        createdOrder: createdEntry?.orderNo,
        createdStatus: createdEntry?.status,
        slotStatus: findSlotById(slotsAfterEntry, createdEntry?.slotId)?.status,
        emptySlotsBefore: entryBeforeEmptySlots,
        emptySlotsAfter: slotsAfterEntry.filter((slot) => slot.status === "empty").length,
      },
      pass:
        entryResponse.ok &&
        ordersAfterEntry.length === entryBeforeCount + 1 &&
        createdEntry?.status === "PARKED" &&
        findSlotById(slotsAfterEntry, createdEntry?.slotId)?.status !== "empty",
      evidence: createdEntry,
    }),
  );

  const queueBeforePreDispatch = unwrap(await request("/api/dispatch/queue")).length;
  const preDispatchResponse = await request("/api/dispatch/pre-dispatch", { method: "POST" });
  const queueAfterPreDispatch = unwrap(await request("/api/dispatch/queue"));
  evidence.preDispatchTask = unwrap(preDispatchResponse);

  functional.push(
    expectResult({
      id: "F-02",
      title: "预调度插入队列",
      category: "functional",
      expected: "调度队列头部新增高峰预调度移位任务",
      actual: {
        queueBefore: queueBeforePreDispatch,
        queueAfter: queueAfterPreDispatch.length,
        headTask: queueAfterPreDispatch[0],
      },
      pass:
        preDispatchResponse.ok &&
        queueAfterPreDispatch.length === queueBeforePreDispatch + 1 &&
        ["Pre-dispatch relocation", "高峰预调度移位"].includes(queueAfterPreDispatch[0]?.type),
      evidence: unwrap(preDispatchResponse),
    }),
  );

  const vipResponse = await request(`/api/dispatch/vip?orderNo=${encodeURIComponent(createdEntry.orderNo)}`, {
    method: "POST",
  });
  const ordersAfterVip = unwrap(await request("/api/orders"));
  const queueAfterVip = unwrap(await request("/api/dispatch/queue"));
  const vipOrder = findOrderByNo(ordersAfterVip, createdEntry.orderNo);
  evidence.vipTask = unwrap(vipResponse);

  functional.push(
    expectResult({
      id: "F-03",
      title: "VIP 取车插队",
      category: "functional",
      expected: "指定订单状态切换为 RETRIEVING，VIP 任务进入队列头部",
      actual: {
        vipOrderStatus: vipOrder?.status,
        queueHead: queueAfterVip[0],
      },
      pass:
        vipResponse.ok &&
        vipOrder?.status === "RETRIEVING" &&
        queueAfterVip[0]?.plateNo === createdEntry.plateNo &&
        queueAfterVip[0]?.vip === true,
      evidence: unwrap(vipResponse),
    }),
  );

  const touchEntry = unwrap(await request("/api/orders/entry", { method: "POST" }));
  const touchResponse = await request(`/api/orders/${encodeURIComponent(touchEntry.orderNo)}/touch-and-go`, {
    method: "POST",
  });
  const ordersAfterTouch = unwrap(await request("/api/orders"));
  const slotsAfterTouch = unwrap(await request("/api/slots"));
  const touchedOrder = findOrderByNo(ordersAfterTouch, touchEntry.orderNo);

  functional.push(
    expectResult({
      id: "F-04",
      title: "临时取物 Touch-and-Go",
      category: "functional",
      expected: "订单状态变为 TOUCHING，对应车位切换为 BUFFER",
      actual: {
        orderStatus: touchedOrder?.status,
        slotStatus: findSlotById(slotsAfterTouch, touchEntry.slotId)?.status,
      },
      pass:
        touchResponse.ok &&
        touchedOrder?.status === "TOUCHING" &&
        findSlotById(slotsAfterTouch, touchEntry.slotId)?.status === "buffer",
      evidence: unwrap(touchResponse),
    }),
  );

  const payEntry = unwrap(await request("/api/orders/entry", { method: "POST" }));
  const payResponse = await request(`/api/orders/${encodeURIComponent(payEntry.orderNo)}/pay`, { method: "POST" });
  const ordersAfterPay = unwrap(await request("/api/orders"));
  const slotsAfterPay = unwrap(await request("/api/slots"));
  const paidOrder = findOrderByNo(ordersAfterPay, payEntry.orderNo);

  functional.push(
    expectResult({
      id: "F-05",
      title: "支付完成并关闭订单",
      category: "functional",
      expected: "订单状态变为 FINISHED，车位释放为空位，订单金额被计算",
      actual: {
        orderStatus: paidOrder?.status,
        slotStatus: findSlotById(slotsAfterPay, payEntry.slotId)?.status,
        amount: paidOrder?.amount,
      },
      pass:
        payResponse.ok &&
        paidOrder?.status === "FINISHED" &&
        findSlotById(slotsAfterPay, payEntry.slotId)?.status === "empty" &&
        Number(paidOrder?.amount || 0) > 0,
      evidence: unwrap(payResponse),
    }),
  );

  const visionNormal = await request("/api/edge/vision/infer", {
    method: "POST",
    body: { cameraId: "CAM-SOUTH-01", simulateIntrusion: false },
  });
  const devicesAfterVisionNormal = unwrap(await request("/api/devices/overview"));
  const normalCamera = devicesAfterVisionNormal.cameras.find((camera) => camera.cameraId === "CAM-SOUTH-01");

  functional.push(
    expectResult({
      id: "F-06",
      title: "AI 识别正常流程",
      category: "functional",
      expected: "返回 OCR 结果，摄像头状态更新，系统不进入急停",
      actual: {
        inference: unwrap(visionNormal),
        cameraIntrusion: normalCamera?.intrusionState,
      },
      pass: visionNormal.ok && unwrap(visionNormal)?.intrusion === false && normalCamera?.intrusionState === false,
      evidence: unwrap(visionNormal),
    }),
  );

  const visionIntrusion = await request("/api/edge/vision/infer", {
    method: "POST",
    body: { cameraId: "CAM-HANDOFF-02", simulateIntrusion: true },
  });
  const devicesAfterIntrusion = unwrap(await request("/api/devices/overview"));
  const nodesAfterIntrusion = unwrap(await request("/api/system/nodes"));
  const emergencyLatched = devicesAfterIntrusion.gates.some((gate) => gate.estopArmed);

  functional.push(
    expectResult({
      id: "F-07",
      title: "AI 入侵触发急停",
      category: "functional",
      expected: "入侵识别后，摄像头 ROI 置为风险态，闸机急停位拉起，系统节点进入 warning",
      actual: {
        inference: unwrap(visionIntrusion),
        estopArmed: emergencyLatched,
        warningNodes: nodesAfterIntrusion.filter((node) => node.level === "warning").map((node) => node.name),
      },
      pass:
        visionIntrusion.ok &&
        unwrap(visionIntrusion)?.intrusion === true &&
        emergencyLatched &&
        nodesAfterIntrusion.some((node) => node.level === "warning"),
      evidence: unwrap(visionIntrusion),
    }),
  );

  const clearEmergency = await request("/api/devices/emergency?active=false", { method: "POST" });
  const devicesAfterClear = unwrap(await request("/api/devices/overview"));
  functional.push(
    expectResult({
      id: "F-08",
      title: "人工解除急停",
      category: "functional",
      expected: "解除急停后，闸机急停位恢复，系统回到自动模式",
      actual: {
        response: unwrap(clearEmergency),
        estopArmed: devicesAfterClear.gates.some((gate) => gate.estopArmed),
      },
      pass: clearEmergency.ok && devicesAfterClear.gates.every((gate) => gate.estopArmed === false),
      evidence: unwrap(clearEmergency),
    }),
  );

  const pricingPreview = await request("/api/pricing/preview");
  functional.push(
    expectResult({
      id: "F-09",
      title: "动态计费预览",
      category: "functional",
      expected: "返回当前订单、时长、组成项和总金额",
      actual: unwrap(pricingPreview),
      pass:
        pricingPreview.ok &&
        Array.isArray(unwrap(pricingPreview)?.components) &&
        unwrap(pricingPreview)?.components.length >= 4 &&
        Number(unwrap(pricingPreview)?.totalAmount || 0) >= 0,
      evidence: unwrap(pricingPreview),
    }),
  );

  const indoorRoute = await request("/api/navigation/indoor");
  functional.push(
    expectResult({
      id: "F-10",
      title: "室内接驳导航",
      category: "functional",
      expected: "返回目标交接区、剩余距离、车主 ETA、AGV ETA 与安全提示",
      actual: unwrap(indoorRoute),
      pass:
        indoorRoute.ok &&
        typeof unwrap(indoorRoute)?.remainingMeters === "number" &&
        typeof unwrap(indoorRoute)?.etaSeconds === "number" &&
        Boolean(unwrap(indoorRoute)?.targetGate),
      evidence: unwrap(indoorRoute),
    }),
  );

  const adminReport = await request("/api/admin/report", {
    method: "POST",
    body: { query: "对比最近 7 天 VIP 取车与标准取车收入。" },
  });
  functional.push(
    expectResult({
      id: "F-11",
      title: "管理台 AI 报表",
      category: "functional",
      expected: "返回查询摘要和周收入对比曲线数据",
      actual: {
        query: unwrap(adminReport)?.query,
        labelCount: unwrap(adminReport)?.labels?.length || 0,
        summary: unwrap(adminReport)?.summary,
      },
      pass:
        adminReport.ok &&
        Array.isArray(unwrap(adminReport)?.labels) &&
        unwrap(adminReport)?.labels.length > 0 &&
        Array.isArray(unwrap(adminReport)?.currentWeekRevenue),
      evidence: unwrap(adminReport),
    }),
  );

  const invalidRetrieve = await request("/api/orders/ORDER-404/retrieve", { method: "POST" });
  negative.push(
    expectResult({
      id: "B-01",
      title: "非法订单号取车",
      category: "blackbox",
      expected: "返回 400 和 ORDER_NOT_FOUND",
      actual: { status: invalidRetrieve.status, body: invalidRetrieve.json },
      pass: invalidRetrieve.status === 400 && invalidRetrieve.json?.code === "ORDER_NOT_FOUND",
      evidence: invalidRetrieve.json,
    }),
  );

  const invalidVip = await request("/api/dispatch/vip?orderNo=ORDER-404", { method: "POST" });
  negative.push(
    expectResult({
      id: "B-02",
      title: "非法订单号 VIP 插队",
      category: "blackbox",
      expected: "返回 400 和 ORDER_NOT_FOUND",
      actual: { status: invalidVip.status, body: invalidVip.json },
      pass: invalidVip.status === 400 && invalidVip.json?.code === "ORDER_NOT_FOUND",
      evidence: invalidVip.json,
    }),
  );

  const invalidPricing = await request("/api/pricing/preview?orderNo=ORDER-404");
  negative.push(
    expectResult({
      id: "B-03",
      title: "非法订单号计费预览",
      category: "blackbox",
      expected: "返回 400 和 ORDER_NOT_FOUND",
      actual: { status: invalidPricing.status, body: invalidPricing.json },
      pass: invalidPricing.status === 400 && invalidPricing.json?.code === "ORDER_NOT_FOUND",
      evidence: invalidPricing.json,
    }),
  );

  const invalidIndoor = await request("/api/navigation/indoor?orderNo=ORDER-404");
  negative.push(
    expectResult({
      id: "B-04",
      title: "非法订单号室内导航",
      category: "blackbox",
      expected: "返回 400 和 ORDER_NOT_FOUND",
      actual: { status: invalidIndoor.status, body: invalidIndoor.json },
      pass: invalidIndoor.status === 400 && invalidIndoor.json?.code === "ORDER_NOT_FOUND",
      evidence: invalidIndoor.json,
    }),
  );

  const invalidEmergency = await request("/api/devices/emergency", { method: "POST" });
  negative.push(
    expectResult({
      id: "B-05",
      title: "缺失必要参数的急停请求",
      category: "blackbox",
      expected: "返回 400，接口拒绝不完整请求",
      actual: { status: invalidEmergency.status, body: invalidEmergency.json || invalidEmergency.text },
      pass: invalidEmergency.status === 400,
      evidence: invalidEmergency.json || invalidEmergency.text,
    }),
  );

  const finalSnapshot = await snapshotCore();

  const payload = {
    generatedAt: startedAt,
    finishedAt: new Date().toISOString(),
    environment: {
      baseUrl,
      frontendUrl: "http://localhost:5173",
      persistence: "H2 文件数据库",
      deviceMode: "后端模拟现场设备",
    },
    dataset: {
      slotCount: finalSnapshot.slots.length,
      orderCount: finalSnapshot.orders.length,
      queueCount: finalSnapshot.queue.length,
      agvCount: finalSnapshot.agvs.length,
      alertCount: finalSnapshot.alerts.length,
      accessListCount: finalSnapshot.accessList.length,
      pricingRuleCount: finalSnapshot.pricingRules.length,
      deviceEventCount: finalSnapshot.devices.events.length,
      cameraCount: finalSnapshot.devices.cameras.length,
      gateCount: finalSnapshot.devices.gates.length,
      chargerCount: finalSnapshot.devices.chargers.length,
    },
    baseline,
    finalSnapshot,
    evidence,
    functional,
    negative,
    summary: {
      functionalPassed: functional.filter((item) => item.pass).length,
      functionalTotal: functional.length,
      negativePassed: negative.filter((item) => item.pass).length,
      negativeTotal: negative.length,
      overallPass: [...functional, ...negative].every((item) => item.pass),
    },
  };

  await writeFile(
    path.join(outputDir, "system-test-results.json"),
    `${JSON.stringify(payload, null, 2)}\n`,
    "utf8",
  );

  console.log(`Collected system test data at ${path.join(outputDir, "system-test-results.json")}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
