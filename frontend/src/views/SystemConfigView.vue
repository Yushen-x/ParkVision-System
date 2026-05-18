<script setup>
import { computed } from "vue";
import DataTable from "../components/DataTable.vue";
import { state } from "../stores/parkingStore";

const accessRows = computed(() =>
  state.accessList.map((item) => [item.plateNo, item.listType, item.userType, item.validUntil, item.remark]),
);
</script>

<template>
  <section class="dispatch-layout">
    <article class="surface">
      <div class="section-head">
        <div>
          <h2><i class="fa-solid fa-shield-halved" style="color:var(--brand); margin-right:8px;"></i>Access and trust list</h2>
          <p>Whitelist, blacklist, and temporary-entry policies are served from the same backend data used by the admin console.</p>
        </div>
      </div>
      <div class="table-wrap" style="margin-top:16px;">
        <DataTable :headers="['Plate', 'List type', 'User type', 'Valid until', 'Remark']" :rows="accessRows" />
      </div>
    </article>

    <aside>
      <article class="surface" style="height: 100%;">
        <div class="section-head compact">
          <h2>Node health</h2>
        </div>
        <div style="margin-top:16px; display:flex; flex-direction:column; gap:12px;">
          <div
            v-for="node in state.systemNodes"
            :key="node.name"
            class="strategy-card"
            :style="
              node.level === 'warning'
                ? 'border-left: 4px solid var(--danger-red); background: rgba(239,68,68,0.05);'
                : 'border-left: 4px solid var(--safety-green);'
            "
          >
            <div style="display:flex; justify-content:space-between; gap:12px;">
              <strong :style="node.level === 'warning' ? 'color:var(--danger-red);' : ''">{{ node.name }}</strong>
              <span class="status-pill" :class="node.level === 'warning' ? 'warning' : 'stable'" style="min-height:auto; padding:2px 8px;">
                {{ node.latency }}
              </span>
            </div>
            <span :style="node.level === 'warning' ? 'color:var(--danger-red);' : ''">{{ node.detail }}</span>
          </div>
        </div>
      </article>
    </aside>
  </section>
</template>
