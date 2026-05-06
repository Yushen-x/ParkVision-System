<script setup>
import { state } from "../stores/parkingStore";
import { computed } from "vue";

const gridCells = computed(() => {
    // Generate a 12x12 grid representing slots, mostly empty but mapping to state.slots if possible
    const cells = [];
    for(let i=0; i<144; i++) {
        let isAgv = false;
        let isLoaded = false;
        let status = 'empty';
        
        // Map 1D to 2D roughly for visual, state.slots has 150 items
        if(state.slots[i]) {
            status = state.slots[i].status;
        }
        
        // Map AGVs
        state.agvs.forEach(agv => {
            // roughly map agv.x, agv.y (0-100) to 12x12
            const ax = Math.floor((agv.x / 100) * 12);
            const ay = Math.floor((agv.y / 100) * 12);
            const idx = ay * 12 + ax;
            if(idx === i) {
                isAgv = true;
                isLoaded = agv.load;
            }
        });
        
        cells.push({ id: i, status, isAgv, isLoaded });
    }
    return cells;
});
</script>

<template>
  <section class="twin-layout" :style="state.emergency ? 'box-shadow: inset 0 0 100px rgba(239,68,68,0.3); border-radius:12px; transition: all 0.5s;' : ''">
    <article class="surface wide" style="grid-column: 1 / 2; padding: 0; background: transparent; border: none; box-shadow: none;">
      <div class="view-3d-container" :style="state.emergency ? 'border-color: #ef4444;' : ''">
        <div class="grid-3d">
            <div v-for="cell in gridCells" :key="cell.id" 
                 class="cell" 
                 :class="[cell.status, { 'agv': cell.isAgv, 'loaded': cell.isLoaded }]">
            </div>
        </div>
        
        <div v-if="state.emergency" style="position:absolute; top:20px; left:20px; background:rgba(239,68,68,0.2); padding:10px 20px; border-radius:8px; border:1px solid #ef4444; color:#ef4444; font-weight:700; animation:blink 1s infinite alternate; font-family:'Orbitron', sans-serif; font-size: 20px;">
            <i class="fa-solid fa-triangle-exclamation"></i> EMERGENCY STOP
        </div>
      </div>
    </article>
    
    <aside style="display:flex; flex-direction:column; gap:20px;">
      <article class="surface" style="flex:1;">
        <div class="section-head compact">
          <h2>AGV 车队集群</h2>
        </div>
        <div class="agv-list" style="margin-top:16px;">
          <div v-for="agv in state.agvs" :key="agv.id" class="agv-card">
            <b><i class="fa-solid fa-robot" style="color:var(--brand); margin-right:8px;"></i> {{ agv.id }} · {{ agv.load ? "载车" : "空载" }}</b>
            <span>坐标 [{{ Math.round(agv.x) }}, {{ Math.round(agv.y) }}] · {{ agv.task }}</span>
          </div>
        </div>
      </article>

      <article class="surface" style="padding:0; background:transparent; border:none; box-shadow:none;">
        <div class="safety-card" :class="{ danger: state.emergency }">
          <strong><i class="fa-solid" :class="state.emergency ? 'fa-triangle-exclamation' : 'fa-shield-check'"></i> {{ state.emergency ? "安全急停已触发" : "边缘网关正常" }}</strong>
          <span>{{ state.emergency ? "检测到交接区异常闯入，YOLOv8 视觉告警。AGV 队列已物理冻结，等待人工复核解锁。" : "未发现人员闯入，系统心跳正常，闸机允许放行。" }}</span>
        </div>
      </article>
    </aside>
  </section>
</template>
