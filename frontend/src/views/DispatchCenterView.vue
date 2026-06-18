<script setup>
import { computed, ref } from "vue";
import { enqueueVip, getters, runOwnerAction, runVision, state, triggerPreDispatch } from "../stores/parkingStore";
import { useNow } from "../composables/useNow";
import { zhMoney, zhText } from "../utils/localize";

const now = useNow();

const currentOrder = getters.currentOrder;
const keyword = ref("");
const statusFilter = ref("");
const selectedOrderNo = ref(currentOrder.value?.orderNo || state.orders[0]?.orderNo || "");

const statusOptions = [
  ["", "全部状态"],
  ["PARKED", "停车中"],
  ["RETRIEVING", "取车中"],
  ["TOUCHING", "取物中"],
  ["FINISHED", "已完成"],
];

const selectedOrder = computed(() => {
  const order = state.orders.find((item) => item.orderNo === selectedOrderNo.value);
  return order || currentOrder.value || state.orders[0] || null;
});

const filteredOrders = computed(() => {
  const text = keyword.value.trim().toLowerCase();
  return state.orders.filter((order) => {
    const matchesText =
      !text ||
      order.orderNo.toLowerCase().includes(text) ||
      order.plateNo.toLowerCase().includes(text) ||
      order.slotId.toLowerCase().includes(text);
    const matchesStatus = !statusFilter.value || order.status === statusFilter.value;
    return matchesText && matchesStatus;
  });
});

const activeCamera = computed(
  () => state.devices.cameras.find((camera) => camera.cameraId === state.visionResult.cameraId) || state.devices.cameras[0],
);
const outboundGate = computed(() => state.devices.gates.find((gate) => gate.gateId.includes("OUT")) || state.devices.gates[0]);
const route = computed(() => state.indoorRoute);
const selectedAgv = computed(() => {
  const plate = selectedOrder.value?.plateNo;
  return (
    state.agvs.find((agv) => plate && zhText(agv.task).includes(plate)) ||
    state.agvs.find((agv) => agv.loaded) ||
    state.agvs[0]
  );
});

const selectedQueue = computed(() => {
  const plate = selectedOrder.value?.plateNo;
  const matched = state.queue.filter((task) => task.plateNo === plate);
  return matched.length ? matched : state.queue.slice(0, 4);
});

function taskStatusText(task) {
  if (task.status === "DONE") return "已完成";
  if (task.status === "IN_PROGRESS") return `搬运中 ${task.progress || 0}%`;
  return `排队中 · 预计 ${task.wait || "--:--"}`;
}

function taskBadge(task) {
  if (task.status === "DONE") return "完成";
  if (task.status === "IN_PROGRESS") return "执行中";
  return "排队";
}

function taskTone(task) {
  if (task.status === "DONE") return "done";
  if (task.status === "IN_PROGRESS") return "active";
  return "";
}

const relatedEvents = computed(() => {
  const order = selectedOrder.value;
  if (!order) return [];
  const plate = order.plateNo;
  const slot = order.slotId;
  return [
    ...state.devices.events.filter((event) => zhText(event.message).includes(plate) || zhText(event.message).includes(slot)),
    ...state.events
      .filter(([, detail]) => zhText(detail).includes(plate) || zhText(detail).includes(order.orderNo))
      .map(([title, detail], index) => ({
        eventId: `local-${index}`,
        deviceId: title,
        eventCode: "业务事件",
        severity: "info",
        message: detail,
      })),
  ].slice(0, 6);
});

const elapsedLabel = computed(() => {
  if (!selectedOrder.value?.entryTime) return "无";
  const diff = Math.max(0, now.value - new Date(selectedOrder.value.entryTime).getTime());
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(minutes / 60);
  return `${hours} 小时 ${minutes % 60} 分钟`;
});

const billingRows = computed(() => {
  const order = selectedOrder.value;
  if (!order) return [];
  if (state.pricingPreview.orderNo === order.orderNo || state.pricingPreview.plateNo === order.plateNo) {
    return state.pricingPreview.components.filter((item) => Number(item.amount || 0) > 0);
  }
  return [{ label: "停车费", formula: "按订单已生成金额", amount: Number(order.amount || 0), accent: "base" }];
});

