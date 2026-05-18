<script setup>
import EventStream from "../components/EventStream.vue";
import MetricCard from "../components/MetricCard.vue";
import SlotGrid from "../components/SlotGrid.vue";
import TrafficChart from "../components/TrafficChart.vue";
import { state } from "../stores/parkingStore";
</script>

<template>
  <section>
    <div class="kpi-grid">
      <MetricCard label="Occupancy" :value="`${state.summary.occupancyRate}%`" hint="Slot state is refreshed from the parking API." />
      <MetricCard label="Vehicle throughput" :value="state.summary.trafficTotal" hint="Entry, retrieval, and reservation activity combined." />
      <MetricCard label="AGV online" :value="state.summary.agvOnline" hint="Digital twin coordinates are refreshed with the dispatch feed." />
      <MetricCard label="Live alerts" :value="state.summary.alertCount" hint="Safety, device, and order anomalies." tone="alert" />
    </div>

    <div class="dashboard-layout">
      <article class="surface">
        <div class="section-head compact">
          <div>
            <h2>Traffic forecasting</h2>
            <p>Live traffic history and the next six prediction windows drive pre-dispatch decisions.</p>
          </div>
          <span class="status-pill stable">API online</span>
        </div>
        <TrafficChart :history="state.forecast.history" :prediction="state.forecast.prediction" />
      </article>

      <article class="surface">
        <div class="section-head compact">
          <h2>Control loop</h2>
        </div>
        <div class="flow-stack" style="margin-top:16px;">
          <div class="flow-step done"><b>Edge AI perception</b><span>Plate OCR and safety-zone recognition push structured events into the backend.</span></div>
          <div class="flow-step done"><b>Order orchestration</b><span>Orders, billing, and owner actions are now API-backed instead of local-only mutations.</span></div>
          <div class="flow-step active"><b>Dispatch decision</b><span>Pre-dispatch and VIP retrieval tasks are inserted into the live AGV queue.</span></div>
          <div class="flow-step"><b>Digital twin sync</b><span>Slots, AGV positions, and alerts reflect the same source of truth used by the admin console.</span></div>
        </div>
      </article>
    </div>

    <div class="dashboard-layout lower">
      <article class="surface">
        <div class="section-head compact">
          <h2>Slot overview</h2>
          <RouterLink class="text-button" to="/twin">Open twin ></RouterLink>
        </div>
        <SlotGrid :slots="state.slots" />
      </article>
      <article class="surface">
        <div class="section-head compact">
          <h2>Event stream</h2>
          <span class="status-pill">Live</span>
        </div>
        <EventStream :events="state.events" />
      </article>
    </div>
  </section>
</template>
