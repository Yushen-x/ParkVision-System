<script setup>
defineProps({ label: String, value: [String, Number], hint: String, tone: String });
</script>
<template>
  <div class="kpi-card" :class="{ alert: tone === 'alert' }">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <small><i class="fa-solid fa-chart-simple" style="margin-right:4px;"></i>{{ hint }}</small>
  </div>
</template>
