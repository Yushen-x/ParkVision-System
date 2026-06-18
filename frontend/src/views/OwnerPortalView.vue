<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute as useVueRoute, useRouter } from "vue-router";
import {
  cancelReservation,
  createReservation,
  enqueueVip,
  fulfillReservation,
  getters,
  loadOwnerData,
  logout,
  ownerEntry,
  rechargeWallet,
  runOwnerAction,
  state,
} from "../stores/parkingStore";
import { useNow } from "../composables/useNow";
import { ENERGY_EV, ENERGY_FUEL, isEvEnergyType, normalizeEnergyType } from "../utils/energyType";
import { zhMoney, zhText } from "../utils/localize";
import { aiChat, aiStatusLabel } from "../services/aiClient";

// Strictly the signed-in owner's own active order (pulled from /api/owner).
const currentOrder = computed(
  () => getters.ownerActiveOrder.value || state.owner.orders[0] || null,
);
const myVehicles = computed(() => state.owner.vehicles);
const history = computed(() => getters.ownerHistory.value);
const hasActiveOrder = computed(() => Boolean(getters.ownerActiveOrder.value));
const checkInError = ref("");

async function doCheckIn(plate) {
  checkInError.value = "";
  const result = await ownerEntry(plate);
  if (!result.ok) checkInError.value = result.error || "入场失败";
}
const pageRoute = useVueRoute();
const router = useRouter();
const now = useNow();
const validTabs = ["vehicle", "wallet", "navigation", "reserve", "assistant"];
const activeTab = ref(validTabs.includes(pageRoute.query.tab) ? pageRoute.query.tab : "vehicle");
const ownerName = computed(() => state.auth.user?.displayName || state.auth.user?.username || "车主");

function doLogout() {
  logout();
  router.replace({ name: "login" });
}

const tabTitle = computed(() => {
  switch (activeTab.value) {
    case "wallet":
      return "我的钱包";
    case "navigation":
      return "室内导航";
    case "reserve":
      return "车位预约";
    case "assistant":
      return "智能助手";
    default:
      return "我的车辆";
  }
});

const wallet = computed(() => state.owner.wallet);
const walletBalance = computed(() => Number(state.owner.wallet?.balance ?? state.owner.profile?.balance ?? 0));
const discountPercent = computed(() => {
  const rate = Number(state.owner.wallet?.discountRate ?? 1);
  return rate < 1 ? Math.round((1 - rate) * 100) : 0;
});
const walletTransactions = computed(() => state.owner.wallet?.transactions || []);
const rechargeAmount = ref(50);
const rechargeBusy = ref(false);
const walletNotice = ref("");
const walletError = ref("");
const actionError = ref("");
const vipNotice = ref("");
const vipError = ref("");

const txLabels = { WALLET: "停车扣款", CASH: "现金结算", RECHARGE: "钱包充值", AUTO_SETTLEMENT: "自动结算" };

async function doRecharge() {
  walletError.value = "";
  walletNotice.value = "";
  const amount = Number(rechargeAmount.value);
  if (!amount || amount <= 0) {
    walletError.value = "请输入有效的充值金额";
    return;
  }
  rechargeBusy.value = true;
  const result = await rechargeWallet(amount);
  rechargeBusy.value = false;
  if (result.ok) walletNotice.value = `充值 ${amount.toFixed(2)} 元成功`;
  else walletError.value = result.error || "充值失败";
}
const showOverlay = ref(false);
const timer = ref(180);
let timerId = null;

function clearTouchTimer() {
  if (timerId) {
    clearInterval(timerId);
    timerId = null;
  }
}

onBeforeUnmount(clearTouchTimer);

onMounted(() => {
  void loadOwnerData();
});

function formatOrderTime(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}

const route = computed(() => state.indoorRoute);
const leadGate = computed(() => state.devices.gates.find((gate) => gate.gateId === route.value.targetGate) || state.devices.gates[0]);
const progressSegments = computed(() => {
  const completed = Math.max(0, Math.min(3, Number(route.value.completedSegments || 0)));
  return [0, 1, 2].map((index) => index < completed);
});

const ownerStatus = computed(() => {
  const status = currentOrder.value?.status;
  switch (status) {
    case "PARKED":
      return "已入库";
    case "RETRIEVING":
      return "取车中";
    case "TOUCHING":
      return "取物中";
    case "FINISHED":
      return "已关闭";
    default:
      return "待命";
  }
});

const isRetrieving = computed(() => ["RETRIEVING", "TOUCHING"].includes(currentOrder.value?.status));

async function retrieveFromNav() {
  if (!currentOrder.value || isRetrieving.value) return;
  await doAction("retrieve");
}

