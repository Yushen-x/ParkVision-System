<script setup>
defineProps({
  slots: {
    type: Array,
    default: () => [],
  },
  agvs: {
    type: Array,
    default: () => [],
  },
});
</script>

<template>
  <div class="warehouse-map">
    <div v-for="slot in slots" :key="slot.id" class="warehouse-slot" :class="slot.status">
      <strong>{{ slot.id }}</strong>
      {{ slot.layer }}
    </div>
    <div
      v-for="agv in agvs"
      :key="agv.id"
      class="agv-unit"
      :class="{ loaded: agv.load }"
      :style="{ left: `${agv.x}%`, top: `${agv.y}%` }"
      :title="agv.task"
    >
      {{ agv.id.slice(-2) }}
    </div>
  </div>
</template>
