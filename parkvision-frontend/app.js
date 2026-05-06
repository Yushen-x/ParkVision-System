const state = {
  currentView: "overview",
  emergency: false,
  adminTab: "orders",
  durationMinutes: 156,
  fee: 18,
  traffic: [12, 18, 16, 22, 35, 48, 52, 42, 36, 58, 64, 49],
  prediction: [61, 57, 49, 43, 38, 32],
  events: [
    ["系统启动", "数字孪生沙盘与模拟数据流已连接"],
    ["AI 边缘节点", "车牌识别服务返回沪A·P7686，置信度 0.98"],
    ["调度中枢", "AGV-03 接收 A08 至浅层缓存区任务"],
  ],
  ownerTimeline: [
    ["入库完成", "车辆已由 AGV 搬运至 B2-18"],
    ["计费开始", "工作日阶梯费率已生效"],
    ["等待取车", "可在车主端发起取车或临时取物"],
  ],
  slots: [],
  agvs: [
    { id: "AGV-01", x: 10, y: 12, load: false, task: "巡航至 A 区" },
    { id: "AGV-02", x: 45, y: 32, load: true, task: "搬运沪A·P7686" },
    { id: "AGV-03", x: 72, y: 58, load: false, task: "前往浅层缓存区" },
    { id: "AGV-04", x: 28, y: 76, load: false, task: "充电待命" },
  ],
  queue: [
    { plate: "沪A·P7686", type: "普通取车", tag: "FIFO", wait: "04:12", vip: false },
    { plate: "沪D·E5218", type: "新能源腾桩", tag: "充电完成", wait: "03:40", vip: false },
    { plate: "苏E·M9021", type: "临时取物", tag: "Touch", wait: "02:10", vip: false },
    { plate: "沪B·V7780", type: "预约出库", tag: "预约", wait: "01:58", vip: false },
  ],
  tasks: [
    ["接收取车请求", "订单状态由 parked 切换为 retrieving"],
    ["计算 AGV 路径", "规避充电区与维修通道"],
    ["下发搬运指令", "AGV-02 正在前往 B2-18"],
    ["车辆到达交接区", "等待车主确认支付后放行"],
  ],
  orders: [
    ["PV20260506001", "沪A·P7686", "入库", "A区入口", "停车中", "¥18.00"],
    ["PV20260506002", "沪D·E5218", "充电完成", "C区充电位", "待腾桩", "¥42.50"],
    ["PV20260506003", "苏E·M9021", "临时取物", "交接区2", "倒计时", "¥25.00"],
    ["PV20260506004", "浙A·K1314", "出库", "出口闸机", "已支付", "¥16.00"],
  ],
  alerts: [
    ["AL2026050601", "安全", "交接区人员闯入", "已急停", "高"],
    ["AL2026050602", "设备", "AGV-04 电量低于 20%", "处理中", "中"],
    ["AL2026050603", "订单", "车牌二次识别不一致", "待复核", "中"],
  ],
};

const viewTitles = {
  overview: "总览驾驶舱",
  owner: "车主端 H5",
  admin: "运营后台",
  twin: "数字孪生",
  ai: "AI 感知",
  dispatch: "调度中枢",
  demo: "答辩脚本",
};

const plates = ["沪A·P7686", "沪D·E5218", "苏E·M9021", "浙A·K1314", "沪B·V7780", "皖C·N2608"];

function createSlots() {
  const statuses = ["empty", "occupied", "occupied", "empty", "charging", "buffer", "occupied", "empty"];
  state.slots = Array.from({ length: 72 }, (_, index) => ({
    id: `${String.fromCharCode(65 + Math.floor(index / 12))}${String(index % 12 + 1).padStart(2, "0")}`,
    status: index === 64 ? "maintenance" : statuses[index % statuses.length],
    layer: index < 24 ? "浅层" : index < 48 ? "中层" : "深层",
  }));
}

function $(selector) {
  return document.querySelector(selector);
}

function $all(selector) {
  return Array.from(document.querySelectorAll(selector));
}

function nowTime() {
  return new Date().toLocaleTimeString("zh-CN", { hour12: false });
}

function addEvent(title, detail) {
  state.events.unshift([title, detail]);
  state.events = state.events.slice(0, 8);
  renderEvents();
}

