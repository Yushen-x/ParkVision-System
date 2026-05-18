<script setup>
import { computed } from "vue";
import DataTable from "../components/DataTable.vue";
import { state } from "../stores/parkingStore";

const accessRows = computed(() =>
  state.accessList.map((item) => [item.plateNo, item.listType, item.userType, item.validUntil, item.remark]),
);

const deviceRows = computed(() => [
  ...state.devices.cameras.map((camera) => [
    camera.cameraId,
    "Camera",
    camera.profile,
    camera.status,
    `${camera.fps} fps / ${camera.bitrateKbps} kbps`,
  ]),
  ...state.devices.gates.map((gate) => [
    gate.gateId,
    "Gate",
    gate.protocol,
    gate.gateState,
    `${gate.endpoint} | queue ${gate.queueDepth}`,
  ]),
  ...state.devices.chargers.map((charger) => [
    charger.chargerId,
    "Charger",
    charger.protocol,
    charger.connectorStatus,
    `${charger.endpoint} | ${charger.powerKw} kW`,
  ]),
]);
</script>

<template>
  <section class="dispatch-layout">
    <article class="surface">
      <div class="section-head">
        <div>
          <h2><i class="fa-solid fa-shield-halved" style="color:var(--brand); margin-right:8px;"></i>Access and trust list</h2>
          <p>Whitelist, blacklist, and temporary-entry policies are persisted in the backend and shared with the admin console.</p>
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

    <article class="surface wide">
      <div class="section-head">
        <div>
          <h2><i class="fa-solid fa-network-wired" style="color:var(--brand); margin-right:8px;"></i>Field devices</h2>
          <p>Cameras, PLC gates, and OCPP chargers below are all read from the device overview endpoint.</p>
        </div>
      </div>
      <div class="table-wrap" style="margin-top:16px;">
        <DataTable :headers="['Device', 'Type', 'Protocol', 'State', 'Telemetry']" :rows="deviceRows" />
      </div>
    </article>

    <article class="surface wide">
      <div class="section-head compact">
        <div>
          <h2>Recent field events</h2>
          <p>This event stream is persisted server-side and reflects device-side simulations, not frontend-only notes.</p>
        </div>
      </div>
      <div class="queue-list" style="margin-top:16px;">
        <div v-for="event in state.devices.events" :key="event.eventId" class="queue-item" style="grid-template-columns: minmax(0, 1fr) auto;">
          <div>
            <b>{{ event.eventCode }} - {{ event.deviceId }}</b>
            <span>{{ event.message }}</span>
          </div>
          <span class="status-pill" :class="event.severity === 'critical' || event.severity === 'high' ? 'warning' : 'stable'">
            {{ event.severity }}
          </span>
        </div>
      </div>
    </article>
  </section>
</template>