const traceCards = computed(() => {
  const order = selectedOrder.value;
  if (!order) return [];
  return [
    {
      label: "订单状态",
      value: zhText(order.status),
      detail: `${order.orderNo} / ${order.slotId}`,
      icon: "fa-file-lines",
    },
    {
      label: "视觉记录",
      value: activeCamera.value?.lastPlate || state.visionResult.plate,
      detail: `${activeCamera.value?.cameraId || "CAM"} / ${zhText(state.visionResult.action)}`,
      icon: "fa-eye",
    },
    {
      label: "AGV 任务",
      value: selectedAgv.value?.id || "待分配",
      detail: zhText(selectedAgv.value?.task || "暂无搬运任务"),
      icon: "fa-robot",
    },
    {
      label: "交接位置",
      value: zhText(route.value.handoffZone),
      detail: `${route.value.remainingMeters}m / ${outboundGate.value?.gateId || "出场闸机"}`,
      icon: "fa-location-dot",
    },
  ];
});

function selectOrder(orderNo) {
  selectedOrderNo.value = orderNo;
}

async function submitRetrieve() {
  if (!selectedOrder.value) return;
  await runOwnerAction("retrieve", selectedOrder.value.orderNo);
}

async function submitTouch() {
  if (!selectedOrder.value) return;
  await runOwnerAction("touch", selectedOrder.value.orderNo);
}

async function submitVip() {
  if (!selectedOrder.value) return;
  await enqueueVip(selectedOrder.value.orderNo);
}
</script>

