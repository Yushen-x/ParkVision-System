<script setup>
import { zhText } from "../utils/localize";

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
      <b>{{ zhText(title) }}</b>
      <span>{{ new Date().toLocaleTimeString("zh-CN", { hour12: false }) }} | {{ zhText(detail) }}</span>
    </div>
  </div>
</template>
