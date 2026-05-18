import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "../..");
const dataDir = path.join(projectRoot, "docs", "testing", "assets", "data");
const figuresDir = path.join(projectRoot, "docs", "testing", "assets", "figures");

function escapeXml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;");
}

function backgroundSvg(width, height, title, subtitle, body) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#0f172a" />
      <stop offset="100%" stop-color="#1e293b" />
    </linearGradient>
  </defs>
  <rect width="${width}" height="${height}" rx="28" fill="url(#bg)" />
  <text x="40" y="52" font-size="34" font-weight="700" fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">${escapeXml(title)}</text>
  <text x="40" y="82" font-size="18" fill="#94a3b8" font-family="Microsoft YaHei, Segoe UI">${escapeXml(subtitle)}</text>
  ${body}
</svg>
`;
}

function card(x, y, width, height, title, value, detail, accent = "#38bdf8") {
  return `
    <rect x="${x}" y="${y}" width="${width}" height="${height}" rx="20" fill="rgba(15,23,42,0.72)" stroke="rgba(255,255,255,0.08)" />
    <rect x="${x + 18}" y="${y + 20}" width="8" height="${height - 40}" rx="4" fill="${accent}" />
    <text x="${x + 42}" y="${y + 42}" font-size="22" fill="#cbd5e1" font-family="Microsoft YaHei, Segoe UI">${escapeXml(title)}</text>
    <text x="${x + 42}" y="${y + 86}" font-size="42" font-weight="700" fill="#ffffff" font-family="Segoe UI">${escapeXml(value)}</text>
    <text x="${x + 42}" y="${y + 120}" font-size="16" fill="#94a3b8" font-family="Microsoft YaHei, Segoe UI">${escapeXml(detail)}</text>
  `;
}

function processFigure() {
  const stepWidth = 210;
  const gap = 30;
  const steps = [
    ["测试计划", "明确范围、对象、功能点、非功能目标"],
    ["环境部署", "启动前后端、数据库、模拟设备并校验健康状态"],
    ["功能验证", "执行入场、调度、取车、计费、导航、报表链路"],
    ["异常验证", "校验非法订单、缺参请求、急停与恢复流程"],
    ["性能评估", "统计平均响应时间、P95、成功率与稳定性"],
  ];

  const body = steps
    .map((step, index) => {
      const x = 40 + index * (stepWidth + gap);
      return `
        <rect x="${x}" y="150" width="${stepWidth}" height="170" rx="22" fill="rgba(15,23,42,0.72)" stroke="rgba(255,255,255,0.08)" />
        <circle cx="${x + 38}" cy="188" r="22" fill="#38bdf8" />
        <text x="${x + 31}" y="196" font-size="22" font-weight="700" fill="#0f172a" font-family="Segoe UI">${index + 1}</text>
        <text x="${x + 72}" y="190" font-size="24" fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">${escapeXml(step[0])}</text>
        <text x="${x + 24}" y="240" font-size="18" fill="#cbd5e1" font-family="Microsoft YaHei, Segoe UI">${escapeXml(step[1])}</text>
        ${index < steps.length - 1 ? `<line x1="${x + stepWidth}" y1="235" x2="${x + stepWidth + gap}" y2="235" stroke="#38bdf8" stroke-width="6" stroke-linecap="round" />` : ""}
      `;
    })
    .join("\n");

  return backgroundSvg(1240, 380, "系统测试执行流程", "本轮测试采用“部署 -> 功能 -> 异常 -> 性能 -> 汇总”的完整闭环。", body);
}

function orderFlowFigure() {
  const boxes = [
    { x: 80, y: 140, w: 220, h: 92, title: "接收订单状态变更", detail: "输入订单号与目标状态" },
    { x: 380, y: 140, w: 220, h: 92, title: "查找订单", detail: "未找到则返回订单不存在" },
    { x: 680, y: 140, w: 220, h: 92, title: "同步车位状态", detail: "取车/临取时切换为周转区" },
    { x: 980, y: 140, w: 220, h: 92, title: "完成订单计算金额", detail: "按时长阶梯 + 充电附加费" },
    { x: 220, y: 320, w: 240, h: 96, title: "取车分支", detail: "插入标准取车队列" },
    { x: 500, y: 320, w: 240, h: 96, title: "临时取物分支", detail: "插入临停取物队列" },
    { x: 780, y: 320, w: 240, h: 96, title: "支付完成分支", detail: "释放车位并记录出口事件" },
  ];

  const boxSvg = boxes
    .map(
      (box) => `
        <rect x="${box.x}" y="${box.y}" width="${box.w}" height="${box.h}" rx="20" fill="rgba(15,23,42,0.72)" stroke="rgba(255,255,255,0.08)" />
        <text x="${box.x + 22}" y="${box.y + 40}" font-size="22" fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">${escapeXml(box.title)}</text>
        <text x="${box.x + 22}" y="${box.y + 70}" font-size="16" fill="#94a3b8" font-family="Microsoft YaHei, Segoe UI">${escapeXml(box.detail)}</text>
      `,
    )
    .join("\n");

  const arrows = `
    <line x1="300" y1="186" x2="380" y2="186" stroke="#38bdf8" stroke-width="6" stroke-linecap="round" />
    <line x1="600" y1="186" x2="680" y2="186" stroke="#38bdf8" stroke-width="6" stroke-linecap="round" />
    <line x1="900" y1="186" x2="980" y2="186" stroke="#38bdf8" stroke-width="6" stroke-linecap="round" />
    <line x1="790" y1="232" x2="790" y2="280" stroke="#38bdf8" stroke-width="6" stroke-linecap="round" />
    <line x1="790" y1="280" x2="340" y2="280" stroke="#38bdf8" stroke-width="4" stroke-linecap="round" />
    <line x1="790" y1="280" x2="620" y2="280" stroke="#38bdf8" stroke-width="4" stroke-linecap="round" />
    <line x1="790" y1="280" x2="900" y2="280" stroke="#38bdf8" stroke-width="4" stroke-linecap="round" />
    <line x1="340" y1="280" x2="340" y2="320" stroke="#38bdf8" stroke-width="4" stroke-linecap="round" />
    <line x1="620" y1="280" x2="620" y2="320" stroke="#38bdf8" stroke-width="4" stroke-linecap="round" />
    <line x1="900" y1="280" x2="900" y2="320" stroke="#38bdf8" stroke-width="4" stroke-linecap="round" />
  `;

  return backgroundSvg(1280, 500, "OrderService.changeStatus 控制流", "白盒测试重点覆盖 3 条业务分支：取车、临时取物、支付完成。", `${boxSvg}${arrows}`);
}

function emergencyFlowFigure() {
  const body = `
    <rect x="70" y="140" width="250" height="100" rx="20" fill="rgba(15,23,42,0.72)" stroke="rgba(255,255,255,0.08)" />
    <text x="92" y="182" font-size="24" fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">接收急停指令</text>
    <text x="92" y="214" font-size="16" fill="#94a3b8" font-family="Microsoft YaHei, Segoe UI">控制台或 AI 入侵事件触发</text>

    <rect x="390" y="140" width="250" height="100" rx="20" fill="rgba(15,23,42,0.72)" stroke="rgba(255,255,255,0.08)" />
    <text x="412" y="182" font-size="24" fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">更新摄像头 ROI 状态</text>
    <text x="412" y="214" font-size="16" fill="#94a3b8" font-family="Microsoft YaHei, Segoe UI">入侵状态随急停同步</text>

    <rect x="710" y="140" width="250" height="100" rx="20" fill="rgba(15,23,42,0.72)" stroke="rgba(255,255,255,0.08)" />
    <text x="732" y="182" font-size="24" fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">更新闸机锁定状态</text>
    <text x="732" y="214" font-size="16" fill="#94a3b8" font-family="Microsoft YaHei, Segoe UI">急停后闸机锁定</text>

    <rect x="1030" y="140" width="180" height="100" rx="20" fill="rgba(15,23,42,0.72)" stroke="rgba(255,255,255,0.08)" />
    <text x="1050" y="182" font-size="24" fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">写入安全事件</text>
    <text x="1050" y="214" font-size="16" fill="#94a3b8" font-family="Microsoft YaHei, Segoe UI">急停生效 / 解除</text>

    <rect x="280" y="330" width="280" height="104" rx="20" fill="rgba(15,23,42,0.72)" stroke="rgba(255,255,255,0.08)" />
    <text x="304" y="372" font-size="24" fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">更新系统节点健康等级</text>
    <text x="304" y="404" font-size="16" fill="#94a3b8" font-family="Microsoft YaHei, Segoe UI">预警 / 稳定</text>

    <rect x="700" y="330" width="320" height="104" rx="20" fill="rgba(15,23,42,0.72)" stroke="rgba(255,255,255,0.08)" />
    <text x="724" y="372" font-size="24" fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">若 active=true，则新增高优先级告警</text>
    <text x="724" y="404" font-size="16" fill="#94a3b8" font-family="Microsoft YaHei, Segoe UI">记录操作台触发的急停告警</text>

    <line x1="320" y1="190" x2="390" y2="190" stroke="#38bdf8" stroke-width="6" stroke-linecap="round" />
    <line x1="640" y1="190" x2="710" y2="190" stroke="#38bdf8" stroke-width="6" stroke-linecap="round" />
    <line x1="960" y1="190" x2="1030" y2="190" stroke="#38bdf8" stroke-width="6" stroke-linecap="round" />
    <line x1="835" y1="240" x2="835" y2="290" stroke="#38bdf8" stroke-width="6" stroke-linecap="round" />
    <line x1="835" y1="290" x2="420" y2="290" stroke="#38bdf8" stroke-width="4" stroke-linecap="round" />
    <line x1="835" y1="290" x2="860" y2="290" stroke="#38bdf8" stroke-width="4" stroke-linecap="round" />
    <line x1="420" y1="290" x2="420" y2="330" stroke="#38bdf8" stroke-width="4" stroke-linecap="round" />
    <line x1="860" y1="290" x2="860" y2="330" stroke="#38bdf8" stroke-width="4" stroke-linecap="round" />
  `;

  return backgroundSvg(1280, 520, "DeviceService.setEmergency 控制流", "测试目标是验证 AI 入侵与人工急停能否一致地联动摄像头、闸机、系统节点与告警。", body);
}

async function main() {
  await mkdir(figuresDir, { recursive: true });
  const raw = await readFile(path.join(dataDir, "system-test-results.json"), "utf8");
  const results = JSON.parse(raw);

  const overview = backgroundSvg(
    1280,
    420,
    "测试数据规模概览",
    "以下统计均来自本轮真实运行后的数据库快照。",
    [
      card(40, 140, 260, 160, "车位总数", String(results.dataset.slotCount), "覆盖空位、占用、缓存、充电等状态"),
      card(340, 140, 260, 160, "订单总数", String(results.dataset.orderCount), "含历史单、进行中订单与新增测试单", "#10b981"),
      card(640, 140, 260, 160, "调度任务", String(results.dataset.queueCount), "包含先到先取、预调度、VIP、临取等类型", "#f59e0b"),
      card(940, 140, 260, 160, "设备事件", String(results.dataset.deviceEventCount), "摄像头、闸机、充电桩与调度事件", "#ef4444"),
    ].join("\n"),
  );

  const passRate = `${results.summary.functionalPassed + results.summary.negativePassed}/${results.summary.functionalTotal + results.summary.negativeTotal}`;
  const resultFigure = backgroundSvg(
    1280,
    420,
    "测试结果总览",
    "功能、黑盒异常、设备联动与数据持久化均以真实接口结果为依据。",
    [
      card(40, 140, 360, 160, "功能测试通过", `${results.summary.functionalPassed}/${results.summary.functionalTotal}`, "覆盖入场、预调度、VIP、计费、导航、报表"),
      card(440, 140, 360, 160, "黑盒异常通过", `${results.summary.negativePassed}/${results.summary.negativeTotal}`, "覆盖非法订单号、缺参请求与错误路径", "#f59e0b"),
      card(840, 140, 360, 160, "综合通过率", passRate, results.summary.overallPass ? "本轮测试全部通过" : "仍存在失败项，需人工复核", results.summary.overallPass ? "#10b981" : "#ef4444"),
    ].join("\n"),
  );

  await writeFile(path.join(figuresDir, "test-process.svg"), processFigure(), "utf8");
  await writeFile(path.join(figuresDir, "order-service-flow.svg"), orderFlowFigure(), "utf8");
  await writeFile(path.join(figuresDir, "emergency-control-flow.svg"), emergencyFlowFigure(), "utf8");
  await writeFile(path.join(figuresDir, "dataset-overview.svg"), overview, "utf8");
  await writeFile(path.join(figuresDir, "test-results-overview.svg"), resultFigure, "utf8");

  console.log(`Saved supporting figures to ${figuresDir}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
