<script setup>
import { computed } from "vue";
import { getters, runVision, state } from "../stores/parkingStore";
import { zhText } from "../utils/localize";

const freeCount = getters.freeCount;
const occupiedCount = getters.occupiedCount;
const activeCamera = computed(
  () => state.devices.cameras.find((camera) => camera.cameraId === state.visionResult.cameraId) || state.devices.cameras[0],
);
const deviceEvents = computed(() => state.devices.events.slice(0, 3));

const modelCards = computed(() => [
  {
    label: "视觉模型",
    title: "车牌 OCR",
    metric: `${Math.round(state.visionResult.confidence * 1000) / 10}%`,
    detail: "最新识别结果优先来自后端推理接口，服务不可用时才走本地兜底。",
  },
  {
    label: "安全复核",
    title: "人员入侵检测",
    metric: state.visionResult.intrusion ? "告警" : "正常",
    detail: "交接区异常会触发后端急停复核，AI 页面不是孤立演示。",
  },
  {
    label: "车位遥测",
    title: "占用状态流",
    metric: `${occupiedCount.value}/${state.slots.length}`,
    detail: "车位状态与运营首页、数字孪生共用同一个后端数据源。",
  },
  {
    label: "预测窗口",
    title: "预调度提前量",
    metric: "+30 min",
    detail: "车流预测会输入调度中心，在高峰到来前提前移动车辆。",
  },
]);

const forecastBars = computed(() =>
  state.forecast.prediction.map((value, index) => ({
    label: `T+${(index + 1) * 10}`,
    value,
    height: Math.max(56, value * 2.1),
    risk: value >= 55 ? "high" : value >= 43 ? "mid" : "low",
  })),
);

const visionJson = computed(() =>
  JSON.stringify(
    {
      请求编号: state.visionResult.requestId || `edge-${Date.now().toString().slice(-6)}`,
      摄像头编号: state.visionResult.cameraId || "gate-A-01",
      模型流水线: ["车辆检测", "车牌 OCR", "安全区复核"],
      车牌号: state.visionResult.plate,
      置信度: state.visionResult.confidence,
      识别结果: [
        { 类型: "车辆", 置信度: 0.96, 坐标框: [214, 112, 438, 286] },
        { 类型: "车牌", 置信度: state.visionResult.confidence, 坐标框: [278, 246, 382, 278] },
        { 类型: "人员入侵", 置信度: state.visionResult.intrusion ? 0.91 : 0.02 },
      ],
      车位占用: { 空闲: freeCount.value, 已占用: occupiedCount.value },
      后端动作: zhText(state.visionResult.action),
    },
    null,
    2,
  ),
);

const assistantItems = [
  ["用户意图", "我现在要取车，能不能快一点？"],
  ["识别意图", "取车 + VIP 优先级"],
  ["工具调用", "POST /api/dispatch/vip"],
  ["系统响应", "优先 AGV 任务已插入队首"],
];

const pipeline = [
  "摄像头采集",
  "边缘 AI 推理",
  "结构化结果",
  "后端决策",
  "调度更新",
];
</script>

