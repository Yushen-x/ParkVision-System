<script setup>
defineProps({
  events: {
    type: Array,
    default: () => [],
  },
});
</script>

<template>
  <div class="event-list">
    <div v-for="([title, detail], index) in events" :key="`${title}-${index}`" class="event-item">
      <b>{{ title }}</b>
      <span>{{ new Date().toLocaleTimeString("zh-CN", { hour12: false }) }} · {{ detail }}</span>
    </div>
  </div>
</template>
