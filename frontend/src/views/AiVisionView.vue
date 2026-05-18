<script setup>
import { computed } from "vue";
import { getters, runVision, state } from "../stores/parkingStore";

const freeCount = getters.freeCount;
const occupiedCount = getters.occupiedCount;
const activeCamera = computed(
  () => state.devices.cameras.find((camera) => camera.cameraId === state.visionResult.cameraId) || state.devices.cameras[0],
);
const deviceEvents = computed(() => state.devices.events.slice(0, 3));

const modelCards = computed(() => [
  {
    label: "Vision model",
    title: "Plate OCR",
    metric: `${Math.round(state.visionResult.confidence * 1000) / 10}%`,
      detail: "The latest OCR result comes from the backend inference endpoint before falling back locally.",
  },
  {
    label: "Safety check",
    title: "Intrusion detection",
    metric: state.visionResult.intrusion ? "ALERT" : "CLEAR",
      detail: "The handoff zone can raise an emergency review signal without leaving the AI page isolated from the backend.",
  },
  {
    label: "Slot telemetry",
    title: "Occupancy feed",
    metric: `${occupiedCount.value}/${state.slots.length}`,
      detail: "Slot states refresh from the same source consumed by the dashboard and the digital twin.",
  },
  {
    label: "Forecast window",
    title: "Pre-dispatch lead",
    metric: "+30 min",
      detail: "Traffic prediction feeds the dispatch center so deep-slot vehicles can be moved ahead of the surge.",
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
      requestId: state.visionResult.requestId || `edge-${Date.now().toString().slice(-6)}`,
      cameraId: state.visionResult.cameraId || "gate-A-01",
      modelPipeline: ["YOLOv8-detector", "Plate-OCR", "ROI-safety-check"],
      plate: state.visionResult.plate,
      confidence: state.visionResult.confidence,
      detections: [
        { type: "vehicle", confidence: 0.96, bbox: [214, 112, 438, 286] },
        { type: "plate", confidence: state.visionResult.confidence, bbox: [278, 246, 382, 278] },
        { type: "person_intrusion", confidence: state.visionResult.intrusion ? 0.91 : 0.02 },
      ],
      slotOccupancy: { free: freeCount.value, occupied: occupiedCount.value },
      action: state.visionResult.action,
    },
    null,
    2,
  ),
);

const assistantItems = [
  ["User intent", "I want my car now, can you make it faster?"],
  ["Detected intent", "retrieve_car + vip_priority"],
  ["Tool call", "POST /api/dispatch/vip"],
  ["System response", "Priority AGV job inserted at the head of the queue"],
];

const pipeline = [
  "Camera capture",
  "Edge AI inference",
  "Structured JSON",
  "Backend decision",
  "Dispatch update",
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
            <h2>Live edge inference</h2>
            <p>Plate OCR, vehicle detection, and handoff-zone safety checks all write into the same operational story.</p>
          </div>
          <button class="primary-button small" :disabled="state.busy.vision" @click="runVision()">
            <i class="fa-solid" :class="state.busy.vision ? 'fa-spinner fa-spin' : 'fa-play'"></i>
            {{ state.busy.vision ? "Running..." : "Capture frame" }}
          </button>
        </div>

        <div class="camera-view ai-camera">
          <div class="camera-grid"></div>
          <div class="camera-tag">{{ activeCamera?.cameraId || state.visionResult.cameraId }} | {{ activeCamera?.profile || "LIVE" }}</div>
          <div class="lane-line left"></div>
          <div class="lane-line right"></div>
          <div class="safety-zone" :class="{ danger: state.visionResult.intrusion }">ROI SAFETY ZONE</div>
          <div class="vehicle-shape">
            <span class="plate">{{ state.visionResult.plate }}</span>
          </div>
          <div class="detection-box vehicle-box">VEHICLE 0.96</div>
          <div class="detection-box plate-box">PLATE {{ state.visionResult.confidence }}</div>
          <div class="human-box" :class="{ danger: state.visionResult.intrusion }">
            {{ state.visionResult.intrusion ? "PERSON RISK 0.91" : "PERSON CLEAR 0.02" }}
          </div>
          <div class="ai-overlay">
            <span>YOLOv8</span>
            <span>Plate OCR</span>
            <span>ROI Safety</span>
            <span>{{ activeCamera?.codec || "H.265" }}</span>
          </div>
        </div>
      </article>

      <aside class="side-stack">
        <article class="surface">
          <div class="section-head compact">
            <h2>Inference JSON</h2>
            <span class="status-pill" :class="state.visionResult.intrusion ? 'warning' : 'stable'">
              {{ state.visionResult.intrusion ? "ESTOP" : "VERIFIED" }}
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
            <h2>Forecast and pre-dispatch</h2>
            <p>The prediction window below is what feeds the relocation strategy used by the dispatch center.</p>
          </div>
          <span class="status-pill stable">Pre-dispatch ready</span>
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
          <b>Decision</b>
          <span>When the next 30-minute outbound window crosses the threshold, the backend can enqueue a relocation task before the handoff zone becomes congested.</span>
        </div>
      </article>

      <article class="surface">
        <div class="section-head compact">
          <div>
            <h2>Assistant intent routing</h2>
            <p>Natural-language requests can resolve into retrieval, VIP priority, billing, or reporting actions.</p>
          </div>
          <span class="status-pill stable">LLM ready</span>
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
            <h2>Field device events</h2>
            <p>Recent camera and gate events are read from the device telemetry stream.</p>
          </div>
        </div>
        <div class="intent-flow">
          <div v-for="event in deviceEvents" :key="event.eventId" class="intent-row">
            <span>{{ event.deviceId }}</span>
            <b>{{ event.eventCode }} | {{ event.message }}</b>
          </div>
        </div>
      </article>

      <article class="surface wide">
        <div class="section-head compact">
          <div>
            <h2>Cloud-edge path</h2>
            <p>The AI page is no longer a decorative shell. It now feeds the same backend flow that updates orders, alerts, and dispatch state.</p>
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
