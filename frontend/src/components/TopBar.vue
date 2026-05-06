<script setup>
import { onMounted, onUnmounted, ref } from "vue";
defineProps({ title: { type: String, required: true }, emergency: { type: Boolean, default: false } });
defineEmits(["entry", "pre-dispatch", "emergency"]);
const clock = ref("--:--:--");
let timer;
function tick() { clock.value = new Date().toLocaleTimeString("zh-CN", { hour12: false }); }
onMounted(() => { tick(); timer = window.setInterval(tick, 1000); });
onUnmounted(() => window.clearInterval(timer));
</script>

<template>
  <header class="topbar">
    <div>
      <p class="eyebrow"><i class="fa-solid fa-microchip"></i> AGV 自动立体车库 · 云边协同调度</p>
      <h1>{{ title }}</h1>
    </div>
    <div class="topbar-actions">
      <div class="time-chip" style="background: rgba(255,255,255,0.05); border: 1px solid var(--border-color); padding: 8px 16px; border-radius: 8px;">
        <span style="color:var(--text-muted); font-size:11px;">本地时间</span>
        <strong style="color:#fff; font-family:'Orbitron', sans-serif;">{{ clock }}</strong>
      </div>
      <button class="ghost-button" @click="$emit('entry')"><i class="fa-solid fa-car"></i> 模拟入场</button>
      <button class="primary-button" @click="$emit('pre-dispatch')"><i class="fa-solid fa-forward-fast"></i> 流量预调</button>
      <button class="danger-button" @click="$emit('emergency')">
        <i class="fa-solid" :class="emergency ? 'fa-lock-open' : 'fa-triangle-exclamation'"></i> {{ emergency ? "解除急停" : "全域急停" }}
      </button>
    </div>
  </header>
</template>
