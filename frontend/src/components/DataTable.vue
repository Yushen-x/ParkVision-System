<script setup>
const props = defineProps({
  headers: {
    type: Array,
    default: () => [],
  },
  rows: {
    type: Array,
    default: () => [],
  },
  rowKeys: {
    type: Array,
    default: () => [],
  },
  clickable: {
    type: Boolean,
    default: false,
  },
  selectedRowKey: {
    type: [String, Number],
    default: null,
  },
});

const emit = defineEmits(["row-click"]);

function rowKey(row, index) {
  return props.rowKeys[index] ?? index;
}

function handleRowClick(row, index) {
  if (!props.clickable) return;
  emit("row-click", { row, index, key: rowKey(row, index) });
}
</script>

<template>
  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th v-for="header in headers" :key="header">{{ header }}</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(row, index) in rows"
          :key="rowKey(row, index)"
          :class="{ clickable: clickable, selected: selectedRowKey !== null && rowKey(row, index) === selectedRowKey }"
          @click="handleRowClick(row, index)"
        >
          <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
