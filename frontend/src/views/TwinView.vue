<script setup>
import { computed } from "vue";
import { state } from "../stores/parkingStore";
import { zhText } from "../utils/localize";

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

const safetyGate = computed(() => state.devices.gates.find((gate) => gate.gateId.includes("OUT")) || null);
</script>

<template>
  <section class="twin-layout" :style="state.emergency ? 'box-shadow: inset 0 0 100px rgba(239,68,68,0.3); border-radius:12px; transition: all 0.5s;' : ''">
    <article class="surface wide" style="grid-column: 1 / 2; padding: 0; background: transparent; border: none; box-shadow: none;">
      <div class="view-3d-container" :style="state.emergency ? 'border-color: #ef4444;' : ''">
        <div class="grid-3d">
          <div v-for="cell in gridCells" :key="cell.id" class="cell" :class="[cell.status, { agv: cell.isAgv, loaded: cell.isLoaded }]"></div>
        </div>

        <div v-if="state.emergency" style="position:absolute; top:20px; left:20px; background:rgba(239,68,68,0.2); padding:10px 20px; border-radius:8px; border:1px solid #ef4444; color:#ef4444; font-weight:700; animation:blink 1s infinite alternate; font-family:'Orbitron', sans-serif; font-size: 20px;">
          <i class="fa-solid fa-triangle-exclamation"></i> 紧急停车
        </div>
      </div>
    </article>

    <aside style="display:flex; flex-direction:column; gap:20px;">
      <article class="surface" style="flex:1;">
        <div class="section-head compact">
          <h2>AGV 车队</h2>
        </div>
        <div class="agv-list" style="margin-top:16px;">
          <div v-for="agv in state.agvs" :key="agv.id" class="agv-card">
            <b><i class="fa-solid fa-robot" style="color:var(--brand); margin-right:8px;"></i>{{ agv.id }} | {{ zhText(agv.mode) }}</b>
            <span>坐标 [{{ Math.round(agv.x) }}, {{ Math.round(agv.y) }}] | {{ zhText(agv.task) }}</span>
            <span>电量 {{ agv.batteryPct }}% | 速度 {{ Number(agv.velocityMps || 0).toFixed(2) }} m/s | 指令 {{ zhText(agv.lastCommand) }}</span>
          </div>
        </div>
      </article>

      <article class="surface" style="padding:0; background:transparent; border:none; box-shadow:none;">
        <div class="safety-card" :class="{ danger: state.emergency }">
          <strong><i class="fa-solid" :class="state.emergency ? 'fa-triangle-exclamation' : 'fa-shield-check'"></i> {{ state.emergency ? "安全停机生效" : "安全边界正常" }}</strong>
          <span>
            {{
              state.emergency
                ? "后端安全锁已生效，PLC 闸机输出会在复核通过前保持禁止。"
                : `交接区未检测到入侵，${safetyGate?.gateId || "出场闸机"} 当前状态为 ${zhText(safetyGate?.gateState, "就绪")}。`
            }}
          </span>
        </div>
      </article>
    </aside>
  </section>
</template>
