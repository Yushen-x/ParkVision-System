import { mkdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "../..");
const outputDir = path.join(projectRoot, "docs", "testing", "assets", "screenshots");
const frontendUrl = process.env.PARKVISION_FRONTEND_URL || "http://localhost:5173";
const backendUrl = process.env.PARKVISION_BASE_URL || "http://localhost:8080";

async function ensureDir() {
  await mkdir(outputDir, { recursive: true });
}

async function api(endpoint, options = {}) {
  return fetch(`${backendUrl}${endpoint}`, {
    method: options.method || "GET",
    headers: { "Content-Type": "application/json" },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
}

async function prepareData() {
  await api("/api/devices/emergency?active=false", { method: "POST" });
  const entry = await api("/api/orders/entry", { method: "POST" }).then((res) => res.json());
  await api("/api/dispatch/pre-dispatch", { method: "POST" });
  await api(`/api/dispatch/vip?orderNo=${encodeURIComponent(entry.data.orderNo)}`, { method: "POST" });
  await api("/api/edge/vision/infer", {
    method: "POST",
    body: { cameraId: "CAM-SOUTH-01", simulateIntrusion: false },
  });
}

async function openRoute(page, route, readySelector) {
  await page.goto(`${frontendUrl}${route}`, { waitUntil: "domcontentloaded", timeout: 20000 });
  await page.locator(readySelector).first().waitFor({ state: "visible", timeout: 12000 });
  await page.waitForTimeout(900);
}

async function waitForCount(page, selector, minCount, label) {
  await page.waitForFunction(
    ({ selector: targetSelector, minCount: targetMinCount }) =>
      document.querySelectorAll(targetSelector).length >= targetMinCount,
    { selector, minCount },
    { timeout: 15000 },
  );
  console.log(`${label}: ready`);
}

async function capture(page, filename, options = {}) {
  console.log(`Capturing ${filename}`);
  await page.screenshot({
    path: path.join(outputDir, filename),
    fullPage: options.fullPage ?? false,
    animations: "disabled",
  });
}

async function main() {
  await ensureDir();
  await prepareData();

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1100 }, deviceScaleFactor: 1 });
  page.setDefaultTimeout(12000);

  await page.addStyleTag({
    content: `
      html { scroll-behavior: auto !important; }
      * { caret-color: transparent !important; }
    `,
  });

  await openRoute(page, "/", "text=车流预测");
  await waitForCount(page, ".kpi-grid > *", 4, "Dashboard KPI cards");
  await capture(page, "dashboard.png");

  await openRoute(page, "/twin", "text=AGV 车队");
  await capture(page, "twin.png");

  await openRoute(page, "/ai", "text=实时边缘识别");
  await page.getByRole("button", { name: /采集画面/ }).click();
  await page.getByRole("button", { name: "采集画面" }).waitFor({ state: "visible", timeout: 15000 });
  await page.locator(".json-output").waitFor({ state: "visible", timeout: 15000 });
  await waitForCount(page, ".ai-kpi", 4, "AI KPI cards");
  await capture(page, "ai-vision.png");

  await openRoute(page, "/dispatch", "text=实时调度队列");
  await waitForCount(page, ".queue-item", 4, "Dispatch queue items");
  await capture(page, "dispatch.png");

  await openRoute(page, "/admin", "text=AI 运营报表");
  await page.getByRole("button", { name: /生成报表/ }).click();
  await page.locator(".report-output").waitFor({ state: "visible", timeout: 15000 });
  await page.getByRole("button", { name: "生成报表" }).waitFor({ state: "visible", timeout: 15000 });
  await capture(page, "admin-report.png");
  await page.getByRole("button", { name: /^订单台账$/ }).click();
  await page.waitForTimeout(1200);
  await capture(page, "admin-orders.png");

  await openRoute(page, "/pricing", "text=动态计费引擎");
  await capture(page, "pricing.png");

  await openRoute(page, "/system", "text=准入与信任名单");
  await capture(page, "system.png");

  await openRoute(page, "/owner", "text=我的车辆");
  await capture(page, "owner.png");

  await openRoute(page, "/indoor-map", "text=室内交接导航");
  await capture(page, "indoor-map.png");

  await api("/api/edge/vision/infer", {
    method: "POST",
    body: { cameraId: "CAM-HANDOFF-02", simulateIntrusion: true },
  });

  await openRoute(page, "/twin", "text=紧急停车");
  await page.locator("text=安全停机生效").waitFor({ state: "visible", timeout: 15000 });
  await capture(page, "twin-emergency.png");

  await api("/api/devices/emergency?active=false", { method: "POST" });
  await browser.close();

  console.log(`Saved screenshots to ${outputDir}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