function toast(title, detail = "") {
  const stack = $("#toastStack");
  const node = document.createElement("div");
  node.className = "toast";
  node.innerHTML = `<b>${title}</b>${detail ? `<span>${detail}</span>` : ""}`;
  stack.appendChild(node);
  setTimeout(() => node.remove(), 3200);
}

function switchView(view) {
  state.currentView = view;
  $all(".view").forEach((item) => item.classList.toggle("active", item.id === view));
  $all(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.view === view));
  $("#viewTitle").textContent = viewTitles[view];
  if (view === "overview") drawTrafficChart();
}

function renderKpis() {
  const occupied = state.slots.filter((slot) => slot.status !== "empty").length;
  const free = state.slots.length - occupied;
  $("#occupancyRate").textContent = `${Math.round((occupied / state.slots.length) * 100)}%`;
  $("#trafficTotal").textContent = state.traffic.reduce((sum, value) => sum + value, 0);
  $("#agvOnline").textContent = `${state.agvs.length}/${state.agvs.length}`;
  $("#alertCount").textContent = state.alerts.length;
  $("#mobileFreeCount").textContent = free;
}

function renderMiniGrid() {
  const mini = $("#miniSlotGrid");
  mini.innerHTML = state.slots
    .slice(0, 60)
    .map((slot) => `<span class="slot-dot ${slot.status}" title="${slot.id} ${slot.status}"></span>`)
    .join("");
}

function renderEvents() {
  $("#eventList").innerHTML = state.events
    .map(([title, detail]) => `<div class="event-item"><b>${title}</b><span>${nowTime()} · ${detail}</span></div>`)
    .join("");
}

