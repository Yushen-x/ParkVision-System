<script setup>
import { onMounted, onUnmounted, ref } from "vue";

defineProps({
  title: { type: String, required: true },
  emergency: { type: Boolean, default: false },
  entryBusy: { type: Boolean, default: false },
  dispatchBusy: { type: Boolean, default: false },
});

defineEmits(["entry", "pre-dispatch", "emergency", "reset"]);

const clock = ref("--:--:--");
let timer;

function tick() {
  clock.value = new Date().toLocaleTimeString("zh-CN", { hour12: false });
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
      <p class="eyebrow"><i class="fa-solid fa-microchip"></i> 车主请求、AGV 履约、跨楼层交接和数据库状态同步</p>
      <h1>{{ title }}</h1>
    </div>
    <div class="topbar-actions">
      <div class="time-chip" style="background: rgba(79, 70, 229, 0.06); border: 1px solid rgba(79, 70, 229, 0.15); padding: 8px 16px; border-radius: 8px;">
        <span style="color:var(--text-muted); font-size:11px; margin-right: 6px;">本地时间</span>
        <strong style="color:var(--brand); font-family:'Outfit', sans-serif; font-variant-numeric: tabular-nums;">{{ clock }}</strong>
      </div>
      <button class="ghost-button" :disabled="entryBusy" @click="$emit('entry')">
        <i class="fa-solid fa-car"></i>
        {{ entryBusy ? "提交中..." : "登记入场" }}
      </button>
      <button class="primary-button" :disabled="dispatchBusy" @click="$emit('pre-dispatch')">
        <i class="fa-solid fa-forward-fast"></i>
        {{ dispatchBusy ? "调度中..." : "触发预调度" }}
      </button>
      <button class="danger-button" @click="$emit('emergency')">
        <i class="fa-solid" :class="emergency ? 'fa-lock-open' : 'fa-triangle-exclamation'"></i>
        {{ emergency ? "解除急停" : "紧急停车" }}
      </button>
      <button class="reset-button" @click="$emit('reset')" title="重置所有数据到初始状态">
        <i class="fa-solid fa-arrows-rotate"></i> 重置演示
      </button>
    </div>
  </header>
</template>

<style scoped>
.reset-button {
  height: 36px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid rgba(99, 102, 241, 0.25);
  background: rgba(99, 102, 241, 0.08);
  color: var(--brand, #6366f1);
  font-size: 13px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.18s ease;
}
.reset-button:hover {
  background: var(--brand, #6366f1);
  color: #fff;
}
</style>
