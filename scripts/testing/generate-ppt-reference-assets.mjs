import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { chromium } from "playwright";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "../..");
const outputDir = path.join(projectRoot, "docs", "testing", "assets", "ppt");

function escapeXml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function wrapText(value, maxChars) {
  const text = String(value ?? "");
  const lines = [];
  for (const rawPart of text.split("\n")) {
    let part = rawPart.trim();
    if (!part) {
      lines.push("");
      continue;
    }
    while (part.length > maxChars) {
      let cut = maxChars;
      const punctuation = part.slice(0, maxChars + 1).search(/[，。；、：/]/);
      if (punctuation > Math.floor(maxChars * 0.55)) {
        cut = punctuation + 1;
      }
      lines.push(part.slice(0, cut));
      part = part.slice(cut);
    }
    lines.push(part);
  }
  return lines;
}

function textLines(text, x, y, width, rowHeight, options = {}) {
  const fontSize = options.fontSize ?? 20;
  const lineHeight = options.lineHeight ?? Math.round(fontSize * 1.35);
  const maxChars = options.maxChars ?? Math.max(4, Math.floor(width / (fontSize * 0.92)));
  const maxLines = Math.max(1, Math.floor((rowHeight - 18) / lineHeight));
  const lines = wrapText(text, maxChars);
  const clipped = lines.length > maxLines ? [...lines.slice(0, maxLines - 1), `${lines[maxLines - 1]}...`] : lines;
  const fontWeight = options.bold ? 700 : 400;
  const fill = options.fill ?? "#111827";
  const anchor = options.anchor ?? "start";
  const textX = anchor === "middle" ? x + width / 2 : x + 12;
  return clipped
    .map((line, index) => {
      const dy = y + 24 + index * lineHeight;
      return `<text x="${textX}" y="${dy}" font-size="${fontSize}" font-weight="${fontWeight}" fill="${fill}" font-family="Microsoft YaHei, SimSun, Arial" text-anchor="${anchor}">${escapeXml(line)}</text>`;
    })
    .join("\n");
}

function tableSvg({ title, subtitle, columns, rows, widths, rowHeight = 130, fontSize = 19, height = 900 }) {
  const width = 1600;
  const tableWidth = widths.reduce((sum, value) => sum + value, 0);
  const x0 = Math.round((width - tableWidth) / 2);
  const y0 = 142;
  const headerHeight = 58;
  const totalHeight = headerHeight + rows.length * rowHeight;

  let currentX = x0;
  const header = columns
    .map((column, index) => {
      const w = widths[index];
      const svg = `
        <rect x="${currentX}" y="${y0}" width="${w}" height="${headerHeight}" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="1" />
        ${textLines(column, currentX, y0, w, headerHeight, { fontSize: 21, bold: true, maxChars: Math.floor(w / 17) })}
      `;
      currentX += w;
      return svg;
    })
    .join("\n");

  const body = rows
    .map((row, rowIndex) => {
      const y = y0 + headerHeight + rowIndex * rowHeight;
      let x = x0;
      return row
        .map((cell, colIndex) => {
          const w = widths[colIndex];
          const fill = rowIndex % 2 === 0 ? "#ffffff" : "#f8fafc";
          const cellSvg = `
            <rect x="${x}" y="${y}" width="${w}" height="${rowHeight}" fill="${fill}" stroke="#cbd5e1" stroke-width="1" />
            ${textLines(cell, x, y, w, rowHeight, { fontSize, bold: colIndex === 2 || String(cell).startsWith("通过"), maxChars: Math.floor(w / (fontSize * 0.9)) })}
          `;
          x += w;
          return cellSvg;
        })
        .join("\n");
    })
    .join("\n");

  return `<?xml version="1.0" encoding="UTF-8"?>
<svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg">
  <rect width="${width}" height="${height}" fill="#ffffff" />
  <text x="64" y="66" font-size="38" font-weight="700" fill="#0f172a" font-family="Microsoft YaHei, SimHei, Arial">${escapeXml(title)}</text>
  <text x="64" y="104" font-size="21" fill="#64748b" font-family="Microsoft YaHei, SimHei, Arial">${escapeXml(subtitle)}</text>
  <rect x="${x0}" y="${y0}" width="${tableWidth}" height="${totalHeight}" fill="none" stroke="#94a3b8" stroke-width="1.4" />
  ${header}
  ${body}
</svg>
`;
}

