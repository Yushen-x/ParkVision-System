<script setup>
import { computed } from "vue";
import { state } from "../stores/parkingStore";

const inboundGate = computed(() => state.devices.gates.find((gate) => gate.gateId.includes("IN")) || null);
const outboundGate = computed(() => state.devices.gates.find((gate) => gate.gateId.includes("OUT")) || null);
const activeCharger = computed(() => state.devices.chargers.find((charger) => charger.connectorStatus !== "Available") || null);
</script>

<template>
  <section class="dispatch-layout">
    <article class="surface">
      <div class="section-head">
        <div>
          <h2>Live dispatch queue</h2>
          <p>Tasks are sourced from the backend queue first, including pre-dispatch, inbound storage, and VIP insertions.</p>
        </div>
      </div>
      <div class="queue-list" style="margin-top:16px;">
        <div v-for="(task, index) in state.queue" :key="`${task.plateNo}-${index}`" class="queue-item" :class="{ vip: task.vip }">
          <div class="queue-rank">{{ index + 1 }}</div>
          <div>
            <b>{{ task.type }} - {{ task.plateNo }}</b>
            <span><i class="fa-regular fa-clock"></i> Estimated wait: {{ task.wait }}</span>
          </div>
          <span class="queue-tag"><i class="fa-solid fa-tag"></i> {{ task.tag }}</span>
        </div>
      </div>
    </article>

    <aside>
      <article class="surface" style="height: 100%;">
        <div class="section-head compact">
          <h2>Field release conditions</h2>
        </div>
        <div style="margin-top:16px;">
          <div class="strategy-card">
            <strong><i class="fa-solid fa-right-to-bracket" style="color:var(--brand); margin-right:6px;"></i>{{ inboundGate?.gateId || "Inbound gate" }}</strong>
            <span>State {{ inboundGate?.gateState || "N/A" }} | queue {{ inboundGate?.queueDepth ?? 0 }} | decision {{ inboundGate?.lastDecision || "N/A" }}</span>
          </div>
          <div class="strategy-card">
            <strong><i class="fa-solid fa-right-from-bracket" style="color:var(--warning-yellow); margin-right:6px;"></i>{{ outboundGate?.gateId || "Outbound gate" }}</strong>
            <span>State {{ outboundGate?.gateState || "N/A" }} | queue {{ outboundGate?.queueDepth ?? 0 }} | ESTOP {{ outboundGate?.estopArmed ? "armed" : "clear" }}</span>
          </div>
          <div class="strategy-card">
            <strong><i class="fa-solid fa-plug-circle-check" style="color:var(--safety-green); margin-right:6px;"></i>{{ activeCharger?.chargerId || "No active charger" }}</strong>
            <span>
              {{
                activeCharger
                  ? `${activeCharger.vehiclePlate || "No vehicle"} | ${activeCharger.powerKw} kW | ${activeCharger.sessionKwh} kWh`
                  : "Charging bays are currently idle."
              }}
            </span>
          </div>
        </div>
      </article>
    </aside>
  </section>
</template>
