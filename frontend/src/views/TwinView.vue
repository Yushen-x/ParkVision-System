<script setup>
import { computed } from "vue";
import { state } from "../stores/parkingStore";

const gridCells = computed(() => {
  const cells = [];
  for (let index = 0; index < 144; index += 1) {
    let isAgv = false;
    let isLoaded = false;
    let status = "empty";

    if (state.slots[index]) {
      status = state.slots[index].status;
    }

    state.agvs.forEach((agv) => {
      const ax = Math.floor((agv.x / 100) * 12);
      const ay = Math.floor((agv.y / 100) * 12);
      const agvIndex = ay * 12 + ax;
      if (agvIndex === index) {
        isAgv = true;
        isLoaded = agv.loaded;
      }
    });

    cells.push({ id: index, status, isAgv, isLoaded });
  }
  return cells;
});
</script>

<template>
  <section class="twin-layout" :style="state.emergency ? 'box-shadow: inset 0 0 100px rgba(239,68,68,0.3); border-radius:12px; transition: all 0.5s;' : ''">
    <article class="surface wide" style="grid-column: 1 / 2; padding: 0; background: transparent; border: none; box-shadow: none;">
      <div class="view-3d-container" :style="state.emergency ? 'border-color: #ef4444;' : ''">
        <div class="grid-3d">
          <div v-for="cell in gridCells" :key="cell.id" class="cell" :class="[cell.status, { agv: cell.isAgv, loaded: cell.isLoaded }]"></div>
        </div>

        <div v-if="state.emergency" style="position:absolute; top:20px; left:20px; background:rgba(239,68,68,0.2); padding:10px 20px; border-radius:8px; border:1px solid #ef4444; color:#ef4444; font-weight:700; animation:blink 1s infinite alternate; font-family:'Orbitron', sans-serif; font-size: 20px;">
          <i class="fa-solid fa-triangle-exclamation"></i> EMERGENCY STOP
        </div>
      </div>
    </article>

    <aside style="display:flex; flex-direction:column; gap:20px;">
      <article class="surface" style="flex:1;">
        <div class="section-head compact">
          <h2>AGV fleet</h2>
        </div>
        <div class="agv-list" style="margin-top:16px;">
          <div v-for="agv in state.agvs" :key="agv.id" class="agv-card">
            <b><i class="fa-solid fa-robot" style="color:var(--brand); margin-right:8px;"></i>{{ agv.id }} | {{ agv.loaded ? "Loaded" : "Idle" }}</b>
            <span>Position [{{ Math.round(agv.x) }}, {{ Math.round(agv.y) }}] | {{ agv.task }}</span>
          </div>
        </div>
      </article>

      <article class="surface" style="padding:0; background:transparent; border:none; box-shadow:none;">
        <div class="safety-card" :class="{ danger: state.emergency }">
          <strong><i class="fa-solid" :class="state.emergency ? 'fa-triangle-exclamation' : 'fa-shield-check'"></i> {{ state.emergency ? "Safety stop active" : "Safety perimeter normal" }}</strong>
          <span>
            {{
              state.emergency
                ? "A simulated safety event paused AGV motion and locked the dispatch visualization."
                : "No intrusion is present in the handoff zone and the AGV fleet is moving under normal constraints."
            }}
          </span>
        </div>
      </article>
    </aside>
  </section>
</template>
