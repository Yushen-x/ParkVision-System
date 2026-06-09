<script setup>
import { computed, onBeforeUnmount, ref } from "vue";
import { getters, registerEntry, state } from "../stores/parkingStore";
import { zhText } from "../utils/localize";
import { aiVisionPlate } from "../services/aiClient";

// ---------------------------------------------------------------------------
// 入场识别：可播放的识别流程（驶入 → 扫描 → 框出车牌 → 放行建单）
// ---------------------------------------------------------------------------
const PLATES = [
  { plate: "沪A·7686Z", confidence: 0.983, energy: "Fuel", color: "蓝牌" },
  { plate: "沪D·5218N", confidence: 0.971, energy: "Electric", color: "绿牌·新能源" },
  { plate: "苏M·9021X", confidence: 0.965, energy: "Fuel", color: "蓝牌" },
  { plate: "沪K·1314Q", confidence: 0.978, energy: "Fuel", color: "蓝牌" },
  { plate: "浙B·6602H", confidence: 0.959, energy: "Electric", color: "绿牌·新能源" },
];

const phase = ref("idle"); // idle | incoming | scanning | detected | passed
const playing = ref(false);
const current = ref(null);
const records = ref([]);
let recIndex = 0;
let timers = [];

const phaseText = computed(() => {
  switch (phase.value) {
    case "incoming":
      return "检测到车辆驶入入口…";
    case "scanning":
      return "正在扫描车牌区域…";
    case "detected":
      return `识别成功 · ${current.value?.plate}`;
    case "passed":
      return "已自动建单 · 道闸抬杆放行";
    default:
      return "等待车辆进入识别区";
  }
});

function schedule(fn, ms) {
  timers.push(window.setTimeout(fn, ms));
}

function startRecognition() {
  if (playing.value) return;
  playing.value = true;
  const pick = PLATES[recIndex % PLATES.length];
  recIndex += 1;
  current.value = pick;
  phase.value = "incoming";

  schedule(() => (phase.value = "scanning"), 950);
  schedule(() => (phase.value = "detected"), 1900);
  schedule(() => {
    phase.value = "passed";
    const gate = state.devices.gates?.[0]?.gateId || "GATE-IN-01";
    registerEntry({ plateNo: pick.plate, energyType: pick.energy });
    records.value.unshift({
      time: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", second: "2-digit" }),
      plate: pick.plate,
      confidence: pick.confidence,
      color: pick.color,
      gate,
    });
    records.value = records.value.slice(0, 8);
  }, 3000);
  schedule(() => {
    phase.value = "idle";
    playing.value = false;
  }, 4400);
}

onBeforeUnmount(() => {
  timers.forEach((t) => window.clearTimeout(t));
  timers = [];
});

// KPI（基于识别记录派生，避免硬编码静态值）
const kpis = computed(() => {
  const recs = records.value;
  const avg = recs.length ? recs.reduce((s, r) => s + r.confidence, 0) / recs.length : 0.974;
  return [
    { label: "今日识别车辆", value: 1286 + recs.length, hint: "入口/出口摄像头累计", icon: "fa-car-side" },
    { label: "平均识别置信度", value: `${(avg * 100).toFixed(1)}%`, hint: "车牌 OCR 置信度均值", icon: "fa-bullseye" },
    { label: "自动放行率", value: "99.2%", hint: "无需人工干预的比例", icon: "fa-gauge-high" },
    { label: "异常拦截", value: state.emergency ? 1 : 0, hint: "交接区入侵 / 复核", icon: "fa-shield-halved" },
  ];
});

const freeCount = getters.freeCount;

// ---------------------------------------------------------------------------
// 上传识别
// ---------------------------------------------------------------------------
const plateFile = ref(null);
const plateImage = ref("");
const plateBusy = ref(false);
const plateResult = ref(null);

function onPlateFile(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  plateResult.value = null;
  const reader = new FileReader();
  reader.onload = () => {
    plateImage.value = String(reader.result || "");
    void recognizePlate();
  };
  reader.readAsDataURL(file);
}

async function recognizePlate() {
  if (!plateImage.value || plateBusy.value) return;
  plateBusy.value = true;
  try {
    plateResult.value = await aiVisionPlate({ imageDataUrl: plateImage.value });
  } finally {
    plateBusy.value = false;
  }
}

function clearPlate() {
  plateImage.value = "";
  plateResult.value = null;
  if (plateFile.value) plateFile.value.value = "";
}
</script>