function drawTrafficChart() {
  const canvas = $("#trafficCanvas");
  if (!canvas) return;
  const ctx = canvas.getContext("2d");
  const width = canvas.width;
  const height = canvas.height;
  ctx.clearRect(0, 0, width, height);
  ctx.fillStyle = "#f8fbfc";
  ctx.fillRect(0, 0, width, height);

  const padding = 46;
  const chartWidth = width - padding * 2;
  const chartHeight = height - padding * 2;
  const max = Math.max(...state.traffic, ...state.prediction) + 12;

  ctx.strokeStyle = "#d8e1e7";
  ctx.lineWidth = 1;
  ctx.font = "12px Microsoft YaHei";
  ctx.fillStyle = "#667782";

  for (let i = 0; i <= 4; i += 1) {
    const y = padding + (chartHeight / 4) * i;
    ctx.beginPath();
    ctx.moveTo(padding, y);
    ctx.lineTo(width - padding, y);
    ctx.stroke();
    ctx.fillText(String(Math.round(max - (max / 4) * i)), 12, y + 4);
  }

  const all = [...state.traffic, ...state.prediction];
  const step = chartWidth / (all.length - 1);

  ctx.beginPath();
  all.forEach((value, index) => {
    const x = padding + step * index;
    const y = padding + chartHeight - (value / max) * chartHeight;
    if (index === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  });
  ctx.strokeStyle = "#0e5f8d";
  ctx.lineWidth = 3;
  ctx.stroke();

  all.forEach((value, index) => {
    const x = padding + step * index;
    const y = padding + chartHeight - (value / max) * chartHeight;
    ctx.fillStyle = index < state.traffic.length ? "#117c73" : "#c4652d";
    ctx.beginPath();
    ctx.arc(x, y, 4.8, 0, Math.PI * 2);
    ctx.fill();
  });

  ctx.setLineDash([6, 6]);
  ctx.strokeStyle = "#c4652d";
  ctx.beginPath();
  const splitX = padding + step * (state.traffic.length - 1);
  ctx.moveTo(splitX, padding);
  ctx.lineTo(splitX, height - padding);
  ctx.stroke();
  ctx.setLineDash([]);

  ctx.fillStyle = "#14212b";
  ctx.font = "13px Microsoft YaHei";
  ctx.fillText("历史车流", padding, 24);
  ctx.fillStyle = "#c4652d";
  ctx.fillText("未来预测", splitX + 16, 24);

  all.forEach((value, index) => {
    if (index % 2 === 0) {
      const x = padding + step * index;
      ctx.fillStyle = "#667782";
      ctx.fillText(`${index < 12 ? index + 8 : index - 4}:00`, x - 12, height - 14);
    }
  });
}

function renderOwnerTimeline() {
  $("#ownerTimeline").innerHTML = state.ownerTimeline
    .map(
      ([title, detail]) => `
        <div class="timeline-entry">
          <span class="timeline-dot"></span>
          <div><b>${title}</b><span>${detail}</span></div>
        </div>
      `,
    )
    .join("");
}

function renderAdminTable() {
  const tableMap = {
    orders: {
      head: ["订单号", "车牌", "事件", "位置", "状态", "费用"],
      rows: state.orders,
    },
    pricing: {
      head: ["规则名", "适用时段", "计费方式", "附加策略", "状态"],
      rows: [
        ["工作日阶梯计费", "07:00-22:00", "首小时 ¥6，之后 ¥4/小时", "封顶 ¥48", "启用"],
        ["夜间包干", "22:00-07:00", "¥12 包干", "月卡减免", "启用"],
        ["VIP 加急取车", "全天", "基础费用 + ¥8", "队列权重 +40", "启用"],
        ["新能源充电", "全天", "¥1.2/kWh", "满电自动腾桩", "启用"],
      ],
    },
    alerts: {
      head: ["告警号", "类型", "内容", "处理状态", "级别"],
      rows: state.alerts,
    },
    access: {
      head: ["车牌", "名单类型", "用户类型", "有效期", "备注"],
      rows: [
        ["沪A·P7686", "白名单", "月卡用户", "2026-12-31", "自动放行"],
        ["沪D·E5218", "白名单", "新能源用户", "2026-09-01", "充电优先"],
        ["苏E·M9021", "普通用户", "临停", "单次订单", "支持无感支付"],
        ["沪X·B9001", "黑名单", "异常欠费", "人工解除", "禁止入场"],
      ],
    },
  };
  const config = tableMap[state.adminTab];
  $("#adminTableHead").innerHTML = `<tr>${config.head.map((item) => `<th>${item}</th>`).join("")}</tr>`;
  $("#adminTableBody").innerHTML = config.rows
    .map((row) => `<tr>${row.map((item) => `<td>${item}</td>`).join("")}</tr>`)
    .join("");
}

function renderWarehouse() {
  const map = $("#warehouseMap");
  const slots = state.slots
    .map(
      (slot) => `
        <div class="warehouse-slot ${slot.status}">
          <strong>${slot.id}</strong>
          ${slot.layer}
        </div>
      `,
    )
    .join("");
  const agvs = state.agvs
    .map(
      (agv) => `
        <div class="agv-unit ${agv.load ? "loaded" : ""}" style="left:${agv.x}%;top:${agv.y}%;" title="${agv.task}">
          ${agv.id.slice(-2)}
        </div>
      `,
    )
    .join("");
  map.innerHTML = slots + agvs;
}

function renderAgvs() {
  $("#agvList").innerHTML = state.agvs
    .map(
      (agv) => `
        <div class="agv-card">
          <b>${agv.id} · ${agv.load ? "载车" : "空载"}</b>
          <span>坐标 ${Math.round(agv.x)}, ${Math.round(agv.y)} · ${agv.task}</span>
        </div>
      `,
    )
    .join("");
}

function renderVisionJson(intrusion = false) {
  const plate = $("#detectedPlate").textContent;
  const payload = {
    requestId: `edge-${Date.now().toString().slice(-6)}`,
    cameraId: "gate-A-01",
    plate,
    confidence: Number((0.94 + Math.random() * 0.05).toFixed(3)),
    vehicleType: plate.includes("D") ? "new-energy" : "sedan",
    slotOccupancy: {
      free: state.slots.filter((slot) => slot.status === "empty").length,
      occupied: state.slots.filter((slot) => slot.status !== "empty").length,
    },
    intrusion,
    action: intrusion ? "ESTOP_AND_REVIEW" : "ALLOW_ENTRY_AND_CREATE_ORDER",
  };
  $("#visionJson").textContent = JSON.stringify(payload, null, 2);
  $("#visionStatus").textContent = intrusion ? "需复核" : "可信";
  $("#visionStatus").className = `status-pill ${intrusion ? "warning" : "stable"}`;
}

function renderQueue() {
  $("#queueList").innerHTML = state.queue
    .map(
      (item, index) => `
        <div class="queue-item ${item.vip ? "vip" : ""}">
          <span class="queue-rank">${index + 1}</span>
          <div>
            <b>${item.plate} · ${item.type}</b>
            <span>预计等待 ${item.wait}</span>
          </div>
          <span class="queue-tag">${item.tag}</span>
        </div>
      `,
    )
    .join("");
}

function renderTasks() {
  $("#taskTimeline").innerHTML = state.tasks
    .map(
      ([title, detail], index) => `
        <div class="task-item">
          <span class="queue-rank">${index + 1}</span>
          <div><b>${title}</b><span>${detail}</span></div>
          <span class="queue-tag">${index < 2 ? "已完成" : "进行中"}</span>
        </div>
      `,
    )
    .join("");
}

function simulateEntry() {
  const empty = state.slots.find((slot) => slot.status === "empty");
  const plate = plates[Math.floor(Math.random() * plates.length)];
  if (empty) empty.status = plate.includes("D") ? "charging" : "occupied";
  state.orders.unshift([`PV${Date.now().toString().slice(-8)}`, plate, "AI识别入库", empty ? empty.id : "等待区", "停车中", "¥0.00"]);
  state.traffic[state.traffic.length - 1] += 1;
  $("#detectedPlate").textContent = plate;
  addEvent("模拟入场", `${plate} 已识别，订单创建并分配至 ${empty ? empty.id : "等待区"}`);
  toast("车辆入场完成", `${plate} 已进入停车计费流程`);
  renderAll();
}

function triggerPreDispatch() {
  const deepOccupied = state.slots.find((slot) => slot.layer === "深层" && slot.status === "occupied");
  const buffer = state.slots.find((slot) => slot.status === "empty");
  if (deepOccupied) deepOccupied.status = "buffer";
  if (buffer) buffer.status = "buffer";
  state.queue.unshift({ plate: "沪A·P7686", type: "提前移库", tag: "Pre", wait: "00:48", vip: true });
  state.tasks.unshift(["生成预调度任务", "预测高峰到来，优先把深层车辆迁移至浅层缓存区"]);
  $("#dispatchPressure").textContent = "高峰预警";
  $("#dispatchPressure").className = "status-pill warning";
  addEvent("预调度触发", "预测未来 30 分钟出库需求超过阈值，已生成深浅库位迁移任务");
  toast("已触发预调度", "深层车辆正在迁移至浅层缓存区");
  renderAll();
}

function toggleEmergency() {
  state.emergency = !state.emergency;
  const safetyCard = $("#safetyCard");
  const twinStatus = $("#twinStatus");
  if (state.emergency) {
    safetyCard.classList.add("danger");
    safetyCard.innerHTML = "<strong>安全急停已触发</strong><span>检测到交接区异常闯入，AGV 队列冻结，等待人工复核。</span>";
    twinStatus.textContent = "急停冻结";
    twinStatus.className = "status-pill warning";
    $("#systemMode").textContent = "安全急停";
    $("#intruderMark").classList.add("show");
    renderVisionJson(true);
    addEvent("安全急停", "交接区检测到人员闯入，已冻结 AGV 指令队列");
    toast("安全急停已触发", "演示安全网关联动和异常追溯能力");
  } else {
    safetyCard.classList.remove("danger");
    safetyCard.innerHTML = "<strong>交接区正常</strong><span>未发现人员闯入，闸机允许放行。</span>";
    twinStatus.textContent = "运行中";
    twinStatus.className = "status-pill";
    $("#systemMode").textContent = "联调演示中";
    $("#intruderMark").classList.remove("show");
    renderVisionJson(false);
    addEvent("急停解除", "人工复核完成，AGV 队列恢复调度");
    toast("急停已解除", "系统恢复正常调度");
  }
}

function handleMobileAction(action) {
  const actions = {
    reserve: ["预约成功", "已为你锁定 A05 浅层车位 15 分钟", "已预约"],
    retrieve: ["取车申请已提交", "AGV-02 已加入取车队列，预计 4 分钟到达", "取车中"],
    touch: ["临时取物已开启", "车辆到达交接区后保留 8 分钟，计费不中断", "取物倒计时"],
    pay: ["缴费完成", "已支付 ¥18.00，出口闸机允许放行", "已支付"],
  };
  const [title, detail, status] = actions[action];
  $("#ownerOrderStatus").textContent = status;
  state.ownerTimeline.unshift([title, detail]);
  addEvent(`车主端：${title}`, detail);
  toast(title, detail);
  renderOwnerTimeline();
}

function moveAgvs() {
  if (state.emergency) return;
  state.agvs = state.agvs.map((agv, index) => {
    const dx = index % 2 === 0 ? 3 : -2;
    const dy = index % 2 === 0 ? -2 : 3;
    return {
      ...agv,
      x: agv.x + dx > 88 ? 12 : agv.x + dx < 8 ? 86 : agv.x + dx,
      y: agv.y + dy > 84 ? 14 : agv.y + dy < 10 ? 82 : agv.y + dy,
    };
  });
  renderWarehouse();
  renderAgvs();
}

function bindEvents() {
  $all(".nav-item").forEach((item) => item.addEventListener("click", () => switchView(item.dataset.view)));
  $all("[data-jump]").forEach((item) => item.addEventListener("click", () => switchView(item.dataset.jump)));
  $("#simulateEntryBtn").addEventListener("click", simulateEntry);
  $("#preDispatchBtn").addEventListener("click", triggerPreDispatch);
  $("#emergencyBtn").addEventListener("click", toggleEmergency);
  $("#clearLogBtn").addEventListener("click", () => {
    state.events = [];
    renderEvents();
  });
  $all("[data-mobile-action]").forEach((item) => {
    item.addEventListener("click", () => handleMobileAction(item.dataset.mobileAction));
  });
  $all("[data-admin-tab]").forEach((item) => {
    item.addEventListener("click", () => {
      state.adminTab = item.dataset.adminTab;
      $all("[data-admin-tab]").forEach((tab) => tab.classList.toggle("active", tab === item));
      renderAdminTable();
    });
  });
  $("#addRuleBtn").addEventListener("click", () => toast("规则草稿已创建", "可用于展示后台动态计费配置能力"));
  $("#reportBtn").addEventListener("click", () => {
    $("#reportOutput").textContent =
      "18:00 后出库需求明显上升，峰值集中在 19:20-20:10。建议提前 25 分钟执行 8 辆车的深浅库位迁移，预计平均等待时间可从 7 分 40 秒降至 4 分 10 秒。";
    toast("经营简报已生成", "自然语言查询已转化为受控统计结果");
  });
  $("#runVisionBtn").addEventListener("click", () => {
    const intrusion = Math.random() > 0.72;
    $("#detectedPlate").textContent = plates[Math.floor(Math.random() * plates.length)];
    $("#intruderMark").classList.toggle("show", intrusion);
    renderVisionJson(intrusion);
    addEvent("AI 识别完成", intrusion ? "检测到交接区人员闯入，建议急停复核" : "车牌识别通过，允许创建入库订单");
    toast("边缘识别完成", intrusion ? "发现安全风险" : "结构化结果已生成");
  });
  $("#enqueueVipBtn").addEventListener("click", () => {
    state.queue.unshift({ plate: "沪V·IP888", type: "VIP 加急取车", tag: "VIP", wait: "00:30", vip: true });
    addEvent("VIP 插队", "高优先级取车任务已插入队首");
    renderQueue();
    toast("VIP 任务已加入队列", "优先级权重 +40");
  });
  $("#optimizeBtn").addEventListener("click", triggerPreDispatch);
}

function tickClock() {
  $("#clock").textContent = nowTime();
  state.durationMinutes += 1;
  if (state.durationMinutes % 12 === 0) state.fee += 2;
  const hours = String(Math.floor(state.durationMinutes / 60)).padStart(2, "0");
  const minutes = String(state.durationMinutes % 60).padStart(2, "0");
  $("#parkingDuration").textContent = `${hours}:${minutes}`;
  $("#parkingFee").textContent = `¥${state.fee.toFixed(2)}`;
}

function renderAll() {
  renderKpis();
  renderMiniGrid();
  renderEvents();
  renderOwnerTimeline();
  renderAdminTable();
  renderWarehouse();
  renderAgvs();
  renderQueue();
  renderTasks();
  drawTrafficChart();
}

function init() {
  createSlots();
  bindEvents();
  renderAll();
  renderVisionJson(false);
  tickClock();
  setInterval(tickClock, 1000);
  setInterval(moveAgvs, 900);
}

init();
