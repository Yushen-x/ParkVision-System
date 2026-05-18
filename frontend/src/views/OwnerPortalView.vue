<script setup>
import { computed, ref } from "vue";
import { enqueueVip, getters, runOwnerAction, state } from "../stores/parkingStore";

const currentOrder = getters.currentOrder;
const showOverlay = ref(false);
const timer = ref(180);
let timerId = null;

const ownerStatus = computed(() => {
  const status = currentOrder.value?.status;
  switch (status) {
    case "PARKED":
      return "Stored";
    case "RETRIEVING":
      return "Retrieving";
    case "TOUCHING":
      return "Touch-and-go";
    case "FINISHED":
      return "Closed";
    default:
      return "Idle";
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

const fee = computed(() => Number(currentOrder.value?.amount || 0).toFixed(2));
const plate = computed(() => currentOrder.value?.plateNo || "No active order");
const slotLabel = computed(() => currentOrder.value?.slotId || "N/A");

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
          <h2>My vehicle</h2>
        </div>

        <div class="c-status-card">
          <div class="c-status-indicator">
            <div class="c-pulse-ring"></div>
            <div class="c-inner-circle">{{ ownerStatus }}</div>
          </div>
          <div class="c-location"><i class="fa-solid fa-location-dot"></i> Slot {{ slotLabel }} | backend-backed order flow</div>
          <div class="c-info-grid">
            <div class="c-info-item"><span>Parking duration</span><strong>{{ duration }}</strong></div>
            <div class="c-info-item"><span>Current fee</span><strong style="color: #10b981;">CNY {{ fee }}</strong></div>
          </div>
        </div>

        <div class="c-actions">
          <button class="c-btn c-btn-primary" :disabled="state.busy.ownerAction" @click="doAction('retrieve')">
            <i class="fa-solid fa-truck-ramp-box" style="font-size: 20px;"></i> Retrieve
          </button>
          <button class="c-btn c-btn-secondary" :disabled="state.busy.ownerAction" @click="doAction('touch')">
            <i class="fa-solid fa-box-open" style="font-size: 20px; color: #f59e0b;"></i> Touch-and-Go
            <span class="c-badge">No checkout</span>
          </button>
        </div>

        <div class="c-vip-card" @click="doVip">
          <div class="c-vip-icon"><i class="fa-solid fa-bolt-lightning"></i></div>
          <div class="c-vip-text">
            <h4>VIP retrieval</h4>
            <p>Insert the current order at the head of the AGV queue.</p>
          </div>
          <div class="c-vip-price">+CNY 5.00</div>
        </div>

        <div style="padding: 0 1.5rem; margin-top: 1rem;">
          <button class="ghost-button" style="width:100%;" :disabled="state.busy.ownerAction" @click="doAction('pay')">
            Mark paid and close order
          </button>
        </div>

        <div
          v-if="showOverlay"
          style="position: absolute; top:0; left:0; width:100%; height:100%; background:rgba(15,23,42,0.85); backdrop-filter:blur(5px); z-index:50; display:flex; flex-direction:column; justify-content:center; align-items:center;"
        >
          <div style="background: rgba(30, 41, 59, 0.95); width: 85%; border-radius: 20px; padding: 2.5rem 1.5rem; text-align: center; color: white; border: 1px solid rgba(255,255,255,0.1);">
            <h3 style="margin:0 0 10px; font-size:18px;">Touch-and-go active</h3>
            <p style="color:#94a3b8; font-size:13px; margin:0;">Finish collecting items before the countdown expires.</p>
            <div style="font-size: 48px; font-weight:700; font-family:'Orbitron', sans-serif; color: #10b981; margin: 30px 0;">
              {{ formatTime(timer) }}
            </div>
            <button class="primary-button full" @click="finishTouch">Dismiss</button>
          </div>
        </div>
      </div>
    </div>

    <div class="surface owner-detail" style="border:none; box-shadow:none; background:transparent;">
      <div class="section-head">
        <div>
          <h2 style="font-size:24px; margin-bottom:10px;">Owner journey</h2>
          <p style="font-size:15px; max-width:500px;">Retrieve, touch-and-go, pay, and VIP queue insertion now call real backend endpoints when available.</p>
        </div>
      </div>
      <div class="module-row" style="grid-template-columns: 1fr;">
        <div v-for="([title, detail], index) in state.ownerTimeline" :key="`${title}-${index}`">
          <b>{{ title }}</b>
          <span>{{ detail }}</span>
        </div>
      </div>
    </div>
  </section>
</template>