<template>
  <section class="ai-page">
    <div class="ai-kpis">
      <article v-for="kpi in kpis" :key="kpi.label" class="surface ai-kpi">
        <div class="ai-kpi-icon"><i class="fa-solid" :class="kpi.icon"></i></div>
        <div>
          <span>{{ kpi.label }}</span>
          <strong>{{ kpi.value }}</strong>
          <small>{{ kpi.hint }}</small>
        </div>
      </article>
    </div>

    <section class="ai-grid">
      <article class="surface cam-card">
        <div class="section-head">
          <div>
            <h2>入口车牌识别</h2>
            <p>实时检测驶入车辆，OCR 识别车牌后自动建单并联动道闸放行——可点击播放完整识别过程。</p>
          </div>
          <button class="primary-button small" :disabled="playing" @click="startRecognition">
            <i class="fa-solid" :class="playing ? 'fa-spinner fa-spin' : 'fa-play'"></i>
            {{ playing ? "识别中..." : "开始识别" }}
          </button>
        </div>

        <div class="cam-stage" :class="phase">
          <div class="cam-grid"></div>
          <div class="cam-tag"><i class="fa-solid fa-video"></i> CAM-IN-01 · 入口</div>
          <div class="lane left"></div>
          <div class="lane right"></div>

          <div class="gate-post"></div>
          <div class="gate-bar"></div>

          <div class="cam-car">
            <span class="windshield"></span>
            <span class="roof"></span>
            <span class="plate-tag">{{ current?.plate || "" }}</span>
          </div>

          <div class="scan-line"></div>
          <div class="det-box vehicle"><span>车辆 {{ ((current?.confidence || 0.96) * 100).toFixed(0) }}%</span></div>
          <div class="det-box plate"><span>车牌</span></div>

          <div class="plate-readout">
            <b>{{ current?.plate }}</b>
            <em>{{ current?.color }} · 置信度 {{ ((current?.confidence || 0) * 100).toFixed(1) }}%</em>
          </div>

          <div class="cam-ribbon" :class="phase">
            <i
              class="fa-solid"
              :class="phase === 'detected' || phase === 'passed' ? 'fa-circle-check' : phase === 'idle' ? 'fa-circle-pause' : 'fa-magnifying-glass'"
            ></i>
            {{ phaseText }}
          </div>
        </div>

        <div class="cam-foot">
          <div><span>识别模型</span><b>YOLOv8 + CRNN OCR</b></div>
          <div><span>当前空位</span><b>{{ freeCount }} 个</b></div>
          <div><span>放行联动</span><b>道闸 / 立体库入库</b></div>
        </div>
      </article>

      <aside class="ai-side">
        <article class="surface upload-card">
          <div class="section-head compact">
            <div>
              <h2>图片识别</h2>
              <p>上传车辆图片识别车牌。</p>
            </div>
          </div>
          <label class="up-drop" :class="{ filled: plateImage }">
            <input ref="plateFile" type="file" accept="image/*" hidden @change="onPlateFile" />
            <template v-if="!plateImage">
              <i class="fa-solid fa-cloud-arrow-up"></i>
              <b>点击上传图片</b>
            </template>
            <template v-else>
              <img :src="plateImage" alt="上传图片" />
              <div v-if="plateBusy" class="up-scan"><i class="fa-solid fa-spinner fa-spin"></i> 识别中…</div>
            </template>
          </label>
          <div v-if="plateResult" class="up-result">
            <div class="up-plate" :class="{ green: String(plateResult.color || '').includes('绿') }">{{ plateResult.plate }}</div>
            <div class="up-meta">
              <span>置信度 {{ (plateResult.confidence * 100).toFixed(1) }}%</span>
              <span>{{ plateResult.color || "—" }}</span>
            </div>
            <button class="ghost-button small" @click="clearPlate"><i class="fa-solid fa-xmark"></i> 清除</button>
          </div>
        </article>

        <article class="surface rec-card">
          <div class="section-head compact">
            <div>
              <h2>识别记录</h2>
              <p>最近通过入口识别的车辆。</p>
            </div>
          </div>
          <div v-if="records.length" class="rec-list">
            <div v-for="(r, i) in records" :key="i" class="rec-item">
              <div class="rec-plate">{{ r.plate }}</div>
              <div class="rec-info">
                <b>{{ (r.confidence * 100).toFixed(1) }}% · {{ r.gate }}</b>
                <span>{{ r.time }} · {{ r.color }}</span>
              </div>
              <span class="rec-pass"><i class="fa-solid fa-circle-check"></i> 放行</span>
            </div>
          </div>
          <p v-else class="rec-empty">点击「开始识别」演示一次入场识别。</p>
        </article>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.ai-page {
  display: grid;
  gap: 20px;
}

