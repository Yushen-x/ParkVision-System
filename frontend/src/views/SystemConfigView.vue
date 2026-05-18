<script setup>
import { computed } from "vue";
import DataTable from "../components/DataTable.vue";
import { state } from "../stores/parkingStore";
import { zhText } from "../utils/localize";

const accessRows = computed(() =>
  state.accessList.map((item) => [item.plateNo, zhText(item.listType), zhText(item.userType), zhText(item.validUntil), zhText(item.remark)]),
);

const deviceRows = computed(() => [
  ...state.devices.cameras.map((camera) => [
    camera.cameraId,
    "摄像头",
    camera.profile,
    zhText(camera.status),
    `${camera.fps} 帧/秒 / ${camera.bitrateKbps} kbps`,
  ]),
  ...state.devices.gates.map((gate) => [
    gate.gateId,
    "闸机",
    gate.protocol,
    zhText(gate.gateState),
    `${gate.endpoint} | 排队 ${gate.queueDepth}`,
  ]),
  ...state.devices.chargers.map((charger) => [
    charger.chargerId,
    "充电桩",
    charger.protocol,
    zhText(charger.connectorStatus),
    `${charger.endpoint} | ${charger.powerKw} kW`,
  ]),
]);
</script>

<template>
  <section class="dispatch-layout">
    <article class="surface">
      <div class="section-head">
        <div>
          <h2><i class="fa-solid fa-shield-halved" style="color:var(--brand); margin-right:8px;"></i>准入与信任名单</h2>
          <p>白名单、黑名单和临时入场策略持久化在后端数据库，并与管理台共用。</p>
        </div>
      </div>
      <div class="table-wrap" style="margin-top:16px;">
        <DataTable :headers="['车牌', '名单类型', '用户类型', '有效期', '备注']" :rows="accessRows" />
      </div>
    </article>

    <aside>
      <article class="surface" style="height: 100%;">
        <div class="section-head compact">
          <h2>节点健康</h2>
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
                {{ zhText(node.latency) }}
              </span>
            </div>
            <span :style="node.level === 'warning' ? 'color:var(--danger-red);' : ''">{{ zhText(node.detail) }}</span>
          </div>
        </div>
      </article>
    </aside>

    <article class="surface wide">
      <div class="section-head">
        <div>
          <h2><i class="fa-solid fa-network-wired" style="color:var(--brand); margin-right:8px;"></i>现场设备</h2>
          <p>摄像头、PLC 闸机和 OCPP 充电桩都从设备总览接口读取。</p>
        </div>
      </div>
      <div class="table-wrap" style="margin-top:16px;">
        <DataTable :headers="['设备', '类型', '协议', '状态', '遥测']" :rows="deviceRows" />
      </div>
    </article>

    <article class="surface wide">
      <div class="section-head compact">
          <div>
          <h2>近期现场事件</h2>
          <p>事件流持久化在服务端，反映设备侧模拟结果，不是前端临时备注。</p>
        </div>
      </div>
      <div class="queue-list" style="margin-top:16px;">
        <div v-for="event in state.devices.events" :key="event.eventId" class="queue-item" style="grid-template-columns: minmax(0, 1fr) auto;">
          <div>
            <b>{{ zhText(event.eventCode) }} - {{ event.deviceId }}</b>
            <span>{{ zhText(event.message) }}</span>
          </div>
          <span class="status-pill" :class="event.severity === 'critical' || event.severity === 'high' ? 'warning' : 'stable'">
            {{ zhText(event.severity) }}
          </span>
        </div>
      </div>
    </article>
  </section>
</template>
