import { computed, reactive } from "vue";
import { parkvisionApi } from "../api/parkvisionApi";

const plates = ["沪A·P7686", "沪D·E5218", "苏E·M9021", "浙A·K1314", "沪B·V7780", "皖C·N2608"];

function createSlots() {
  const statuses = ["empty", "occupied", "occupied", "empty", "charging", "buffer", "occupied", "empty"];
  return Array.from({ length: 72 }, (_, index) => ({
    id: `${String.fromCharCode(65 + Math.floor(index / 12))}${String(index % 12 + 1).padStart(2, "0")}`,
    status: index === 64 ? "maintenance" : statuses[index % statuses.length],
    layer: index < 24 ? "浅层" : index < 48 ? "中层" : "深层",
  }));
}

export const state = reactive({
  onlineMode: "mock",
  emergency: false,
  activePlate: "沪A·P7686",
  summary: {
    occupancyRate: 68,
    trafficTotal: 414,
    agvOnline: "4/4",
    alertCount: 3,
    revenue: 8426,
    avgWait: "04:12",
    chargingTurnover: "7.4 次/日",
  },
  forecast: {
    history: [12, 18, 16, 22, 35, 48, 52, 42, 36, 58, 64, 49],
    prediction: [61, 57, 49, 43, 38, 32],
  },
  events: [
    ["系统启动", "数字孪生沙盘与模拟数据流已连接"],
    ["AI 边缘节点", "车牌识别服务返回沪A·P7686，置信度 0.98"],
    ["调度中枢", "AGV-03 接收 A08 至浅层缓存区任务"],
  ],
  slots: createSlots(),
  agvs: [
    { id: "AGV-01", x: 10, y: 12, load: false, task: "巡航至 A 区" },
    { id: "AGV-02", x: 45, y: 32, load: true, task: "搬运沪A·P7686" },
    { id: "AGV-03", x: 72, y: 58, load: false, task: "前往浅层缓存区" },
    { id: "AGV-04", x: 28, y: 76, load: false, task: "充电待命" },
  ],
  orders: [],
  alerts: [],
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
  ownerTimeline: [
    ["入库完成", "车辆已由 AGV 搬运至 B2-18"],
    ["计费开始", "工作日阶梯费率已生效"],
    ["等待取车", "可在车主端发起取车或临时取物"],
  ],
  visionResult: {
    plate: "沪A·P7686",
    confidence: 0.98,
    intrusion: false,
    action: "ALLOW_ENTRY_AND_CREATE_ORDER",
  },
});

export const getters = {
  freeCount: computed(() => state.slots.filter((slot) => slot.status === "empty").length),
  occupiedCount: computed(() => state.slots.filter((slot) => slot.status !== "empty").length),
};

export function addEvent(title, detail) {
  state.events.unshift([title, detail]);
  state.events = state.events.slice(0, 10);
}

export async function hydrate() {
  const [summary, orders, alerts, forecast] = await Promise.all([
    parkvisionApi.getSummary(),
    parkvisionApi.getOrders(),
    parkvisionApi.getAlerts(),
    parkvisionApi.getForecast(),
  ]);
  state.summary = summary;
  state.orders = orders;
  state.alerts = alerts;
  state.forecast = forecast;
}

export function simulateEntry() {
  const empty = state.slots.find((slot) => slot.status === "empty");
  const plate = plates[Math.floor(Math.random() * plates.length)];
  if (empty) empty.status = plate.includes("D") ? "charging" : "occupied";
  state.activePlate = plate;
  state.orders.unshift([`PV${Date.now().toString().slice(-8)}`, plate, "AI识别入库", empty?.id || "等待区", "停车中", "¥0.00"]);
  state.summary.trafficTotal += 1;
  state.summary.occupancyRate = Math.round((getters.occupiedCount.value / state.slots.length) * 100);
  addEvent("模拟入场", `${plate} 已识别，订单创建并分配至 ${empty?.id || "等待区"}`);
}

export function triggerPreDispatch() {
  const deep = state.slots.find((slot) => slot.layer === "深层" && slot.status === "occupied");
  if (deep) deep.status = "buffer";
  state.queue.unshift({ plate: state.activePlate, type: "提前移库", tag: "Pre", wait: "00:48", vip: true });
  state.tasks.unshift(["生成预调度任务", "预测高峰到来，优先把深层车辆迁移至浅层缓存区"]);
  addEvent("预调度触发", "未来 30 分钟出库需求超过阈值，已生成深浅库位迁移任务");
}

export function enqueueVip() {
  state.queue.unshift({ plate: "沪V·IP888", type: "VIP 加急取车", tag: "VIP", wait: "00:30", vip: true });
  addEvent("VIP 插队", "高优先级取车任务已插入队首");
}

export function toggleEmergency() {
  state.emergency = !state.emergency;
  state.visionResult.intrusion = state.emergency;
  state.visionResult.action = state.emergency ? "ESTOP_AND_REVIEW" : "ALLOW_ENTRY_AND_CREATE_ORDER";
  addEvent(state.emergency ? "安全急停" : "急停解除", state.emergency ? "交接区检测到人员闯入，已冻结 AGV 指令队列" : "人工复核完成，AGV 队列恢复调度");
}

export function runVision() {
  const plate = plates[Math.floor(Math.random() * plates.length)];
  const intrusion = Math.random() > 0.75;
  state.activePlate = plate;
  state.visionResult = {
    plate,
    confidence: Number((0.94 + Math.random() * 0.05).toFixed(3)),
    intrusion,
    action: intrusion ? "ESTOP_AND_REVIEW" : "ALLOW_ENTRY_AND_CREATE_ORDER",
  };
  addEvent("AI 识别完成", intrusion ? "检测到交接区人员闯入，建议急停复核" : `${plate} 识别通过，允许创建入库订单`);
}

export function handleOwnerAction(action) {
  const actions = {
    reserve: ["预约成功", "已为你锁定 A05 浅层车位 15 分钟"],
    retrieve: ["取车申请已提交", "AGV-02 已加入取车队列，预计 4 分钟到达"],
    touch: ["临时取物已开启", "车辆到达交接区后保留 8 分钟，计费不中断"],
    pay: ["缴费完成", "已支付 ¥18.00，出口闸机允许放行"],
  };
  const [title, detail] = actions[action];
  state.ownerTimeline.unshift([title, detail]);
  addEvent(`车主端：${title}`, detail);
}

export function moveAgvs() {
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
}
