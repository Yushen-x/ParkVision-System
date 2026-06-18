<script setup>
import { onMounted, onUnmounted, ref, watch } from "vue";

const props = defineProps({
  history: {
    type: Array,
    default: () => [],
  },
  prediction: {
    type: Array,
    default: () => [],
  },
});

const container = ref(null);
const canvas = ref(null);
let observer = null;
let logicalWidth = 0;
let logicalHeight = 0;

function draw() {
  if (!canvas.value || logicalWidth <= 0 || logicalHeight <= 0) return;

  const ctx = canvas.value.getContext("2d");
  const width = logicalWidth;
  const height = logicalHeight;
  const padding = Math.max(36, Math.round(Math.min(width, height) * 0.12));
  const all = [...props.history, ...props.prediction];
  const max = Math.max(...all, 10) + 12;
  const chartWidth = width - padding * 2;
  const chartHeight = height - padding * 2;
  const step = chartWidth / Math.max(all.length - 1, 1);

  ctx.clearRect(0, 0, width, height);
  ctx.fillStyle = "#ffffff";
  ctx.fillRect(0, 0, width, height);
  ctx.strokeStyle = "#f1f5f9";
  ctx.lineWidth = 1;
  ctx.font = "12px Inter, sans-serif";
  ctx.fillStyle = "#64748b";

  for (let i = 0; i <= 4; i += 1) {
    const y = padding + (chartHeight / 4) * i;
    ctx.beginPath();
    ctx.moveTo(padding, y);
    ctx.lineTo(width - padding, y);
    ctx.stroke();
    ctx.fillText(String(Math.round(max - (max / 4) * i)), 12, y + 4);
  }

  ctx.beginPath();
  all.forEach((value, index) => {
    const x = padding + step * index;
    const y = padding + chartHeight - (value / max) * chartHeight;
    if (index === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  });
  ctx.strokeStyle = "#4f46e5";
  ctx.lineWidth = 3;
  ctx.stroke();

  all.forEach((value, index) => {
    const x = padding + step * index;
    const y = padding + chartHeight - (value / max) * chartHeight;
    ctx.fillStyle = index < props.history.length ? "#3b82f6" : "#f59e0b";
    ctx.beginPath();
    ctx.arc(x, y, 4.8, 0, Math.PI * 2);
    ctx.fill();
  });
}

function resize() {
  if (!container.value || !canvas.value) return;

  const rect = container.value.getBoundingClientRect();
  const dpr = window.devicePixelRatio || 1;
  logicalWidth = Math.max(Math.floor(rect.width), 280);
  logicalHeight = Math.max(Math.floor(rect.height), 220);

  canvas.value.width = Math.floor(logicalWidth * dpr);
  canvas.value.height = Math.floor(logicalHeight * dpr);
  canvas.value.style.width = `${logicalWidth}px`;
  canvas.value.style.height = `${logicalHeight}px`;

  const ctx = canvas.value.getContext("2d");
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  draw();
}

onMounted(() => {
  resize();
  if (typeof ResizeObserver !== "undefined" && container.value) {
    observer = new ResizeObserver(resize);
    observer.observe(container.value);
  } else {
    window.addEventListener("resize", resize);
  }
});

onUnmounted(() => {
  observer?.disconnect();
  window.removeEventListener("resize", resize);
});

watch(() => [props.history, props.prediction], draw, { deep: true });
</script>

<template>
  <div ref="container" class="traffic-chart">
    <canvas ref="canvas" aria-label="车流预测图表"></canvas>
  </div>
</template>

<style scoped>
.traffic-chart {
  width: 100%;
  min-height: 220px;
  height: clamp(220px, 28vw, 320px);
  border-radius: 12px;
  overflow: hidden;
}

.traffic-chart canvas {
  display: block;
}
</style>