const duration = computed(() => {
  if (!currentOrder.value?.entryTime) return "00:00";
  const diff = Math.max(0, now.value - new Date(currentOrder.value.entryTime).getTime());
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(minutes / 60)
    .toString()
    .padStart(2, "0");
  const remain = String(minutes % 60).padStart(2, "0");
  return `${hours}:${remain}`;
});

const fee = computed(() => zhMoney(currentOrder.value?.amount || state.pricingPreview.totalAmount || 0));
const plate = computed(() => currentOrder.value?.plateNo || route.value.plateNo || "暂无活跃订单");
const slotLabel = computed(() => currentOrder.value?.slotId || route.value.slotId || "暂无");
const etaLabel = computed(() => `${Math.floor((route.value.etaSeconds || 0) / 60)} 分钟`);
const agvLabel = computed(() => `${Math.max(1, Math.round((route.value.agvEtaSeconds || 0) / 60))} 分钟`);

async function doAction(action) {
  if (!currentOrder.value) return;

  if (action === "touch") {
    showOverlay.value = true;
    timer.value = 180;
    clearTouchTimer();
    timerId = window.setInterval(() => {
      timer.value -= 1;
      if (timer.value <= 0) {
        clearTouchTimer();
      }
    }, 1000);
  }

  actionError.value = "";
  const result = await runOwnerAction(action, currentOrder.value.orderNo);
  if (result && !result.ok) {
    actionError.value = result.error || "操作失败";
    if (action === "touch") {
      showOverlay.value = false;
      clearTouchTimer();
    }
  }
}

async function doVip() {
  if (!currentOrder.value || state.busy.ownerAction) return;
  vipNotice.value = "";
  vipError.value = "";
  const result = await enqueueVip(currentOrder.value.orderNo);
  if (result.ok) {
    vipNotice.value =
      result.mode === "local"
        ? `${result.plateNo || currentOrder.value.plateNo} 的 VIP 任务已在本地入队。`
        : `${result.plateNo || currentOrder.value.plateNo} 已插入 AGV 队首，优先取车已生效。`;
  } else {
    vipError.value = result.error || "VIP 优先取车失败";
  }
}

async function finishTouch() {
  showOverlay.value = false;
  clearTouchTimer();
  if (!currentOrder.value || currentOrder.value.status !== "TOUCHING") return;

  actionError.value = "";
  const result = await runOwnerAction("park", currentOrder.value.orderNo);
  if (result && !result.ok) {
    actionError.value = result.error || "结束临停取物失败";
  }
}

function formatTime(seconds) {
  const minutes = Math.floor(seconds / 60)
    .toString()
    .padStart(2, "0");
  const remain = String(seconds % 60).padStart(2, "0");
  return `${minutes}:${remain}`;
}

// --- AI 车主助手 ---------------------------------------------------------
const chatInput = ref("");
const chatBusy = ref(false);
const chatScroll = ref(null);
const aiStatus = computed(() => aiStatusLabel());
const chatMessages = ref([
  { role: "assistant", content: "您好，我是 ParkVision 智能助手。可以帮您取车、临停取物、查询空位与费用、申请 VIP 优先取车，请问需要什么？" },
]);
const quickAsks = ["我要取车", "现在停车费多少？", "还有空位吗？", "帮我 VIP 优先取车"];

const chatContext = computed(() => ({
  freeCount: getters.freeCount.value,
  totalSlots: state.slots.length,
  plate: currentOrder.value?.plateNo || route.value.plateNo || "您的车辆",
  orderNo: currentOrder.value?.orderNo || route.value.orderNo,
  fee: currentOrder.value?.amount || state.pricingPreview.totalAmount || 0,
}));

function chatSystemPrompt() {
  const c = chatContext.value;
  return (
    "你是 ParkVision 智能停车助手，服务于一个三环立体（回转塔）车库。" +
    "可以帮助车主取车、临停取物、申请 VIP 优先取车、查询空闲车位与停车费用。" +
    `当前车库状态：空闲车位 ${c.freeCount}/${c.totalSlots}，当前订单 ${c.orderNo || "无"}，车牌 ${c.plate}，预估费用 ￥${Number(c.fee).toFixed(2)}。` +
    "请用简洁友好的中文回答，通常不超过 3 句话。"
  );
}

async function scrollChatToEnd() {
  await nextTick();
  if (chatScroll.value) chatScroll.value.scrollTop = chatScroll.value.scrollHeight;
}

async function sendChat(text) {
  const content = (text ?? chatInput.value).trim();
  if (!content || chatBusy.value) return;
  chatInput.value = "";
  chatMessages.value.push({ role: "user", content });
  chatBusy.value = true;
  await scrollChatToEnd();

  const history = chatMessages.value.filter((m) => m.role === "user" || m.role === "assistant").slice(-8);
  const { text: reply, source } = await aiChat({
    system: chatSystemPrompt(),
    messages: history,
    context: chatContext.value,
  });

  chatMessages.value.push({ role: "assistant", content: reply, source });
  chatBusy.value = false;
  await scrollChatToEnd();

  // 助手识别到明确意图后，联动对应的真实业务动作，形成闭环。
  maybeRunIntent(content);
}

