<script setup>
import { computed } from "vue";
import { state } from "../stores/parkingStore";
import { zhText } from "../utils/localize";

const inboundGate = computed(() => state.devices.gates.find((gate) => gate.gateId.includes("IN")) || null);
const outboundGate = computed(() => state.devices.gates.find((gate) => gate.gateId.includes("OUT")) || null);
const activeCharger = computed(() => state.devices.chargers.find((charger) => charger.connectorStatus !== "Available") || null);
</script>

<template>
  <section class="dispatch-layout">
    <article class="surface">
      <div class="section-head">
        <div>
          <h2>实时调度队列</h2>
          <p>任务优先读取后端队列，包含预调度、入场存车和 VIP 插队任务。</p>
        </div>
      </div>
      <div class="queue-list" style="margin-top:16px;">
        <div v-for="(task, index) in state.queue" :key="`${task.plateNo}-${index}`" class="queue-item" :class="{ vip: task.vip }">
          <div class="queue-rank">{{ index + 1 }}</div>
          <div>
            <b>{{ zhText(task.type) }} - {{ task.plateNo }}</b>
            <span><i class="fa-regular fa-clock"></i> 预计等待：{{ task.wait }}</span>
          </div>
          <span class="queue-tag"><i class="fa-solid fa-tag"></i> {{ zhText(task.tag) }}</span>
        </div>
      </div>
    </article>

    <aside>
      <article class="surface" style="height: 100%;">
        <div class="section-head compact">
          <h2>现场放行条件</h2>
        </div>
        <div style="margin-top:16px;">
          <div class="strategy-card">
            <strong><i class="fa-solid fa-right-to-bracket" style="color:var(--brand); margin-right:6px;"></i>{{ inboundGate?.gateId || "入场闸机" }}</strong>
            <span>状态 {{ zhText(inboundGate?.gateState, "未知") }} | 排队 {{ inboundGate?.queueDepth ?? 0 }} | 决策 {{ zhText(inboundGate?.lastDecision, "未知") }}</span>
          </div>
          <div class="strategy-card">
            <strong><i class="fa-solid fa-right-from-bracket" style="color:var(--warning-yellow); margin-right:6px;"></i>{{ outboundGate?.gateId || "出场闸机" }}</strong>
            <span>状态 {{ zhText(outboundGate?.gateState, "未知") }} | 排队 {{ outboundGate?.queueDepth ?? 0 }} | 急停 {{ outboundGate?.estopArmed ? "已锁定" : "正常" }}</span>
          </div>
          <div class="strategy-card">
            <strong><i class="fa-solid fa-plug-circle-check" style="color:var(--safety-green); margin-right:6px;"></i>{{ activeCharger?.chargerId || "暂无活跃充电桩" }}</strong>
            <span>
              {{
                activeCharger
                  ? `${activeCharger.vehiclePlate || "无车辆"} | ${activeCharger.powerKw} kW | ${activeCharger.sessionKwh} kWh`
                  : "充电车位当前空闲。"
              }}
            </span>
          </div>
        </div>
      </article>
    </aside>
  </section>
</template>