<template>
  <section class="operations-workbench">
    <article class="surface ops-toolbar">
      <div>
        <h2>车辆 / 订单检索</h2>
        <p>选中一辆车，即可在右侧看到它从入场到取车放行的完整履约链路。</p>
      </div>
      <div class="ops-filters">
        <div class="search-field">
          <i class="fa-solid fa-magnifying-glass"></i>
          <input v-model="keyword" placeholder="搜索车牌、订单号或车位" />
        </div>
        <select v-model="statusFilter" class="panel-select">
          <option v-for="[value, label] in statusOptions" :key="value" :value="value">{{ label }}</option>
        </select>
        <button class="ghost-button small" :disabled="state.busy.vision" @click="runVision()">
          <i class="fa-solid" :class="state.busy.vision ? 'fa-spinner fa-spin' : 'fa-camera'"></i>
          识别刷新
        </button>
        <button class="primary-button small" :disabled="state.busy.preDispatch" @click="triggerPreDispatch()">
          <i class="fa-solid fa-forward-fast"></i>
          高峰预调度
        </button>
      </div>
    </article>

    <section class="ops-layout">
      <aside class="surface order-list-panel">
        <div class="section-head compact">
          <div>
            <h2>订单列表</h2>
            <p>{{ filteredOrders.length }} 条匹配结果</p>
          </div>
        </div>
        <div class="order-list">
          <button
            v-for="order in filteredOrders"
            :key="order.orderNo"
            class="order-row"
            :class="{ active: selectedOrder?.orderNo === order.orderNo }"
            @click="selectOrder(order.orderNo)"
          >
            <span class="plate">{{ order.plateNo }}</span>
            <b>{{ zhText(order.status) }}</b>
            <small>{{ order.orderNo }} / {{ order.slotId }}</small>
          </button>
        </div>
      </aside>

      <article class="surface order-detail-panel" v-if="selectedOrder">
        <div class="order-title-row">
          <div>
            <span class="eyebrow-label">当前选中订单</span>
            <h2>{{ selectedOrder.plateNo }}</h2>
            <p>{{ selectedOrder.orderNo }} / 车位 {{ selectedOrder.slotId }} / 已停 {{ elapsedLabel }}</p>
          </div>
          <span class="status-pill" :class="selectedOrder.status === 'FINISHED' ? 'stable' : ''">
            {{ zhText(selectedOrder.status) }}
          </span>
        </div>

        <div class="trace-card-grid">
          <div v-for="card in traceCards" :key="card.label" class="trace-card">
            <i class="fa-solid" :class="card.icon"></i>
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
            <small>{{ card.detail }}</small>
          </div>
        </div>

        <div class="ops-detail-grid">
          <section>
            <div class="section-head compact">
              <div>
                <h2>履约进度</h2>
                <p>当前车辆的实时履约节点。</p>
              </div>
            </div>
            <div class="fulfillment-timeline">
              <div class="done">
                <b>入场识别</b>
                <span>{{ activeCamera?.cameraId || "摄像头" }} 识别到 {{ selectedOrder.plateNo }}</span>
              </div>
              <div :class="{ done: selectedOrder.status !== 'PARKED', active: selectedOrder.status === 'PARKED' }">
                <b>库内存放</b>
                <span>车辆当前绑定 {{ selectedOrder.slotId }}，AGV 坐标同步至数字孪生。</span>
              </div>
              <div :class="{ done: ['RETRIEVING', 'TOUCHING', 'FINISHED'].includes(selectedOrder.status), active: ['RETRIEVING', 'TOUCHING'].includes(selectedOrder.status) }">
                <b>机器人履约</b>
                <span>{{ selectedAgv?.id || "AGV" }}：{{ zhText(selectedAgv?.task || "等待调度") }}</span>
              </div>
              <div :class="{ done: selectedOrder.status === 'FINISHED', active: selectedOrder.status === 'TOUCHING' }">
                <b>交接 / 放行</b>
                <span>{{ zhText(route.handoffZone) }}，目标闸机 {{ route.targetGate }}，剩余 {{ route.remainingMeters }}m。</span>
              </div>
            </div>
          </section>

          <section>
            <div class="section-head compact">
              <div>
                <h2>员工操作</h2>
                <p>直接对当前订单发起处理动作。</p>
              </div>
            </div>
            <div class="action-grid">
              <button class="primary-button" :disabled="state.busy.ownerAction" @click="submitRetrieve">
                <i class="fa-solid fa-truck-ramp-box"></i>
                发起取车
              </button>
              <button class="ghost-button" :disabled="state.busy.ownerAction" @click="submitTouch">
                <i class="fa-solid fa-box-open"></i>
                临停取物
              </button>
              <button class="ghost-button" :disabled="state.busy.ownerAction" @click="submitVip">
                <i class="fa-solid fa-bolt-lightning"></i>
                VIP 优先
              </button>
            </div>
          </section>
        </div>

        <div class="ops-related-grid">
          <section class="ops-subcard">
            <div class="section-head compact">
              <div>
                <h2>相关队列</h2>
                <p>当前车辆相关的调度任务。</p>
              </div>
            </div>
            <div class="queue-list">
              <div v-for="(task, index) in selectedQueue" :key="task.id ?? `${task.plateNo}-${index}`" class="queue-item" :class="{ vip: task.vip }">
                <div class="queue-rank">{{ index + 1 }}</div>
                <div class="queue-body">
                  <b>{{ zhText(task.type) }} - {{ task.plateNo }}</b>
                  <span>{{ taskStatusText(task) }} · {{ zhText(task.tag) }}<template v-if="task.slotId"> · 车位 {{ task.slotId }}</template><template v-if="task.agvId"> · {{ task.agvId }}</template></span>
                  <div v-if="task.status === 'IN_PROGRESS'" class="queue-progress">
                    <i :style="{ width: (task.progress || 0) + '%' }"></i>
                  </div>
                </div>
                <span class="queue-tag" :class="taskTone(task)">{{ taskBadge(task) }}</span>
              </div>
            </div>
          </section>

          <section class="ops-subcard">
            <div class="section-head compact">
              <div>
                <h2>费用追溯</h2>
                <p>当前车辆的费用构成。</p>
              </div>
              <strong class="amount-total">{{ zhMoney(selectedOrder.amount || state.pricingPreview.totalAmount) }}</strong>
            </div>
            <div class="billing-list">
              <div v-for="item in billingRows" :key="item.label">
                <span>{{ zhText(item.label) }}</span>
                <b>{{ zhMoney(item.amount) }}</b>
                <small>{{ zhText(item.formula) }}</small>
              </div>
            </div>
          </section>

          <section class="ops-subcard">
            <div class="section-head compact">
              <div>
                <h2>相关事件</h2>
                <p>与当前车辆关联的设备与业务记录。</p>
              </div>
            </div>
            <div class="queue-list">
              <div v-for="event in relatedEvents" :key="event.eventId" class="queue-item event-proof">
                <div>
                  <b>{{ zhText(event.eventCode) }} - {{ event.deviceId }}</b>
                  <span>{{ zhText(event.message) }}</span>
                </div>
                <span class="status-pill" :class="event.severity === 'critical' || event.severity === 'high' ? 'warning' : 'stable'">
                  {{ zhText(event.severity) }}
                </span>
              </div>
              <div v-if="!relatedEvents.length" class="empty-proof">当前车辆暂无新的关联记录。</div>
            </div>
          </section>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.operations-workbench {
  display: grid;
  gap: 20px;
}

.ops-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.ops-toolbar h2 {
  margin-bottom: 6px;
}