function maybeRunIntent(text) {
  if (!currentOrder.value) return;
  if (/取车|提车|出库/.test(text) && !/临停|取物/.test(text)) {
    void doAction("retrieve");
  } else if (/临停|取物/.test(text)) {
    void doAction("touch");
  } else if (/VIP|优先|插队|加急/i.test(text)) {
    void doVip();
  }
}

// --- 车位预约闭环 ---------------------------------------------------------
const resForm = reactive({ plateNo: "", phone: "", energyType: ENERGY_FUEL });
const reservations = computed(() => state.reservations.slice(0, 5));
const resError = ref("");

function syncReservationForm() {
  const vehicle = myVehicles.value[0];
  if (!vehicle?.plateNo) return;
  resForm.plateNo = vehicle.plateNo;
  resForm.energyType = normalizeEnergyType(vehicle.energyType, ENERGY_FUEL);
}

watch(myVehicles, syncReservationForm, { immediate: true });

async function submitReservation() {
  resError.value = "";
  const result = await createReservation({ ...resForm });
  if (result.ok) {
    resForm.phone = "";
    syncReservationForm();
  } else {
    resError.value = result.error || "预约失败";
  }
}

function resHint(reservation) {
  const stamp = reservation.status === "HELD" ? reservation.expiresAt : reservation.createdAt;
  const d = new Date(stamp);
  const time = `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  return reservation.status === "HELD" ? `保留至 ${time}` : time;
}
</script>

<template>
  <div class="owner-app">
    <header class="owner-topbar">
      <div class="owner-brand">
        <div class="owner-brand-mark">PV</div>
        <div>
          <strong>ParkVision 车主端</strong>
          <span>立体车库 · 智能取还车</span>
        </div>
      </div>
      <div class="owner-topbar-right">
        <span class="owner-greeting"><i class="fa-solid fa-circle-user"></i> {{ ownerName }}</span>
        <button class="owner-logout" @click="doLogout"><i class="fa-solid fa-arrow-right-from-bracket"></i> 退出</button>
      </div>
    </header>

    <section class="owner-stage">
      <div class="phone-frame">
        <div class="phone-status">
          <span>ParkVision</span>
          <span><i class="fa-solid fa-wifi"></i> 5G <i class="fa-solid fa-battery-full"></i></span>
        </div>

        <div class="phone-screen">
          <div class="c-app-header">
            <div class="c-header-top">
              <span class="c-plate">{{ plate }}</span>
              <i class="fa-regular fa-bell"></i>
            </div>
            <h2>{{ tabTitle }}</h2>
            <div class="phone-tabs five">
              <button :class="{ active: activeTab === 'vehicle' }" @click="activeTab = 'vehicle'">
                <i class="fa-solid fa-car"></i>
                车辆
              </button>
              <button :class="{ active: activeTab === 'wallet' }" @click="activeTab = 'wallet'">
                <i class="fa-solid fa-wallet"></i>
                钱包
              </button>
              <button :class="{ active: activeTab === 'navigation' }" @click="activeTab = 'navigation'">
                <i class="fa-solid fa-location-arrow"></i>
                导航
              </button>
              <button :class="{ active: activeTab === 'reserve' }" @click="activeTab = 'reserve'">
                <i class="fa-solid fa-calendar-check"></i>
                预约
              </button>
              <button :class="{ active: activeTab === 'assistant' }" @click="activeTab = 'assistant'">
                <i class="fa-solid fa-robot"></i>
                助手
              </button>
            </div>
          </div>

        <template v-if="activeTab === 'vehicle'">
          <template v-if="hasActiveOrder">
            <div class="c-status-card">
              <div class="c-status-indicator">
                <div class="c-pulse-ring"></div>
                <div class="c-inner-circle">{{ ownerStatus }}</div>
              </div>
              <div class="c-location"><i class="fa-solid fa-location-dot"></i> 车位 {{ slotLabel }} | {{ zhText(route.handoffZone) }}</div>
              <div class="c-info-grid">
                <div class="c-info-item"><span>停车时长</span><strong>{{ duration }}</strong></div>
                <div class="c-info-item"><span>当前费用</span><strong>{{ fee }}</strong></div>
              </div>
            </div>

            <div class="c-actions">
              <button class="c-btn c-btn-primary" :disabled="state.busy.ownerAction" @click="doAction('retrieve')">
                <i class="fa-solid fa-truck-ramp-box"></i>
                取车
              </button>
              <button class="c-btn c-btn-secondary" :disabled="state.busy.ownerAction" @click="doAction('touch')">
                <i class="fa-solid fa-box-open"></i>
                临停取物
                <span class="c-badge">不结单</span>
              </button>
            </div>

            <div class="c-vip-card" :class="{ disabled: state.busy.ownerAction || !hasActiveOrder }" @click="doVip">
              <div class="c-vip-icon"><i class="fa-solid fa-bolt-lightning"></i></div>
              <div class="c-vip-text">
                <h4>VIP 优先取车</h4>
                <p>插入 AGV 队首，预计 {{ agvLabel }} 到交接区。</p>
              </div>
              <div class="c-vip-price">+￥5</div>
            </div>
            <p v-if="vipNotice" class="wallet-ok"><i class="fa-solid fa-circle-check"></i> {{ vipNotice }}</p>
            <p v-if="vipError" class="checkin-error"><i class="fa-solid fa-circle-exclamation"></i> {{ vipError }}</p>

            <div class="phone-secondary-action">
              <button class="ghost-button" :disabled="state.busy.ownerAction" @click="doAction('pay')">
                立即支付并关闭订单
              </button>
              <small class="pay-hint">钱包余额 ￥{{ walletBalance.toFixed(2) }}<span v-if="discountPercent">（会员 {{ discountPercent }}% 折扣）</span></small>
            </div>
            <p v-if="actionError" class="checkin-error"><i class="fa-solid fa-circle-exclamation"></i> {{ actionError }}</p>
          </template>

          <template v-else>
            <div class="checkin-pane">
              <div class="checkin-intro">
                <h3>我的车辆</h3>
                <p>当前没有在场车辆。到达车库后选择车辆“我已到场”，系统会分配车位并开始计费。</p>
              </div>
              <p v-if="checkInError" class="checkin-error"><i class="fa-solid fa-circle-exclamation"></i> {{ checkInError }}</p>
              <div v-if="myVehicles.length" class="checkin-list">
                <div v-for="v in myVehicles" :key="v.plateNo" class="checkin-item">
                  <div class="checkin-meta">
                    <b>{{ v.plateNo }}</b>
                    <span>{{ isEvEnergyType(v.energyType) ? '新能源' : '燃油' }} · {{ v.membershipType || '临时' }}</span>
                  </div>
                  <button class="c-btn c-btn-primary checkin-btn" :disabled="state.busy.entry" @click="doCheckIn(v.plateNo)">
                    <i class="fa-solid fa-right-to-bracket"></i> 我已到场
                  </button>
                </div>
              </div>
              <p v-else class="checkin-empty">未绑定车辆，请联系管理员添加车辆档案。</p>

              <div v-if="history.length" class="checkin-history">
                <h4>历史订单</h4>
                <div v-for="o in history.slice(0, 6)" :key="o.orderNo" class="history-row">
                  <span>{{ o.plateNo }} · {{ o.slotId }} · {{ formatOrderTime(o.entryTime) }}</span>
                  <b>{{ zhMoney(o.amount || 0) }}</b>
                </div>
              </div>
            </div>
          </template>
        </template>

        <template v-else-if="activeTab === 'wallet'">
          <div class="wallet-pane">
            <div class="wallet-card">
              <span class="wallet-label">钱包余额</span>
              <strong class="wallet-balance">￥{{ walletBalance.toFixed(2) }}</strong>
              <div class="wallet-meta">
                <span>{{ wallet?.memberLevel || state.owner.profile?.memberLevel || "标准会员" }}</span>
                <span v-if="discountPercent" class="wallet-discount">会员 {{ discountPercent }}% 折扣</span>
              </div>
            </div>

            <div class="wallet-recharge">
              <h4>充值</h4>
              <div class="recharge-presets">
                <button v-for="amt in [20, 50, 100, 200]" :key="amt"
                  :class="{ active: Number(rechargeAmount) === amt }" @click="rechargeAmount = amt">
                  ￥{{ amt }}
                </button>
              </div>
              <div class="recharge-row">
                <input v-model.number="rechargeAmount" type="number" min="1" step="1" placeholder="自定义金额" />
                <button class="c-btn c-btn-primary" :disabled="rechargeBusy" @click="doRecharge">
                  {{ rechargeBusy ? "充值中…" : "确认充值" }}
                </button>
              </div>
              <p v-if="walletNotice" class="wallet-ok"><i class="fa-solid fa-circle-check"></i> {{ walletNotice }}</p>
              <p v-if="walletError" class="checkin-error"><i class="fa-solid fa-circle-exclamation"></i> {{ walletError }}</p>
            </div>

            <div class="wallet-tx">
              <h4>交易明细</h4>
              <div v-if="walletTransactions.length" class="tx-list">
                <div v-for="tx in walletTransactions" :key="tx.paymentNo" class="tx-row">
                  <div class="tx-meta">
                    <b>{{ txLabels[tx.method] || tx.method }}</b>
                    <small>{{ tx.orderNo }} · {{ new Date(tx.paidAt).toLocaleString("zh-CN") }}</small>
                  </div>
                  <strong :class="tx.method === 'RECHARGE' ? 'tx-in' : 'tx-out'">
                    {{ tx.method === 'RECHARGE' ? '+' : '-' }}￥{{ Number(tx.amount).toFixed(2) }}
                  </strong>
                </div>
              </div>
              <p v-else class="checkin-empty">暂无交易记录。</p>
            </div>
          </div>
        </template>

        <template v-else-if="activeTab === 'navigation'">
          <div class="nav-order-banner">
            <div>
              <span>当前订单 {{ currentOrder?.orderNo || "—" }}</span>
              <b>{{ ownerStatus }} · 车位 {{ slotLabel }}</b>
            </div>
            <button
              v-if="!isRetrieving"
              class="nav-cta"
              :disabled="state.busy.ownerAction || !currentOrder"
              @click="retrieveFromNav"
            >
              <i class="fa-solid fa-truck-ramp-box"></i> 发起取车
            </button>
            <span v-else class="nav-live"><i class="fa-solid fa-circle"></i> 接车中</span>
          </div>

          <div class="nav-summary">
            <span>{{ isRetrieving ? "距" : "" }}{{ zhText(route.handoffZone) }}{{ isRetrieving ? "还剩" : "全程" }}</span>
            <strong>{{ route.remainingMeters }}m</strong>
            <small>{{ isRetrieving ? `车主 ${etaLabel}到达，AGV ${agvLabel}到达` : "发起取车后将为你规划接车路线" }}</small>
          </div>

          <div class="phone-map">
            <svg viewBox="0 0 320 380" aria-hidden="true">
              <path class="map-bg-path" d="M 48 328 C 48 245 82 220 130 212 C 188 202 200 152 242 72" />
              <path class="map-active-path" d="M 48 328 C 48 245 82 220 130 212 C 188 202 200 152 242 72" />
            </svg>
            <div class="map-node car-node"><i class="fa-solid fa-person-walking"></i></div>
            <div class="map-node lift-node"><i class="fa-solid fa-elevator"></i></div>
            <div class="map-node gate-node" :class="{ danger: leadGate?.estopArmed }">
              <i class="fa-solid" :class="leadGate?.estopArmed ? 'fa-triangle-exclamation' : 'fa-flag-checkered'"></i>
            </div>
            <span class="map-label start">当前位置</span>
            <span class="map-label end">{{ zhText(route.handoffZone) }}</span>
          </div>

          <div class="nav-instruction">
            <div>
              <b>{{ zhText(route.nextInstruction) }}</b>
              <span>{{ zhText(route.safetyMessage) }}</span>
            </div>
            <strong>{{ route.walkingSpeedKph }} km/h</strong>
          </div>
          <div class="nav-progress">
            <i v-for="(filled, index) in progressSegments" :key="index" :class="{ filled }"></i>
          </div>
        </template>

        <template v-else-if="activeTab === 'reserve'">
          <div class="reserve-pane">
            <div class="reserve-intro">
              <h3>预约车位</h3>
              <p>锁定一个空闲车位（保留 15 分钟），到场后一键转为正式停车订单，立体库同步入库。</p>
            </div>
            <form class="res-form-m" @submit.prevent="submitReservation">
              <input v-model.trim="resForm.plateNo" type="text" placeholder="车牌，如 沪A12345" />
              <input v-model.trim="resForm.phone" type="text" placeholder="手机号（可选）" />
              <select v-model="resForm.energyType">
                <option :value="ENERGY_FUEL">燃油车</option>
                <option :value="ENERGY_EV">新能源</option>
              </select>
              <button class="c-btn c-btn-primary" type="submit"><i class="fa-solid fa-lock"></i> 预约锁位</button>
            </form>
            <p v-if="resError" class="checkin-error"><i class="fa-solid fa-circle-exclamation"></i> {{ resError }}</p>
            <div v-if="reservations.length" class="res-list-m">
              <div v-for="r in reservations" :key="r.id" class="res-item-m">
                <div class="res-meta">
                  <b>{{ r.plateNo }} · 车位 {{ r.slotId }}</b>
                  <span>{{ resHint(r) }}</span>
                </div>
                <div v-if="r.status === 'HELD'" class="res-actions-m">
                  <button class="res-fulfill" @click="fulfillReservation(r.id)">确认到场</button>
                  <button class="res-cancel" @click="cancelReservation(r.id)">取消</button>
                </div>
                <span v-else class="res-status" :class="r.status === 'FULFILLED' ? 'ok' : 'warn'">{{ zhText(r.status) }}</span>
              </div>
            </div>
            <p v-else class="res-empty-m">暂无预约记录。</p>
          </div>
        </template>

        <template v-else>
          <div class="chat-status"><i class="fa-solid fa-circle"></i> {{ aiStatus }}</div>
          <div ref="chatScroll" class="chat-scroll">
            <div
              v-for="(msg, index) in chatMessages"
              :key="index"
              class="chat-bubble"
              :class="msg.role"
            >
              <p>{{ msg.content }}</p>
            </div>
            <div v-if="chatBusy" class="chat-bubble assistant typing">
              <span></span><span></span><span></span>
            </div>
          </div>
          <div class="chat-quick">
            <button v-for="q in quickAsks" :key="q" :disabled="chatBusy" @click="sendChat(q)">{{ q }}</button>
          </div>
          <form class="chat-input" @submit.prevent="sendChat()">
            <input v-model="chatInput" type="text" placeholder="问问助手，或直接说“取车”…" :disabled="chatBusy" />
            <button type="submit" :disabled="chatBusy || !chatInput.trim()">
              <i class="fa-solid" :class="chatBusy ? 'fa-spinner fa-spin' : 'fa-paper-plane'"></i>
            </button>
          </form>
        </template>

        <div v-if="showOverlay" class="touch-overlay">
          <div>
            <h3>临停取物已开启</h3>
            <p>车辆会停在交接区，取物期间订单和计费保持开启。</p>
            <strong>{{ formatTime(timer) }}</strong>
            <button class="primary-button full" :disabled="state.busy.ownerAction" @click="finishTouch">
              {{ state.busy.ownerAction ? "回库中…" : "完成取物并回库" }}
            </button>
          </div>
        </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.owner-app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at 12% -5%, rgba(99, 102, 241, 0.14), transparent 38%),
    linear-gradient(180deg, #eef2ff, #f1f5f9);
}

.owner-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 28px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 5;
}

.owner-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.owner-brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  color: #fff;
  font-family: "Outfit", sans-serif;
  font-weight: 800;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
}

.owner-brand strong {
  display: block;
  color: var(--text-main);
  font-size: 16px;
}

.owner-brand span {
  color: var(--text-muted);
  font-size: 12px;
}

.owner-topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.owner-greeting {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 600;
}

.owner-greeting i {
  color: var(--brand);
  margin-right: 5px;
}

.owner-logout {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-main);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.owner-logout:hover {
  color: var(--danger-red);
  border-color: rgba(239, 68, 68, 0.4);
}

.owner-stage {
  flex: 1;
  display: grid;
  place-items: center;
  padding: 28px 16px 40px;
  overflow-y: auto;
}

.reserve-pane {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.reserve-intro h3 {
  margin: 0 0 4px;
  color: var(--text-main);
  font-size: 17px;
}

.reserve-intro p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.res-form-m {
  display: grid;
  gap: 10px;
}

.res-form-m input,
.res-form-m select {
  height: 42px;
  padding: 0 14px;
  border-radius: 11px;
  border: 1px solid var(--border-color);
  background: #fff;
  font-size: 14px;
  color: var(--text-main);
  outline: none;
}

.res-form-m input:focus,
.res-form-m select:focus {
  border-color: var(--brand);
}

.res-list-m {
  display: grid;
  gap: 10px;
}

.res-item-m {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--border-color);
}

.res-actions-m {
  display: flex;
  gap: 8px;
}

.res-fulfill {
  padding: 8px 12px;
  border: none;
  border-radius: 9px;
  color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.res-cancel {
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: 9px;
  background: #fff;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
}

.res-status {
  font-size: 12px;
  font-weight: 700;
  padding: 5px 10px;
  border-radius: 999px;
}

.res-status.ok {
  color: var(--safety-green);
  background: rgba(16, 185, 129, 0.12);
}

.res-status.warn {
  color: var(--warning-yellow);
  background: rgba(245, 158, 11, 0.12);
}

.res-empty-m {
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
  padding: 24px 0;
}

.checkin-pane {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.checkin-intro h3 {
  margin: 0 0 4px;
  color: var(--text-main);
  font-size: 17px;
}

.checkin-intro p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.checkin-error {
  margin: 0;
  color: var(--danger-red);
  font-size: 13px;
}

.checkin-list {
  display: grid;
  gap: 10px;
}

.checkin-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--border-color);
}

.checkin-meta b {
  display: block;
  color: var(--text-main);
  font-size: 15px;
}

.checkin-meta span {
  color: var(--text-muted);
  font-size: 12px;
}

.checkin-btn {
  width: auto;
  padding: 9px 14px;
  font-size: 13px;
}

.checkin-empty {
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
  padding: 18px 0;
}

.checkin-history h4 {
  margin: 8px 0 8px;
  color: var(--text-main);
  font-size: 14px;
}

.history-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 12px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.015);
  border: 1px solid var(--border-color);
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--text-muted);
}

.history-row b {
  color: var(--text-main);
}

.phone-tabs.four {
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.phone-tabs.five {
  grid-template-columns: repeat(5, 1fr);
  gap: 5px;
}

.phone-tabs.four button,
.phone-tabs.five button {
  flex-direction: column;
  gap: 3px;
  font-size: 11px;
  min-height: 44px;
}

.phone-tabs.five button {
  font-size: 10px;
}

.pay-hint {
  display: block;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 12px;
  text-align: center;
}

.wallet-pane {
  padding: 1.2rem 1.5rem 2rem;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wallet-card {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.wallet-label {
  font-size: 13px;
  opacity: 0.85;
}

.wallet-balance {
  font-size: 30px;
  font-weight: 700;
}

.wallet-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  opacity: 0.9;
}

.wallet-discount {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 999px;
}

.wallet-recharge h4,
.wallet-tx h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--text-main);
}

.recharge-presets {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}

.recharge-presets button {
  padding: 8px 0;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
}

.recharge-presets button.active {
  border-color: var(--brand);
  color: var(--brand);
  background: rgba(79, 70, 229, 0.08);
}

.recharge-row {
  display: flex;
  gap: 8px;
}

.recharge-row input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  font-size: 14px;
}

.wallet-ok {
  margin: 8px 0 0;
  color: #16a34a;
  font-size: 13px;
}

.tx-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tx-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: #fff;
}

.tx-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tx-meta small {
  color: var(--text-muted);
  font-size: 11px;
}

.tx-in {
  color: #16a34a;
}

.tx-out {
  color: #dc2626;
}

.phone-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 18px;
  padding: 4px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.phone-tabs button {
  min-height: 34px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.72);
  background: transparent;
  font-weight: 800;
}

.phone-tabs button.active {
  color: var(--brand);
  background: #fff;
}

.phone-secondary-action {
  padding: 0 1.5rem;
  margin-top: 1rem;
}

.phone-secondary-action .ghost-button {
  width: 100%;
}

.nav-order-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 14px 1.5rem 0;
  padding: 14px 16px;
  border-radius: 16px;
  color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
  box-shadow: 0 16px 32px -24px rgba(79, 70, 229, 0.9);
}

.nav-order-banner span {
  font-size: 11px;
  opacity: 0.85;
}

.nav-order-banner b {
  display: block;
  margin-top: 3px;
  font-size: 15px;
}

.nav-cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 14px;
  border: none;
  border-radius: 10px;
  background: #fff;
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.nav-cta:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.nav-live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.nav-live i {
  font-size: 8px;
  animation: navPulse 1.2s ease-in-out infinite;
}

@keyframes navPulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
}

.nav-summary {
  margin: 1rem 1.5rem 1rem;
  padding: 18px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 18px 38px -28px rgba(15, 23, 42, 0.5);
}

.nav-summary span,
.nav-summary small {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}

.nav-summary strong {
  display: block;
  margin: 4px 0;
  color: var(--brand);
  font-family: "Outfit", sans-serif;
  font-size: 34px;
}

.phone-map {
  flex: 1;
  position: relative;
  margin: 0 1rem 1rem;
  min-height: 330px;
  border-radius: 22px;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(148, 163, 184, 0.1) 1px, transparent 1px),
    linear-gradient(180deg, rgba(148, 163, 184, 0.1) 1px, transparent 1px),
    #fff;
  background-size: 28px 28px;
  border: 1px solid var(--border-color);
}

.phone-map svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.map-bg-path,
.map-active-path {
  fill: none;
  stroke-linecap: round;
}

.map-bg-path {
  stroke: rgba(79, 70, 229, 0.12);
  stroke-width: 18;
}

.map-active-path {
  stroke: var(--brand);
  stroke-width: 5;
  stroke-dasharray: 12 10;
  animation: dashAnim 2s linear infinite;
}

.map-node {
  position: absolute;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  color: #fff;
  box-shadow: 0 12px 24px -16px rgba(15, 23, 42, 0.7);
}

.car-node {
  left: 36px;
  bottom: 34px;
  background: var(--brand);
}

.lift-node {
  left: 151px;
  top: 170px;
  background: var(--warning-yellow);
}

.gate-node {
  right: 46px;
  top: 54px;
  background: var(--safety-green);
}

.gate-node.danger {
  background: var(--danger-red);
}

.map-label {
  position: absolute;
  padding: 5px 8px;
  border-radius: 999px;
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--border-color);
  font-size: 11px;
  font-weight: 800;
}

.map-label.start {
  left: 22px;
  bottom: 78px;
}

.map-label.end {
  right: 22px;
  top: 98px;
}

.nav-instruction {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 18px 1.5rem 8px;
}

.nav-instruction b,
.nav-instruction span {
  display: block;
}

.nav-instruction b {
  color: var(--text-main);
  font-size: 15px;
}

.nav-instruction span {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.45;
}

.nav-instruction strong {
  color: var(--brand);
  font-family: "Outfit", sans-serif;
  white-space: nowrap;
}

.nav-progress {
  display: flex;
  gap: 10px;
  padding: 0 1.5rem 1.4rem;
}

.nav-progress i {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.08);
}

.nav-progress i.filled {
  background: var(--brand);
}

.touch-overlay {
  position: absolute;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(8px);
}

.touch-overlay > div {
  width: 100%;
  padding: 30px 22px;
  text-align: center;
  border-radius: 20px;
  color: var(--text-main);
  background: #fff;
  border: 1px solid var(--border-color);
  box-shadow: 0 20px 48px -32px rgba(15, 23, 42, 0.55);
}

.touch-overlay h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.touch-overlay p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
}

.touch-overlay strong {
  display: block;
  margin: 26px 0;
  color: var(--safety-green);
  font-family: "Outfit", sans-serif;
  font-size: 48px;
  font-variant-numeric: tabular-nums;
}

.phone-tabs.three {
  grid-template-columns: repeat(3, 1fr);
}

.chat-status {
  margin: 14px 1.2rem 0;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 700;
}

.chat-status i {
  font-size: 7px;
  color: var(--safety-green);
}

.chat-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 1.2rem 6px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-bubble {
  max-width: 82%;
  padding: 10px 13px;
  border-radius: 16px;
  font-size: 13.5px;
  line-height: 1.55;
}

.chat-bubble p {
  margin: 0;
}

.chat-bubble.assistant {
  align-self: flex-start;
  color: var(--text-main);
  background: #fff;
  border: 1px solid var(--border-color);
  border-bottom-left-radius: 5px;
}

.chat-bubble.user {
  align-self: flex-end;
  color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
  border-bottom-right-radius: 5px;
}

.chat-bubble small {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 10px;
}

.chat-bubble.typing {
  display: flex;
  gap: 4px;
  align-items: center;
}

.chat-bubble.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-muted);
  animation: chatDot 1s infinite;
}

.chat-bubble.typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.chat-bubble.typing span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes chatDot {
  0%,
  60%,
  100% {
    opacity: 0.3;
  }
  30% {
    opacity: 1;
  }
}

.chat-quick {
  display: flex;
  gap: 8px;
  padding: 4px 1.2rem;
  overflow-x: auto;
}

.chat-quick button {
  white-space: nowrap;
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}

.chat-quick button:disabled {
  opacity: 0.5;
}

.chat-input {
  display: flex;
  gap: 8px;
  padding: 10px 1.2rem 1.2rem;
}

.chat-input input {
  flex: 1;
  height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: #fff;
  font-size: 13.5px;
  outline: none;
}

.chat-input input:focus {
  border-color: var(--brand);
}

.chat-input button {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
}

.chat-input button:disabled {
  opacity: 0.5;
}

.phone-demo-detail {
  border: none;
  box-shadow: none;
  background: transparent;
}

.phone-flow-list {
  grid-template-columns: 1fr;
}

.phone-demo-metrics {
  margin-top: 18px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

@keyframes dashAnim {
  to {
    stroke-dashoffset: -22;
  }
}

.reservation-block {
  margin-top: 22px;
  padding: 20px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid var(--border-color);
}

.reservation-block h3 {
  margin: 0 0 6px;
  font-size: 17px;
  color: var(--text-main);
}

.reservation-block h3 i {
  color: var(--brand);
  margin-right: 8px;
}

.reservation-block > p {
  margin: 0 0 16px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.res-form {
  display: grid;
  grid-template-columns: 1.3fr 1fr 0.85fr auto;
  gap: 10px;
}

.res-form input,
.res-form select {
  height: 40px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: #fff;
  font-size: 13px;
  color: var(--text-main);
  outline: none;
}

.res-form input:focus,
.res-form select:focus {
  border-color: var(--brand);
}

.res-list {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.res-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.015);
  border: 1px solid var(--border-color);
}

.res-meta b {
  display: block;
  color: var(--text-main);
  font-size: 14px;
}

.res-meta span {
  color: var(--text-muted);
  font-size: 12px;
}

.res-actions {
  display: flex;
  gap: 8px;
}

.res-empty {
  margin: 14px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

@media (max-width: 680px) {
  .phone-demo-metrics {
    grid-template-columns: 1fr;
  }

  .res-form {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
