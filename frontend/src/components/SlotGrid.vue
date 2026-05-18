<script setup>
import { zhText } from "../utils/localize";

defineProps({
  slots: {
    type: Array,
    default: () => [],
  },
  limit: {
    type: Number,
    default: 60,
  },
});
</script>

<template>
  <div>
    <div class="mini-grid">
      <span
        v-for="slot in slots.slice(0, limit)"
        :key="slot.id"
        class="slot-dot"
        :class="slot.status"
        :title="`${slot.id} ${zhText(slot.status)}`"
      ></span>
    </div>
    <div class="legend-row">
      <span><i class="legend empty"></i>空闲</span>
      <span><i class="legend occupied"></i>已停放</span>
      <span><i class="legend charging"></i>充电中</span>
      <span><i class="legend buffer"></i>周转区</span>
      <span><i class="legend maintenance"></i>维护中</span>
    </div>
  </div>
</template>