.ops-toolbar p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.ops-filters {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-field {
  min-width: 300px;
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: #fff;
}

.search-field i {
  color: var(--text-muted);
}

.search-field input {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--text-main);
}

.ops-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 20px;
}

.order-list-panel {
  min-height: 620px;
}

.order-list {
  display: grid;
  gap: 10px;
  max-height: 560px;
  overflow: auto;
  padding-right: 4px;
}

.order-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 5px 10px;
  padding: 14px;
  text-align: left;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: linear-gradient(180deg, #fff, #f8fafc);
}

.order-row.active {
  border-color: rgba(79, 70, 229, 0.36);
  background: rgba(79, 70, 229, 0.07);
  box-shadow: inset 3px 0 0 var(--brand);
}

.order-row .plate {
  position: static;
  transform: none;
  padding: 0;
  color: var(--text-main);
  background: transparent;
  border: 0;
  box-shadow: none;
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0;
}

.order-row b {
  color: var(--brand);
  font-size: 12px;
}

.order-row small {
  grid-column: 1 / -1;
  color: var(--text-muted);
  font-size: 12px;
}

.order-title-row {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.eyebrow-label {
  color: var(--brand);
  font-size: 12px;
  font-weight: 800;
}

.order-title-row h2 {
  margin: 5px 0;
  font-size: 32px;
}

.order-title-row p {
  margin: 0;
  color: var(--text-muted);
}

.trace-card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.trace-card {
  min-height: 138px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: linear-gradient(180deg, #fff, #f8fafc);
}

.trace-card i {
  color: var(--brand);
  margin-bottom: 12px;
}

.trace-card span,
.trace-card small,
.billing-list span,
.billing-list small {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}

.trace-card strong {
  display: block;
  margin: 6px 0;
  color: var(--text-main);
  font-family: "Outfit", "Noto Sans SC", sans-serif;
  font-size: 18px;
}

.ops-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(240px, 0.6fr);
  gap: 20px;
}

.fulfillment-timeline {
  display: grid;
  gap: 10px;
}

.fulfillment-timeline div {
  position: relative;
  padding: 14px 14px 14px 18px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: #fff;
}

.fulfillment-timeline div::before {
  content: "";
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 4px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.12);
}

.fulfillment-timeline div.done::before {
  background: var(--safety-green);
}

.fulfillment-timeline div.active::before {
  background: var(--brand);
}

.fulfillment-timeline b,
.fulfillment-timeline span {
  display: block;
}

.fulfillment-timeline b {
  color: var(--text-main);
}

.fulfillment-timeline span {
  margin-top: 5px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
}

.action-grid {
  display: grid;
  gap: 10px;
}

.ops-related-grid {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.8fr) minmax(0, 1fr);
  gap: 20px;
}

.ops-subcard {
  min-width: 0;
}

.amount-total {
  color: var(--brand);
  font-family: "Outfit", sans-serif;
  font-size: 24px;
}

.billing-list {
  display: grid;
  gap: 10px;
}

.billing-list div {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 12px;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: linear-gradient(180deg, #fff, #f8fafc);
}

.billing-list b {
  color: var(--text-main);
  font-family: "Outfit", sans-serif;
}

.billing-list small {
  grid-column: 1 / -1;
}

.event-proof {
  grid-template-columns: minmax(0, 1fr) auto;
}

.empty-proof {
  padding: 16px;
  border-radius: 12px;
  color: var(--text-muted);
  background: rgba(15, 23, 42, 0.03);
  border: 1px dashed var(--border-color);
  font-size: 13px;
}

@media (max-width: 1380px) {
  .ops-toolbar,
  .order-title-row {
    align-items: stretch;
    flex-direction: column;
  }

  .ops-filters {
    justify-content: flex-start;
  }

  .trace-card-grid,
  .ops-related-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .ops-layout,
  .ops-detail-grid,
  .trace-card-grid,
  .ops-related-grid {
    grid-template-columns: 1fr;
  }

  .search-field {
    min-width: min(300px, 100%);
  }
}

.queue-body {
  flex: 1;
  min-width: 0;
}

.queue-progress {
  margin-top: 6px;
  height: 5px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.queue-progress i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--brand), var(--brand-2));
  transition: width 0.5s ease;
}

.queue-tag.active {
  color: var(--brand);
  background: rgba(79, 70, 229, 0.12);
}

.queue-tag.done {
  color: var(--safety-green, #16a34a);
  background: rgba(22, 163, 74, 0.12);
}
</style>
