<script setup>
import { computed } from "vue";
import { state } from "../stores/parkingStore";
import { zhText } from "../utils/localize";

const route = computed(() => state.indoorRoute);
const leadGate = computed(() => state.devices.gates.find((gate) => gate.gateId === route.value.targetGate) || state.devices.gates[0]);
const progressSegments = computed(() => {
  const completed = Math.max(0, Math.min(3, Number(route.value.completedSegments || 0)));
  return [0, 1, 2].map((index) => index < completed);
});
const etaLabel = computed(() => `${Math.floor((route.value.etaSeconds || 0) / 60)} 分钟`);
const agvLabel = computed(() => `${Math.max(1, Math.round((route.value.agvEtaSeconds || 0) / 60))} 分钟`);
</script>

<template>
  <section class="mobile-workbench">
    <div class="phone-frame">
      <div class="phone-status">
        <span>ParkVision</span>
        <span><i class="fa-solid fa-wifi"></i> 5G <i class="fa-solid fa-battery-full"></i></span>
      </div>
      <div class="phone-screen" style="background: #0f172a;">
        <div style="padding: 2rem 1.5rem 1rem; color: #fff;">
          <h2 style="font-size:22px; font-weight:700;"><i class="fa-solid fa-location-arrow" style="color:var(--brand); margin-right:8px;"></i>室内交接导航</h2>
          <p style="color:#94a3b8; font-size:13px; margin-top:5px;">距离 {{ zhText(route.handoffZone) }} 还剩 {{ route.remainingMeters }} 米。</p>
        </div>

        <div style="flex:1; position:relative; margin: 0 1rem 1rem; border-radius: 20px; background: rgba(30,41,59,0.5); border: 1px solid rgba(255,255,255,0.1); overflow:hidden;">
          <div style="position:absolute; width:200%; height:200%; background:conic-gradient(from 0deg, transparent 70%, rgba(56,189,248,0.3) 100%); top:-50%; left:-50%; animation: spin 4s linear infinite; pointer-events:none;"></div>

          <svg width="100%" height="100%" style="position:absolute; top:0; left:0;">
            <path d="M 50 350 Q 50 200 150 200 T 250 50" fill="transparent" stroke="rgba(56,189,248,0.2)" stroke-width="12" stroke-linecap="round"/>
            <path d="M 50 350 Q 50 200 150 200 T 250 50" fill="transparent" stroke="var(--brand)" stroke-width="4" stroke-dasharray="10, 10" class="dash-path"/>
          </svg>

          <div style="position:absolute; bottom:40px; left:35px; width:30px; height:30px; background:#fff; border-radius:50%; display:grid; place-items:center; color:#0f172a; box-shadow:0 0 15px #fff;"><i class="fa-solid fa-car"></i></div>
          <div
            style="position:absolute; top:35px; right:35px; width:40px; height:40px; border-radius:50%; display:grid; place-items:center; color:#fff;"
            :style="leadGate?.estopArmed ? 'background:var(--danger-red); box-shadow:0 0 20px var(--danger-red);' : 'background:var(--safety-green); box-shadow:0 0 20px var(--safety-green);'"
          >
            <i class="fa-solid" :class="leadGate?.estopArmed ? 'fa-triangle-exclamation' : 'fa-flag-checkered'"></i>
          </div>
          <div style="position:absolute; top:15px; right:20px; font-size:11px; font-weight:700;" :style="leadGate?.estopArmed ? 'color:var(--danger-red);' : 'color:var(--safety-green);'">
            {{ zhText(route.handoffZone) }}
          </div>
        </div>

        <div style="padding: 1.5rem; background:rgba(255,255,255,0.05); border-top-left-radius: 24px; border-top-right-radius: 24px; backdrop-filter:blur(10px);">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:1rem; gap:12px;">
            <div style="color:#fff; font-size:18px; font-weight:700;">{{ zhText(route.nextInstruction) }}</div>
            <div style="color:var(--brand); font-size:24px; font-weight:800; font-family:'Orbitron', sans-serif;">{{ route.walkingSpeedKph }} km/h</div>
          </div>
          <div style="display:flex; gap:10px;">
            <div
              v-for="(filled, index) in progressSegments"
              :key="index"
              style="flex:1; height:6px; border-radius:3px;"
              :style="filled ? 'background:var(--brand);' : 'background:rgba(255,255,255,0.1);'"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <div class="surface owner-detail" style="border:none; box-shadow:none; background:transparent;">
      <div class="section-head">
        <div>
          <h2 style="font-size:24px; margin-bottom:10px;">室内接驳导航</h2>
          <p style="font-size:15px; max-width:500px;">本页面读取后端导航快照、出场闸机状态和实时 AGV 到达时间，而不是固定占位文案。</p>
        </div>
      </div>
      <div class="metric-list" style="grid-template-columns: repeat(2, minmax(0, 1fr));">
        <div><span>当前订单</span><strong>{{ route.orderNo }}</strong></div>
        <div><span>车牌 / 车位</span><strong>{{ route.plateNo }} / {{ route.slotId }}</strong></div>
        <div><span>车主预计到达</span><strong>{{ etaLabel }}</strong></div>
        <div><span>AGV 预计到达</span><strong>{{ agvLabel }}</strong></div>
        <div><span>目标闸机</span><strong>{{ route.targetGate }}</strong></div>
        <div><span>闸机状态</span><strong>{{ zhText(leadGate?.gateState, "未知") }}</strong></div>
      </div>

      <div class="module-row" style="grid-template-columns: 1fr; margin-top:18px;">
        <div :style="leadGate?.estopArmed ? 'background:rgba(239,68,68,0.1); border-color:var(--danger-red);' : 'background:rgba(56,189,248,0.1); border-color:var(--brand);'">
          <b><i class="fa-solid" :class="leadGate?.estopArmed ? 'fa-triangle-exclamation' : 'fa-satellite-dish'"></i> 路线安全</b>
          <span>{{ zhText(route.safetyMessage) }}</span>
        </div>
        <div style="background:rgba(255,255,255,0.04); border-color:rgba(255,255,255,0.08);">
          <b><i class="fa-solid fa-robot"></i> AGV 同步</b>
          <span>{{ zhText(route.status) }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
@keyframes spin { 100% { transform: rotate(360deg); } }
.dash-path { animation: dashAnim 2s linear infinite; }
@keyframes dashAnim { to { stroke-dashoffset: -20; } }
</style>