<template>
  <section class="ai-page">
    <div class="ai-hero">
      <article v-for="card in modelCards" :key="card.title" class="surface ai-kpi">
        <span>{{ card.label }}</span>
        <strong>{{ card.metric }}</strong>
        <b>{{ card.title }}</b>
        <p>{{ card.detail }}</p>
      </article>
    </div>

    <section class="ai-layout">
      <article class="surface camera-panel">
        <div class="section-head">
          <div>
            <h2>实时边缘识别</h2>
            <p>车牌 OCR、车辆检测和交接区安全复核都会写入同一条后端业务链路。</p>
          </div>
          <button class="primary-button small" :disabled="state.busy.vision" @click="runVision()">
            <i class="fa-solid" :class="state.busy.vision ? 'fa-spinner fa-spin' : 'fa-play'"></i>
            {{ state.busy.vision ? "识别中..." : "采集画面" }}
          </button>
        </div>

        <div class="camera-view ai-camera">
          <div class="camera-grid"></div>
          <div class="camera-tag">{{ activeCamera?.cameraId || state.visionResult.cameraId }} | {{ activeCamera?.profile || "实时" }}</div>
          <div class="lane-line left"></div>
          <div class="lane-line right"></div>
          <div class="safety-zone" :class="{ danger: state.visionResult.intrusion }">安全复核区</div>
          <div class="vehicle-shape">
            <span class="plate">{{ state.visionResult.plate }}</span>
          </div>
          <div class="detection-box vehicle-box">车辆 0.96</div>
          <div class="detection-box plate-box">车牌 {{ state.visionResult.confidence }}</div>
          <div class="human-box" :class="{ danger: state.visionResult.intrusion }">
            {{ state.visionResult.intrusion ? "人员风险 0.91" : "人员安全 0.02" }}
          </div>
          <div class="ai-overlay">
            <span>YOLOv8 检测</span>
            <span>车牌 OCR</span>
            <span>安全区复核</span>
            <span>{{ activeCamera?.codec || "H.265" }}</span>
          </div>
        </div>
      </article>

      <aside class="side-stack">
        <article class="surface">
          <div class="section-head compact">
            <h2>推理结果 JSON</h2>
            <span class="status-pill" :class="state.visionResult.intrusion ? 'warning' : 'stable'">
              {{ state.visionResult.intrusion ? "急停" : "已校验" }}
            </span>
          </div>
          <pre class="json-output">{{ visionJson }}</pre>
        </article>
      </aside>
    </section>

    <section class="ai-bottom-grid">
      <article class="surface">
        <div class="section-head compact">
          <div>
            <h2>预测与预调度</h2>
            <p>下方预测窗口会输入调度中心，用于决定是否提前移动车辆。</p>
          </div>
          <span class="status-pill stable">预调度就绪</span>
        </div>
        <div class="forecast-panel">
          <div v-for="bar in forecastBars" :key="bar.label" class="forecast-column">
            <div class="bar-wrap">
              <span class="bar-value">{{ bar.value }}</span>
              <i :class="bar.risk" :style="{ height: `${bar.height}px` }"></i>
            </div>
            <small>{{ bar.label }}</small>
          </div>
        </div>
        <div class="decision-strip">
          <b>决策</b>
          <span>当未来 30 分钟出场需求超过阈值时，后端会提前创建移位任务，避免交接区拥堵。</span>
        </div>
      </article>

      <article class="surface">
        <div class="section-head compact">
          <div>
            <h2>助手意图路由</h2>
            <p>自然语言请求可以转成取车、VIP 优先、计费或报表操作。</p>
          </div>
          <span class="status-pill stable">助手就绪</span>
        </div>
        <div class="intent-flow">
          <div v-for="[label, value] in assistantItems" :key="label" class="intent-row">
            <span>{{ label }}</span>
            <b>{{ value }}</b>
          </div>
        </div>
      </article>

      <article class="surface">
        <div class="section-head compact">
          <div>
            <h2>现场设备事件</h2>
            <p>近期摄像头、闸机和充电桩事件来自设备遥测流。</p>
          </div>
        </div>
        <div class="intent-flow">
          <div v-for="event in deviceEvents" :key="event.eventId" class="intent-row">
            <span>{{ event.deviceId }}</span>
            <b>{{ zhText(event.eventCode) }} | {{ zhText(event.message) }}</b>
          </div>
        </div>
      </article>

      <article class="surface wide">
        <div class="section-head compact">
          <div>
            <h2>云边协同链路</h2>
            <p>AI 页面不再只是展示壳，而是会推动订单、告警和调度状态一起更新。</p>
          </div>
        </div>
        <div class="pipeline">
          <template v-for="(item, index) in pipeline" :key="item">
            <div class="pipeline-node">
              <span>{{ String(index + 1).padStart(2, "0") }}</span>
              <b>{{ item }}</b>
            </div>
            <i v-if="index < pipeline.length - 1" class="pipeline-arrow fa-solid fa-arrow-right"></i>
          </template>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.ai-page {
  display: grid;
  gap: 20px;
}

.ai-hero {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.ai-kpi {
  min-height: 168px;
  position: relative;
  overflow: hidden;
}

.ai-kpi::after {
  content: "";
  position: absolute;
  inset: auto -20% -50% -20%;
  height: 110px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.18), transparent 65%);
  pointer-events: none;
}

.ai-kpi span {
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
}

.ai-kpi strong {
  display: block;
  margin: 12px 0 8px;
  color: #fff;
  font-family: "Orbitron", sans-serif;
  font-size: 30px;
}

.ai-kpi b {
  display: block;
  color: #e2e8f0;
  font-size: 15px;
}

