<script setup>
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
        :title="`${slot.id} ${slot.status}`"
      ></span>
    </div>
    <div class="legend-row">
      <span><i class="legend empty"></i>Empty</span>
      <span><i class="legend occupied"></i>Occupied</span>
      <span><i class="legend charging"></i>Charging</span>
      <span><i class="legend buffer"></i>Buffer</span>
      <span><i class="legend maintenance"></i>Maintenance</span>
    </div>
  </div>
</template>
