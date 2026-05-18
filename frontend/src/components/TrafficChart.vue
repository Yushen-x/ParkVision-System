<script setup>
import { onMounted, ref, watch } from "vue";

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

const canvas = ref(null);

function draw() {
  if (!canvas.value) return;
  const ctx = canvas.value.getContext("2d");
  const width = canvas.value.width;
  const height = canvas.value.height;
  const padding = 46;
  const all = [...props.history, ...props.prediction];
  const max = Math.max(...all, 10) + 12;
  const chartWidth = width - padding * 2;
  const chartHeight = height - padding * 2;
  const step = chartWidth / Math.max(all.length - 1, 1);

  ctx.clearRect(0, 0, width, height);
  ctx.fillStyle = "#f8fbfc";
  ctx.fillRect(0, 0, width, height);
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
    ctx.fillStyle = index < props.history.length ? "#117c73" : "#c4652d";
    ctx.beginPath();
    ctx.arc(x, y, 4.8, 0, Math.PI * 2);
    ctx.fill();
  });
}

onMounted(draw);
watch(() => [props.history, props.prediction], draw, { deep: true });
</script>

<template>
  <canvas ref="canvas" width="920" height="320" aria-label="车流预测图表"></canvas>
</template>