.ai-kpi p {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.side-stack {
  display: grid;
  gap: 20px;
}

.ai-camera {
  min-height: 460px;
}

.camera-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(56, 189, 248, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(56, 189, 248, 0.08) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(to bottom, transparent, #000 15%, #000 85%, transparent);
}

.camera-tag {
  position: absolute;
  left: 18px;
  top: 18px;
  padding: 7px 10px;
  border-radius: 8px;
  color: #fecaca;
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.35);
  font-family: "Orbitron", sans-serif;
  font-size: 12px;
}

.safety-zone {
  position: absolute;
  right: 9%;
  top: 22%;
  width: 150px;
  height: 230px;
  display: grid;
  place-items: start center;
  padding-top: 12px;
  border: 2px dashed rgba(16, 185, 129, 0.65);
  color: #86efac;
  background: rgba(16, 185, 129, 0.06);
  font-family: "Orbitron", sans-serif;
  font-size: 11px;
}

.safety-zone.danger {
  border-color: rgba(239, 68, 68, 0.9);
  color: #fecaca;
  background: rgba(239, 68, 68, 0.08);
  box-shadow: 0 0 28px rgba(239, 68, 68, 0.2);
}

.human-box {
  position: absolute;
  right: 12%;
  top: 55%;
  width: 112px;
  min-height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(16, 185, 129, 0.6);
  color: #86efac;
  background: rgba(16, 185, 129, 0.12);
  font-size: 11px;
  font-weight: 800;
}

.human-box.danger {
  border-color: var(--danger-red);
  color: #fecaca;
  background: rgba(239, 68, 68, 0.18);
}

.ai-overlay {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 18px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-overlay span {
  padding: 7px 10px;
  border-radius: 999px;
  color: #bae6fd;
  background: rgba(2, 132, 199, 0.16);
  border: 1px solid rgba(56, 189, 248, 0.25);
  font-size: 12px;
  font-weight: 700;
}

.json-output {
  min-height: 460px;
}

.ai-bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
  gap: 20px;
}

.ai-bottom-grid .wide {
  grid-column: 1 / -1;
}

.forecast-panel {
  min-height: 230px;
  display: flex;
  align-items: end;
  gap: 18px;
  padding: 18px 10px 4px;
  border-radius: 10px;
  background: rgba(2, 6, 23, 0.42);
  border: 1px solid var(--border-color);
}

.forecast-column {
  flex: 1;
  min-width: 42px;
  display: grid;
  justify-items: center;
  gap: 10px;
}

.bar-wrap {
  height: 160px;
  display: flex;
  align-items: end;
  justify-content: center;
  position: relative;
  width: 100%;
}

.bar-wrap i {
  width: 100%;
  max-width: 48px;
  border-radius: 7px 7px 2px 2px;
  display: block;
  transition: height 0.3s ease;
}

.bar-wrap i.low {
  background: linear-gradient(180deg, rgba(16, 185, 129, 0.95), rgba(16, 185, 129, 0.22));
}

.bar-wrap i.mid {
  background: linear-gradient(180deg, rgba(245, 158, 11, 0.95), rgba(245, 158, 11, 0.22));
}

.bar-wrap i.high {
  background: linear-gradient(180deg, rgba(239, 68, 68, 0.95), rgba(239, 68, 68, 0.22));
}

.bar-value {
  position: absolute;
  top: -2px;
  color: #fff;
  font-family: "Orbitron", sans-serif;
  font-size: 13px;
}

.forecast-column small {
  color: var(--text-muted);
  font-size: 12px;
}

.decision-strip {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 10px;
  color: #fde68a;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.28);
  line-height: 1.6;
}

.decision-strip b {
  margin-right: 12px;
  color: #fbbf24;
}

.intent-flow {
  display: grid;
  gap: 12px;
}

.intent-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid var(--border-color);
}

.intent-row span {
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}

.intent-row b {
  color: #f8fafc;
  font-size: 14px;
  line-height: 1.5;
}

.pipeline {
  display: grid;
  grid-template-columns: repeat(9, auto);
  align-items: center;
  gap: 14px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.pipeline-node {
  min-width: 148px;
  min-height: 84px;
  display: grid;
  align-content: center;
  gap: 8px;
  padding: 14px;
  border-radius: 10px;
  background: rgba(56, 189, 248, 0.08);
  border: 1px solid rgba(56, 189, 248, 0.22);
}

.pipeline-node span {
  color: var(--brand);
  font-family: "Orbitron", sans-serif;
  font-size: 12px;
}

.pipeline-node b {
  color: #fff;
  font-size: 14px;
}

.pipeline-arrow {
  color: rgba(56, 189, 248, 0.75);
}

@media (max-width: 1320px) {
  .ai-hero {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ai-bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .ai-hero,
  .ai-layout {
    grid-template-columns: 1fr;
  }

  .intent-row {
    grid-template-columns: 1fr;
  }
}
</style>