.ai-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.ai-kpi {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 96px;
}

.ai-kpi-icon {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: var(--brand);
  background: rgba(79, 70, 229, 0.1);
  font-size: 18px;
}

.ai-kpi span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.ai-kpi strong {
  display: block;
  margin: 4px 0 3px;
  color: var(--text-main);
  font-family: "Outfit", sans-serif;
  font-size: 24px;
}

.ai-kpi small {
  color: var(--text-muted);
  font-size: 12px;
}

.ai-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.85fr);
  gap: 20px;
  align-items: start;
}

/* ---- 摄像头舞台 ---- */
.cam-stage {
  position: relative;
  margin-top: 16px;
  height: 440px;
  border-radius: 14px;
  overflow: hidden;
  background: linear-gradient(180deg, #0f172a, #1e293b);
}

.cam-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.12) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.12) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: linear-gradient(to bottom, transparent, #000 18%, #000 82%, transparent);
}

.cam-tag {
  position: absolute;
  left: 16px;
  top: 14px;
  z-index: 4;
  padding: 6px 10px;
  border-radius: 8px;
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.3);
  font-size: 12px;
  font-weight: 700;
}

.lane {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 3px;
  background: repeating-linear-gradient(180deg, rgba(250, 204, 21, 0.6) 0 24px, transparent 24px 48px);
}

.lane.left {
  left: 33%;
}

.lane.right {
  right: 33%;
}

/* 道闸 */
.gate-post {
  position: absolute;
  top: 96px;
  left: 28%;
  width: 8px;
  height: 64px;
  border-radius: 3px;
  background: #94a3b8;
  z-index: 3;
}

.gate-bar {
  position: absolute;
  top: 100px;
  left: 28%;
  width: 150px;
  height: 8px;
  border-radius: 4px;
  transform-origin: left center;
  background: repeating-linear-gradient(90deg, #ef4444 0 16px, #f8fafc 16px 32px);
  transition: transform 0.7s ease;
  z-index: 3;
}

.cam-stage.passed .gate-bar {
  transform: rotate(-72deg);
}

/* 车辆 */
.cam-car {
  position: absolute;
  left: 50%;
  bottom: -150px;
  width: 96px;
  height: 150px;
  margin-left: -48px;
  border-radius: 16px;
  background: linear-gradient(180deg, #60a5fa, #2563eb);
  box-shadow: 0 10px 30px rgba(37, 99, 235, 0.4);
  transition: bottom 1s ease, opacity 0.6s ease;
  z-index: 2;
}

.cam-stage.incoming .cam-car,
.cam-stage.scanning .cam-car,
.cam-stage.detected .cam-car {
  bottom: 150px;
}

.cam-stage.passed .cam-car {
  bottom: 470px;
  opacity: 0;
}

.cam-car .windshield {
  position: absolute;
  top: 18px;
  left: 14px;
  right: 14px;
  height: 34px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.45);
}

.cam-car .roof {
  position: absolute;
  top: 60px;
  left: 18px;
  right: 18px;
  height: 44px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.12);
}

.cam-car .plate-tag {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  padding: 2px 6px;
  border-radius: 4px;
  background: #fde047;
  color: #1e293b;
  font-size: 10px;
  font-weight: 800;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.cam-stage.scanning .cam-car .plate-tag,
.cam-stage.detected .cam-car .plate-tag {
  opacity: 1;
}

/* 扫描线 */
.scan-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, #22d3ee, transparent);
  box-shadow: 0 0 16px #22d3ee;
  opacity: 0;
  z-index: 3;
}

.cam-stage.scanning .scan-line {
  opacity: 1;
  animation: scanSweep 1s ease-in-out;
}

@keyframes scanSweep {
  0% {
    top: 120px;
  }
  100% {
    top: 320px;
  }
}

/* 检测框 */
.det-box {
  position: absolute;
  left: 50%;
  margin-left: -56px;
  width: 112px;
  border: 2px solid #22d3ee;
  border-radius: 6px;
  opacity: 0;
  transform: scale(0.92);
  transition: all 0.3s ease;
  z-index: 3;
}