const requirementColumns = ["需求编号", "目标用户", "子系统", "需求", "完整性", "正确性", "一致性", "可行性", "必要性", "可测试性", "可追踪性"];
const requirementWidths = [105, 125, 165, 245, 95, 95, 110, 110, 110, 120, 120];

const functionalRequirements = [
  ["R1.1.1", "运营人员", "运营首页", "展示车位占用率、今日车流、AGV 在线数、告警数与交通预测", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R1.2.1", "车主", "订单服务", "车辆入场后创建停车订单并占用真实车位", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R1.2.2", "车主", "车主端", "支持取车、临时取物、支付完成并关闭订单", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R1.3.1", "调度员", "调度中心", "支持标准取车、预调度、VIP 优先和临停取物队列", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R1.4.1", "安全员", "AI 感知", "识别车牌、置信度和入侵事件，入侵时触发急停", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R1.5.1", "管理员", "管理台", "查询订单、告警、名单、计费规则并生成 AI 报表", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R1.6.1", "车主", "室内导航", "返回接驳点、剩余距离、车主 ETA、AGV ETA 与安全提示", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
];

const nonFunctionalRequirements = [
  ["R2.1.1", "开发/测试", "可启动性", "前后端可通过脚本一键启动，健康检查返回 UP", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R2.1.2", "开发/测试", "持久化", "首次启动自动建库并写入 H2 文件数据库", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R2.2.1", "使用者", "性能", "核心接口在并发压测下保持 100% 成功率和可接受响应时间", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R2.3.1", "使用者", "可靠性", "非法订单、缺参请求和急停恢复均有明确处理路径", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R2.4.1", "管理人员", "可观测性", "节点、摄像头、闸机、充电桩、事件与告警可视化展示", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R2.5.1", "教师/评审", "可用性", "页面导航清晰，截图证据覆盖主要演示链路", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
  ["R2.6.1", "安全员", "安全边界", "急停可锁定闸机和节点，接口异常返回结构化错误", "完整", "正确", "一致", "可行", "必要", "可测试", "可追踪"],
];

const functionColumns = ["功能模块", "用例", "用例编号", "用例说明", "前置条件", "输入/动作", "预期结果", "测试结果", "失败原因"];
const functionWidths = [145, 135, 115, 260, 195, 195, 220, 130, 105];

const functionalCaseTables = [
  {
    file: "functional-test-cases-01.svg",
    title: "测试用例表（一）：运营与订单",
    rows: [
      ["运营首页", "数据展示", "T1.1.1", "进入运营首页，查看 KPI、车位概览、交通预测和事件流", "前后端启动，数据库已初始化", "打开 /dashboard", "页面显示真实统计、车位和预测数据", "通过", "无"],
      ["订单服务", "车辆入场", "T1.1.2", "车辆入场后创建订单并绑定空闲车位", "存在空闲车位", "输入车牌 SH-K1314，调用入场接口", "订单状态为 PARKED，空位数减少，车位变为占用", "通过", "无"],
      ["调度中心", "预调度", "T1.1.3", "高峰期提前移动车辆，生成预调度任务", "订单已停放", "执行高峰预调度动作", "队列头部新增预调度任务，任务来源可追踪", "通过", "无"],
      ["调度中心", "标准取车", "T1.1.4", "车主发起取车后进入调度队列", "订单状态为 PARKED", "点击取车或调用 retrieve 接口", "订单变为 RETRIEVING，生成标准取车任务", "通过", "无"],
    ],
  },
  {
    file: "functional-test-cases-02.svg",
    title: "测试用例表（二）：AI 感知与安全联动",
    rows: [
      ["AI 感知", "正常识别", "T1.2.1", "采集画面后返回车牌、置信度、摄像头 ID 和动作建议", "摄像头在线", "点击采集画面，intrusion=false", "返回 OCR 结果，系统不进入急停", "通过", "无"],
      ["AI 感知", "入侵急停", "T1.2.2", "检测到交接区入侵后触发系统急停", "摄像头在线，闸机在线", "点击检测入侵，intrusion=true", "摄像头 ROI 风险、闸机急停、节点 warning、告警新增", "通过", "无"],
      ["数字孪生", "急停展示", "T1.2.3", "数字孪生页面展示安全联动后的 AGV、车位、闸机状态", "急停已触发", "打开数字孪生页面", "急停状态与正常孪生页面有明显差异", "通过", "无"],
      ["设备控制", "解除急停", "T1.2.4", "人工解除急停后设备恢复自动模式", "系统处于急停", "调用 emergency active=false", "闸机解除锁定，节点恢复稳定，事件流记录解除动作", "通过", "无"],
    ],
  },
  {
    file: "functional-test-cases-03.svg",
    title: "测试用例表（三）：车主端、计费与导航",
    rows: [
      ["车主端", "当前订单", "T1.3.1", "车主端展示当前停车订单、车位和状态", "存在进行中订单", "打开车主端页面", "可看到订单号、车牌、车位、状态和可执行动作", "通过", "无"],
      ["车主端", "临时取物", "T1.3.2", "车主发起临时取物后车辆进入缓冲区", "订单处于 PARKED", "点击临时取物", "订单变为 TOUCHING，车位变为 BUFFER", "通过", "无"],
      ["车主端", "支付关闭", "T1.3.3", "支付完成后关闭订单并释放车位", "订单待支付", "点击标记已支付并关闭订单", "订单 FINISHED，金额生成，车位释放为空位", "通过", "无"],
      ["动态计费", "费用预览", "T1.3.4", "根据订单时长、峰值、充电和 VIP 规则计算费用", "存在有效订单", "打开计费页并预览", "返回计费组成、规则解释和总金额", "通过", "无"],
    ],
  },
  {
    file: "functional-test-cases-04.svg",
    title: "测试用例表（四）：管理台与系统设备",
    rows: [
      ["室内导航", "接驳路线", "T1.4.1", "返回接驳点、剩余距离、车主 ETA、AGV ETA 与安全提示", "存在有效订单", "打开室内导航页", "路线快照、ETA 和提示信息完整展示", "通过", "无"],
      ["管理台", "订单列表", "T1.4.2", "管理员查看订单表并核对订单状态", "数据库存在订单", "打开管理台订单页", "订单表来自 /api/admin/orders，数据非空", "通过", "无"],
      ["管理台", "AI 报表", "T1.4.3", "管理员输入问题后生成运营报表和收入曲线", "后端服务在线", "点击生成报表", "返回报表摘要和最近 7 天对比图", "通过", "无"],
      ["系统配置", "设备监控", "T1.4.4", "展示摄像头、闸机、充电桩、节点和设备事件", "设备模拟数据已加载", "打开系统配置页", "ONVIF/Modbus/OCPP 风格字段均可见", "通过", "无"],
    ],
  },
];

const blackboxEquivalence = [
  ["订单号", "数据库中存在的订单号，如 PV20260506015", "不存在或格式错误，如 ORDER-404"],
  ["订单动作", "取车、临时取物、支付关闭", "不存在的动作或状态不允许的动作"],
  ["AI 推理参数", "cameraId 存在，intrusion 为 true/false", "cameraId 为空或 intrusion 类型错误"],
  ["急停接口", "active=true 或 active=false", "缺少 active 必填参数"],
  ["计费预览", "存在订单或默认有效订单", "不存在订单号"],
  ["室内导航", "存在订单并能映射车位/接驳点", "不存在订单号"],
  ["管理报表", "中文运营问题或默认查询", "空查询仍返回默认摘要，不阻断页面"],
];

const blackboxValid = [
  ["创建订单：车牌 SH-K1314，存在空闲车位", "订单创建成功并占用车位", "返回 PARKED 订单，空位减少", "1,2"],
  ["VIP 取车：对活动订单执行 VIP 优先", "订单进入 RETRIEVING，队列头部新增 VIP 任务", "返回 VIP 优先取车任务", "3"],
  ["临时取物：对活动订单执行 touch-and-go", "订单 TOUCHING，车位 BUFFER", "返回临停取物任务和缓冲车位状态", "4"],
  ["AI 正常识别：intrusion=false", "返回车牌和允许入场动作", "返回 OCR 置信度和 ALLOW_ENTRY 动作", "5"],
  ["管理台报表：查询最近 7 天收入", "返回摘要和图表数据", "返回中文摘要、labels 和收入数组", "6,7"],
];

const blackboxInvalid = [
  ["非法订单号 ORDER-404 调用取车", "400 + ORDER_NOT_FOUND", "返回结构化错误 ORDER_NOT_FOUND", "1"],
  ["非法订单号 ORDER-404 调用 VIP", "400 + ORDER_NOT_FOUND", "返回结构化错误 ORDER_NOT_FOUND", "1,2"],
  ["非法订单号 ORDER-404 调用计费预览", "400 + ORDER_NOT_FOUND", "返回结构化错误 ORDER_NOT_FOUND", "5"],
  ["非法订单号 ORDER-404 调用室内导航", "400 + ORDER_NOT_FOUND", "返回结构化错误 ORDER_NOT_FOUND", "6"],
  ["急停接口缺少 active 参数", "400 + VALIDATION_FAILED", "返回 active parameter is required", "3,4"],
];

const whiteboxCoverageColumns = ["用例ID", "输入", "期望输出", "订单存在", "取车分支", "临取分支", "支付分支", "车位释放", "急停开启", "急停解除", "缺参异常"];
const whiteboxCoverageWidths = [95, 180, 210, 105, 105, 105, 105, 105, 105, 105, 105];
const whiteboxCoverage = [
  ["W1", "有效订单 + 取车", "生成取车任务", "T", "T", "F", "F", "F", "/", "/", "/"],
  ["W2", "有效订单 + 临时取物", "生成临取任务，车位 BUFFER", "T", "F", "T", "F", "F", "/", "/", "/"],
  ["W3", "有效订单 + 支付完成", "订单关闭，车位释放", "T", "F", "F", "T", "T", "/", "/", "/"],
  ["W4", "不存在订单", "抛出 ORDER_NOT_FOUND", "F", "/", "/", "/", "/", "/", "/", "/"],
  ["W5", "active=true", "摄像头、闸机、节点、告警联动", "/", "/", "/", "/", "/", "T", "F", "F"],
  ["W6", "active=false", "设备恢复自动模式", "/", "/", "/", "/", "/", "F", "T", "F"],
  ["W7", "缺少 active", "返回 VALIDATION_FAILED", "/", "/", "/", "/", "/", "F", "F", "T"],
];

const staticChecklist = [
  ["前端路由完整性", "覆盖运营首页、AI、调度、孪生、管理台、计费、系统配置、车主端、室内导航", "通过"],
  ["前端数据来源", "Store 优先调用后端 API，失败时才使用兜底数据", "通过"],
  ["组件状态展示", "KPI、表格、事件流、截图页面均为中文展示", "通过"],
  ["后端分层结构", "Controller、Service、Repository 分层清晰", "通过"],
  ["持久化入口", "H2 文件数据库自动初始化并写入真实演示数据", "通过"],
  ["异常处理", "非法订单和缺参请求统一返回结构化错误", "通过"],
  ["自动化脚本", "截图、数据采集、性能测试、报告渲染脚本可重复运行", "通过"],
];

const performanceMethod = [
  ["线程数", "1、5、20、100 级别并发", "模拟单人演示到多人同时访问的压力"],
  ["Ramp-Up 时间", "1 到 10 秒", "避免瞬时启动造成非业务性抖动"],
  ["循环次数", "每个场景多轮请求", "统计平均响应、P95 和成功率"],
  ["测试对象", "运营汇总、调度队列、设备总览、计费预览、管理报表、视觉推理", "覆盖演示最常访问接口"],
];

const securitySummary = [
  ["输入校验", "急停接口缺少 active 时返回 VALIDATION_FAILED", "通过"],
  ["业务异常", "不存在订单统一返回 ORDER_NOT_FOUND", "通过"],
  ["错误信息", "接口返回结构化 JSON，不暴露后端堆栈", "通过"],
  ["安全联动", "AI 入侵或人工急停会锁定闸机和节点状态", "通过"],
  ["当前边界", "未接入登录鉴权，生产化需要补角色权限、审计和限流", "待扩展"],
];

async function main() {
  await mkdir(outputDir, { recursive: true });

  const files = [
    [
      "functional-requirements.svg",
      tableSvg({
        title: "功能性需求",
        subtitle: "参考 PPT 第 4 页：按需求编号和质量属性展示需求可测性",
        columns: requirementColumns,
        rows: functionalRequirements,
        widths: requirementWidths,
        rowHeight: 92,
        fontSize: 17,
      }),
    ],
    [
      "nonfunctional-requirements.svg",
      tableSvg({
        title: "非功能性需求",
        subtitle: "参考 PPT 第 5 页：维护性、性能、可靠性、安全边界和可用性",
        columns: requirementColumns,
        rows: nonFunctionalRequirements,
        widths: requirementWidths,
        rowHeight: 92,
        fontSize: 17,
      }),
    ],
    ...functionalCaseTables.map((item) => [
      item.file,
      tableSvg({
        title: item.title,
        subtitle: "参考 PPT 第 7-10 页：功能模块、用例编号、前置条件、输入、预期结果、实际结果",
        columns: functionColumns,
        rows: item.rows,
        widths: functionWidths,
        rowHeight: 158,
        fontSize: 18,
      }),
    ]),
    [
      "blackbox-equivalence.svg",
      tableSvg({
        title: "等价类列表",
        subtitle: "参考 PPT 第 15 页：按输入条件划分有效等价类和无效等价类",
        columns: ["输入条件", "有效等价类", "无效等价类"],
        rows: blackboxEquivalence,
        widths: [260, 600, 600],
        rowHeight: 84,
        fontSize: 21,
      }),
    ],
    [
      "blackbox-valid-suite.svg",
      tableSvg({
        title: "有效等价类测试套件",
        subtitle: "参考 PPT 第 16 页：测试数据、预期结果、实际输出、覆盖范围",
        columns: ["测试数据", "预期结果", "实际输出", "覆盖范围"],
        rows: blackboxValid,
        widths: [440, 390, 460, 170],
        rowHeight: 118,
        fontSize: 20,
      }),
    ],
    [
      "blackbox-invalid-suite.svg",
      tableSvg({
        title: "无效等价类测试套件",
        subtitle: "参考 PPT 第 17 页：异常输入必须返回明确错误",
        columns: ["测试数据", "预期结果", "实际输出", "覆盖范围"],
        rows: blackboxInvalid,
        widths: [440, 390, 460, 170],
        rowHeight: 118,
        fontSize: 20,
      }),
    ],
    [
      "whitebox-coverage.svg",
      tableSvg({
        title: "白盒测试用例与条件覆盖",
        subtitle: "参考 PPT 第 25 页：按分支条件标记 True/False 覆盖情况",
        columns: whiteboxCoverageColumns,
        rows: whiteboxCoverage,
        widths: whiteboxCoverageWidths,
        rowHeight: 86,
        fontSize: 18,
      }),
    ],
    [
      "static-test-checklist.svg",
      tableSvg({
        title: "静态测试检查表",
        subtitle: "参考 PPT 第 12-13 页：前端结构、后端分层、异常处理与脚本完整性",
        columns: ["检查项", "检查内容", "结果"],
        rows: staticChecklist,
        widths: [300, 900, 180],
        rowHeight: 82,
        fontSize: 21,
      }),
    ],
    [
      "performance-method.svg",
      tableSvg({
        title: "性能测试参数说明",
        subtitle: "参考 PPT 第 27 页：线程数、Ramp-Up 时间、循环次数和测试对象",
        columns: ["参数", "设置", "说明"],
        rows: performanceMethod,
        widths: [260, 420, 780],
        rowHeight: 120,
        fontSize: 22,
      }),
    ],
    [
      "security-summary.svg",
      tableSvg({
        title: "安全与异常处理测试",
        subtitle: "参考 PPT 第 29-30 页：结合当前项目真实能力说明安全边界",
        columns: ["测试点", "验证方式", "结果"],
        rows: securitySummary,
        widths: [300, 890, 190],
        rowHeight: 96,
        fontSize: 21,
      }),
    ],
  ];

  await Promise.all(files.map(([file, content]) => writeFile(path.join(outputDir, file), content, "utf8")));

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1600, height: 900 }, deviceScaleFactor: 2 });
  for (const [file] of files) {
    const svgPath = path.join(outputDir, file);
    const pngPath = svgPath.replace(/\.svg$/i, ".png");
    await page.goto(pathToFileURL(svgPath).href);
    await page.screenshot({ path: pngPath });
  }
  await browser.close();

  console.log(`Saved ${files.length} PPT reference assets to ${outputDir} as SVG and PNG`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
