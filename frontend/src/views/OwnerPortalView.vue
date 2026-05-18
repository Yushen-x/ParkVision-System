<script setup>
import { computed, ref } from "vue";
import { enqueueVip, getters, runOwnerAction, state } from "../stores/parkingStore";
import { zhMoney, zhText } from "../utils/localize";

const currentOrder = getters.currentOrder;
const showOverlay = ref(false);
const timer = ref(180);
let timerId = null;

const ownerStatus = computed(() => {
  const status = currentOrder.value?.status;
  switch (status) {
    case "PARKED":
      return "已入库";
    case "RETRIEVING":
      return "取车中";
    case "TOUCHING":
      return "临停取物";
    case "FINISHED":
      return "已关闭";
    default:
      return "待命";
  }
});

const duration = computed(() => {
  if (!currentOrder.value?.entryTime) return "00:00";
  const diff = Math.max(0, Date.now() - new Date(currentOrder.value.entryTime).getTime());
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(minutes / 60)
    .toString()
    .padStart(2, "0");
  const remain = String(minutes % 60).padStart(2, "0");
  return `${hours}:${remain}`;
});

const fee = computed(() => zhMoney(currentOrder.value?.amount || 0));
const plate = computed(() => currentOrder.value?.plateNo || "暂无活跃订单");
const slotLabel = computed(() => currentOrder.value?.slotId || "暂无");

async function doAction(action) {
  if (!currentOrder.value) return;

  if (action === "touch") {
    showOverlay.value = true;
    timer.value = 180;
    clearInterval(timerId);
    timerId = window.setInterval(() => {
      timer.value -= 1;
      if (timer.value <= 0) {
        clearInterval(timerId);
      }
    }, 1000);
  }

  await runOwnerAction(action, currentOrder.value.orderNo);
}

async function doVip() {
  if (!currentOrder.value) return;
  await enqueueVip(currentOrder.value.orderNo);
}

function finishTouch() {
  showOverlay.value = false;
  clearInterval(timerId);
}

function formatTime(seconds) {
  const minutes = Math.floor(seconds / 60)
    .toString()
    .padStart(2, "0");
  const remain = String(seconds % 60).padStart(2, "0");
  return `${minutes}:${remain}`;
}
</script>

<template>
  <section class="mobile-workbench">
    <div class="phone-frame">
      <div class="phone-status">
        <span>ParkVision</span>
        <span><i class="fa-solid fa-wifi"></i> 5G <i class="fa-solid fa-battery-full"></i></span>
      </div>
      <div class="phone-screen">
        <div class="c-app-header">
          <div class="c-header-top">
            <span class="c-plate">{{ plate }}</span>
            <i class="fa-regular fa-bell" style="font-size: 18px;"></i>
          </div>
          <h2>我的车辆</h2>
        </div>

        <div class="c-status-card">
          <div class="c-status-indicator">
            <div class="c-pulse-ring"></div>
            <div class="c-inner-circle">{{ ownerStatus }}</div>
          </div>
          <div class="c-location"><i class="fa-solid fa-location-dot"></i> 车位 {{ slotLabel }} | 后端订单流</div>
          <div class="c-info-grid">
            <div class="c-info-item"><span>停车时长</span><strong>{{ duration }}</strong></div>
            <div class="c-info-item"><span>当前费用</span><strong style="color: #10b981;">{{ fee }}</strong></div>
          </div>
        </div>

        <div class="c-actions">
          <button class="c-btn c-btn-primary" :disabled="state.busy.ownerAction" @click="doAction('retrieve')">
            <i class="fa-solid fa-truck-ramp-box" style="font-size: 20px;"></i> 取车
          </button>
          <button class="c-btn c-btn-secondary" :disabled="state.busy.ownerAction" @click="doAction('touch')">
            <i class="fa-solid fa-box-open" style="font-size: 20px; color: #f59e0b;"></i> 临停取物
            <span class="c-badge">不结单</span>
          </button>
        </div>

        <div class="c-vip-card" @click="doVip">
          <div class="c-vip-icon"><i class="fa-solid fa-bolt-lightning"></i></div>
          <div class="c-vip-text">
            <h4>VIP 优先取车</h4>
            <p>把当前订单插入 AGV 队列最前面。</p>
          </div>
          <div class="c-vip-price">+￥5.00</div>
        </div>

        <div style="padding: 0 1.5rem; margin-top: 1rem;">
          <button class="ghost-button" style="width:100%;" :disabled="state.busy.ownerAction" @click="doAction('pay')">
            标记已支付并关闭订单
          </button>
        </div>

        <div
          v-if="showOverlay"
          style="position: absolute; top:0; left:0; width:100%; height:100%; background:rgba(15,23,42,0.85); backdrop-filter:blur(5px); z-index:50; display:flex; flex-direction:column; justify-content:center; align-items:center;"
        >
          <div style="background: rgba(30, 41, 59, 0.95); width: 85%; border-radius: 20px; padding: 2.5rem 1.5rem; text-align: center; color: white; border: 1px solid rgba(255,255,255,0.1);">
            <h3 style="margin:0 0 10px; font-size:18px;">临停取物已开启</h3>
            <p style="color:#94a3b8; font-size:13px; margin:0;">请在倒计时结束前完成物品拿取。</p>
            <div style="font-size: 48px; font-weight:700; font-family:'Orbitron', sans-serif; color: #10b981; margin: 30px 0;">
              {{ formatTime(timer) }}
            </div>
            <button class="primary-button full" @click="finishTouch">关闭</button>
          </div>
        </div>
      </div>
    </div>

    <div class="surface owner-detail" style="border:none; box-shadow:none; background:transparent;">
      <div class="section-head">
        <div>
          <h2 style="font-size:24px; margin-bottom:10px;">车主服务流程</h2>
          <p style="font-size:15px; max-width:500px;">取车、临停取物、支付和 VIP 插队都会优先调用真实后端接口。</p>
        </div>
      </div>
      <div class="module-row" style="grid-template-columns: 1fr;">
        <div v-for="([title, detail], index) in state.ownerTimeline" :key="`${title}-${index}`">
          <b>{{ zhText(title) }}</b>
          <span>{{ zhText(detail) }}</span>
        </div>
      </div>
    </div>
  </section>
</template>