.det-box span {
  position: absolute;
  top: -20px;
  left: -2px;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 700;
  color: #0f172a;
  background: #22d3ee;
  border-radius: 4px;
  white-space: nowrap;
}

.det-box.vehicle {
  top: 150px;
  height: 150px;
}

.det-box.plate {
  top: 268px;
  height: 26px;
  width: 76px;
  margin-left: -38px;
  border-color: #fde047;
}

.det-box.plate span {
  background: #fde047;
}

.cam-stage.detected .det-box,
.cam-stage.passed .det-box.vehicle {
  opacity: 1;
  transform: scale(1);
}

/* 识别读数 */
.plate-readout {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%) translateX(20px);
  padding: 14px 18px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.78);
  border: 1px solid rgba(34, 211, 238, 0.4);
  opacity: 0;
  transition: all 0.4s ease;
  z-index: 4;
}

.cam-stage.detected .plate-readout,
.cam-stage.passed .plate-readout {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
}

.plate-readout b {
  display: block;
  color: #fff;
  font-family: "Outfit", sans-serif;
  font-size: 22px;
  letter-spacing: 1px;
}

.plate-readout em {
  display: block;
  margin-top: 4px;
  color: #67e8f9;
  font-style: normal;
  font-size: 12px;
}

.cam-ribbon {
  position: absolute;
  left: 16px;
  bottom: 16px;
  z-index: 4;
  padding: 9px 14px;
  border-radius: 999px;
  color: #e2e8f0;
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.3);
  font-size: 13px;
  font-weight: 600;
}

.cam-ribbon i {
  margin-right: 7px;
}

.cam-ribbon.detected,
.cam-ribbon.passed {
  color: #6ee7b7;
  border-color: rgba(16, 185, 129, 0.45);
}

.cam-foot {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 16px;
}

.cam-foot div {
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.015);
  border: 1px solid var(--border-color);
}

.cam-foot span {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}

.cam-foot b {
  display: block;
  margin-top: 5px;
  color: var(--text-main);
  font-size: 14px;
}

/* ---- 侧栏 ---- */
.ai-side {
  display: grid;
  gap: 20px;
}

.up-drop {
  position: relative;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 8px;
  min-height: 150px;
  margin-top: 14px;
  border-radius: 12px;
  border: 2px dashed rgba(79, 70, 229, 0.3);
  background: rgba(79, 70, 229, 0.03);
  cursor: pointer;
  overflow: hidden;
}

.up-drop.filled {
  border-style: solid;
  border-color: var(--border-color);
  background: #0f172a;
}

.up-drop > i {
  font-size: 26px;
  color: var(--brand);
}

.up-drop b {
  color: var(--text-main);
  font-size: 14px;
}

.up-drop img {
  width: 100%;
  max-height: 220px;
  object-fit: contain;
}

.up-scan {
  position: absolute;
  inset: auto 0 0 0;
  padding: 8px;
  text-align: center;
  color: #fff;
  background: rgba(79, 70, 229, 0.85);
  font-size: 13px;
  font-weight: 700;
}

.up-result {
  margin-top: 14px;
  display: grid;
  gap: 10px;
  justify-items: center;
}

.up-plate {
  width: 100%;
  padding: 12px;
  text-align: center;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  font-family: "Outfit", sans-serif;
  font-size: 24px;
  letter-spacing: 2px;
  font-weight: 800;
}

.up-plate.green {
  background: linear-gradient(135deg, #10b981, #059669);
}

.up-meta {
  display: flex;
  gap: 16px;
  color: var(--text-muted);
  font-size: 13px;
}

.rec-list {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.rec-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.015);
  border: 1px solid var(--border-color);
}

.rec-plate {
  padding: 5px 8px;
  border-radius: 6px;
  background: #1e293b;
  color: #fde047;
  font-family: "Outfit", sans-serif;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.rec-info {
  flex: 1;
  min-width: 0;
}

.rec-info b {
  display: block;
  color: var(--text-main);
  font-size: 13px;
}

.rec-info span {
  color: var(--text-muted);
  font-size: 12px;
}

.rec-pass {
  color: var(--safety-green);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.rec-empty {
  margin-top: 14px;
  color: var(--text-muted);
  font-size: 13px;
}

@media (max-width: 1180px) {
  .ai-kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ai-grid {
    grid-template-columns: 1fr;
  }
}
</style>
