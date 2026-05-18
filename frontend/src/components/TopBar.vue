<script setup>
import { onMounted, onUnmounted, ref } from "vue";

defineProps({
  title: { type: String, required: true },
  emergency: { type: Boolean, default: false },
  entryBusy: { type: Boolean, default: false },
  dispatchBusy: { type: Boolean, default: false },
});

defineEmits(["entry", "pre-dispatch", "emergency"]);

const clock = ref("--:--:--");
let timer;

function tick() {
  clock.value = new Date().toLocaleTimeString("en-GB", { hour12: false });
}

onMounted(() => {
  tick();
  timer = window.setInterval(tick, 1000);
});

onUnmounted(() => window.clearInterval(timer));
</script>

<template>
  <header class="topbar">
    <div>
      <p class="eyebrow"><i class="fa-solid fa-microchip"></i> Smart parking CPS with AI vision, dispatch, and fallback-safe APIs</p>
      <h1>{{ title }}</h1>
    </div>
    <div class="topbar-actions">
      <div class="time-chip" style="background: rgba(255,255,255,0.05); border: 1px solid var(--border-color); padding: 8px 16px; border-radius: 8px;">
        <span style="color:var(--text-muted); font-size:11px;">Local time</span>
        <strong style="color:#fff; font-family:'Orbitron', sans-serif;">{{ clock }}</strong>
      </div>
      <button class="ghost-button" :disabled="entryBusy" @click="$emit('entry')">
        <i class="fa-solid fa-car"></i>
        {{ entryBusy ? "Submitting..." : "Simulate entry" }}
      </button>
      <button class="primary-button" :disabled="dispatchBusy" @click="$emit('pre-dispatch')">
        <i class="fa-solid fa-forward-fast"></i>
        {{ dispatchBusy ? "Dispatching..." : "Pre-dispatch" }}
      </button>
      <button class="danger-button" @click="$emit('emergency')">
        <i class="fa-solid" :class="emergency ? 'fa-lock-open' : 'fa-triangle-exclamation'"></i>
        {{ emergency ? "Clear stop" : "Emergency stop" }}
      </button>
    </div>
  </header>
</template>
