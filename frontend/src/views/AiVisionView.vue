<script setup>
import { computed } from "vue";
import { getters, runVision, state } from "../stores/parkingStore";

const freeCount = getters.freeCount;
const occupiedCount = getters.occupiedCount;
const visionJson = computed(() =>
  JSON.stringify(
    {
      requestId: `edge-${Date.now().toString().slice(-6)}`,
      cameraId: "gate-A-01",
      plate: state.visionResult.plate,
      confidence: state.visionResult.confidence,
      slotOccupancy: { free: freeCount.value, occupied: occupiedCount.value },
      intrusion: state.visionResult.intrusion,
      action: state.visionResult.action,
    },
    null,
    2,
  ),
);
</script>

<template>
  <section class="ai-layout">
    <article class="surface camera-panel">
      <div class="section-head">
        <div>
          <h2>边缘视觉模型实时渲染</h2>
          <p>YOLOv8 + OCR 推理节点展示，结构化 JSON 下发至调度中枢。</p>
        </div>
        <button class="primary-button small" @click="runVision"><i class="fa-solid fa-play"></i> 捕获视频帧</button>
      </div>
      <div class="camera-view">
        <div class="lane-line left"></div>
        <div class="lane-line right"></div>
        <div class="vehicle-shape">
          <span class="plate">{{ state.visionResult.plate }}</span>
        </div>
        <div class="detection-box plate-box">PLATE CONF {{ state.visionResult.confidence }}</div>
        <div class="detection-box vehicle-box">VEHICLE 0.96</div>
        <div class="intruder" :class="{ show: state.visionResult.intrusion }">PERSON</div>
        <div style="position:absolute; top:20px; right:20px; color:#ef4444; font-family:'Orbitron', sans-serif; font-size:14px; animation:blink 1s infinite;"><i class="fa-solid fa-circle"></i> REC</div>
      </div>
    </article>

    <aside style="display:flex; flex-direction:column; gap:20px;">
        <article class="surface" style="flex:1;">
        <div class="section-head compact">
            <h2>结构化输出 payload</h2>
            <span class="status-pill" :class="state.visionResult.intrusion ? 'warning' : 'stable'">
            {{ state.visionResult.intrusion ? "INTERVENTION REQUIRED" : "VERIFIED" }}
            </span>
        </div>
        <pre class="json-output">{{ visionJson }}</pre>
        </article>
    </aside>

    <article class="surface wide" style="margin-top:-5px;">
      <div class="module-row">
        <div><b><i class="fa-solid fa-car-side" style="color:var(--brand); margin-right:8px;"></i>双流车牌识别</b><span>端侧 YOLO 定位 + HyperLPR 识别号码，输出极高置信度。</span></div>
        <div><b><i class="fa-solid fa-border-none" style="color:var(--safety-green); margin-right:8px;"></i>空余泊位检测</b><span>基于车库俯视 ROI 区域遮挡判定，更新孪生系统的车位图谱。</span></div>
        <div><b><i class="fa-solid fa-person-falling-burst" style="color:var(--danger-red); margin-right:8px;"></i>活体闯入告警</b><span>交接区严格执行防夹防闯入，毫秒级下发硬件底层急停指令。</span></div>
      </div>
    </article>
  </section>
</template>
