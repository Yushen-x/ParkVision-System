import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { performance } from "node:perf_hooks";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "../..");
const dataDir = path.join(projectRoot, "docs", "testing", "assets", "data");
const figuresDir = path.join(projectRoot, "docs", "testing", "assets", "figures");
const baseUrl = process.env.PARKVISION_BASE_URL || "http://localhost:8080";

const scenarios = [
  { name: "运营首页汇总", method: "GET", path: "/api/dashboard/summary", requests: 60, concurrency: 10 },
  { name: "调度队列", method: "GET", path: "/api/dispatch/queue", requests: 60, concurrency: 10 },
  { name: "设备总览", method: "GET", path: "/api/devices/overview", requests: 60, concurrency: 10 },
  { name: "计费预览", method: "GET", path: "/api/pricing/preview", requests: 60, concurrency: 10 },
  {
    name: "管理报表",
    method: "POST",
    path: "/api/admin/report",
    body: { query: "汇总本周 VIP 取车对收入的影响。" },
    requests: 30,
    concurrency: 5,
  },
  {
    name: "视觉推理",
    method: "POST",
    path: "/api/edge/vision/infer",
    body: { cameraId: "CAM-SOUTH-01", simulateIntrusion: false },
    requests: 30,
    concurrency: 3,
  },
];

async function ensureDirs() {
  await mkdir(dataDir, { recursive: true });
  await mkdir(figuresDir, { recursive: true });
}

async function timedRequest(scenario) {
  const started = performance.now();
  const response = await fetch(`${baseUrl}${scenario.path}`, {
    method: scenario.method,
    headers: { "Content-Type": "application/json" },
    body: scenario.body === undefined ? undefined : JSON.stringify(scenario.body),
  });
  await response.text();
  return {
    ok: response.ok,
    status: response.status,
    durationMs: Number((performance.now() - started).toFixed(2)),
  };
}

function percentile(sortedValues, ratio) {
  if (!sortedValues.length) return 0;
  const index = Math.min(sortedValues.length - 1, Math.ceil(sortedValues.length * ratio) - 1);
  return sortedValues[index];
}

async function runScenario(scenario) {
  let cursor = 0;
  const measurements = [];

  async function worker() {
    while (cursor < scenario.requests) {
      cursor += 1;
      measurements.push(await timedRequest(scenario));
    }
  }

  await Promise.all(Array.from({ length: scenario.concurrency }, () => worker()));

  const durations = measurements.map((item) => item.durationMs).sort((left, right) => left - right);
  const failures = measurements.filter((item) => !item.ok);
  const sum = durations.reduce((total, value) => total + value, 0);

  return {
    ...scenario,
    minMs: Number((durations[0] || 0).toFixed(2)),
    avgMs: Number((sum / Math.max(1, durations.length)).toFixed(2)),
    p95Ms: Number(percentile(durations, 0.95).toFixed(2)),
    maxMs: Number((durations[durations.length - 1] || 0).toFixed(2)),
    successRate: Number((((measurements.length - failures.length) / Math.max(1, measurements.length)) * 100).toFixed(2)),
    sampleCount: measurements.length,
    failures: failures.length,
  };
}

function escapeXml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;");
}

function buildSvg(results) {
  const width = 1280;
  const rowHeight = 82;
  const height = 150 + results.length * rowHeight;
  const maxMetric = Math.max(...results.flatMap((item) => [item.avgMs, item.p95Ms]), 1);
  const chartLeft = 280;
  const chartWidth = 860;

  const rows = results
    .map((result, index) => {
      const y = 90 + index * rowHeight;
      const avgWidth = (result.avgMs / maxMetric) * chartWidth;
      const p95Width = (result.p95Ms / maxMetric) * chartWidth;

      return `
        <text x="40" y="${y + 18}" font-size="24" fill="#e2e8f0" font-family="Microsoft YaHei, Segoe UI">${escapeXml(result.name)}</text>
        <text x="40" y="${y + 48}" font-size="15" fill="#94a3b8" font-family="Microsoft YaHei, Segoe UI">平均 ${result.avgMs} ms | P95 ${result.p95Ms} ms | 最大 ${result.maxMs} ms</text>
        <rect x="${chartLeft}" y="${y}" width="${chartWidth}" height="20" rx="10" fill="rgba(255,255,255,0.06)" />
        <rect x="${chartLeft}" y="${y}" width="${avgWidth}" height="20" rx="10" fill="#38bdf8" />
        <rect x="${chartLeft}" y="${y + 30}" width="${chartWidth}" height="12" rx="6" fill="rgba(255,255,255,0.06)" />
        <rect x="${chartLeft}" y="${y + 30}" width="${p95Width}" height="12" rx="6" fill="#f59e0b" />
        <text x="${chartLeft + avgWidth + 12}" y="${y + 16}" font-size="14" fill="#bae6fd" font-family="Segoe UI">${result.avgMs} ms</text>
        <text x="${chartLeft + p95Width + 12}" y="${y + 41}" font-size="14" fill="#fde68a" font-family="Segoe UI">${result.p95Ms} ms</text>
      `;
    })
    .join("\n");

  return `<?xml version="1.0" encoding="UTF-8"?>
<svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
      <stop offset="0%" stop-color="#0f172a" />
      <stop offset="100%" stop-color="#1e293b" />
    </linearGradient>
  </defs>
  <rect width="${width}" height="${height}" rx="28" fill="url(#bg)" />
  <text x="40" y="48" font-size="34" font-weight="700" fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">ParkVision API 性能测试结果</text>
  <text x="40" y="78" font-size="18" fill="#94a3b8" font-family="Microsoft YaHei, Segoe UI">蓝色为平均响应时间，黄色为 P95。测试基于本机预览环境与 H2 文件数据库。</text>
  ${rows}
  <rect x="40" y="${height - 38}" width="16" height="16" rx="8" fill="#38bdf8" />
  <text x="64" y="${height - 24}" font-size="14" fill="#cbd5e1" font-family="Microsoft YaHei, Segoe UI">平均响应时间</text>
  <rect x="220" y="${height - 38}" width="16" height="16" rx="8" fill="#f59e0b" />
  <text x="244" y="${height - 24}" font-size="14" fill="#cbd5e1" font-family="Microsoft YaHei, Segoe UI">P95 响应时间</text>
</svg>
`;
}

async function main() {
  await ensureDirs();

  const results = [];
  for (const scenario of scenarios) {
    results.push(await runScenario(scenario));
  }

  const payload = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    results,
  };

  await writeFile(path.join(dataDir, "performance-summary.json"), `${JSON.stringify(payload, null, 2)}\n`, "utf8");
  await writeFile(path.join(figuresDir, "performance-benchmark.svg"), buildSvg(results), "utf8");

  console.log(`Saved performance summary to ${path.join(dataDir, "performance-summary.json")}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
