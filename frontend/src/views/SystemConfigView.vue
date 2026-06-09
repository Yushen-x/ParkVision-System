<script setup>
import { computed, reactive, ref } from "vue";
import DataTable from "../components/DataTable.vue";
import { getters, state } from "../stores/parkingStore";
import { zhText } from "../utils/localize";
import {
  aiChat,
  aiConfig,
  aiStatusLabel,
  isAiLive,
  PROVIDER_PRESETS,
  resetAiConfig,
  saveAiConfig,
} from "../services/aiClient";

const aiForm = reactive({ ...aiConfig });
const aiSaved = ref(false);
const aiTesting = ref(false);
const aiTestResult = ref(null);

const aiStatus = computed(() => aiStatusLabel());
const aiLive = computed(() => isAiLive());
const activePreset = computed(() => PROVIDER_PRESETS[aiForm.provider] || {});

function onProviderChange() {
  const preset = PROVIDER_PRESETS[aiForm.provider];
  aiForm.baseURL = preset?.baseURL || "";
  aiForm.model = preset?.model || "";
  aiTestResult.value = null;
}

function persistAi() {
  saveAiConfig({ ...aiForm });
  aiSaved.value = true;
  window.setTimeout(() => (aiSaved.value = false), 2000);
}

function resetAi() {
  resetAiConfig();
  Object.assign(aiForm, aiConfig);
  aiTestResult.value = null;
}

async function testAi() {
  persistAi();
  aiTesting.value = true;
  aiTestResult.value = null;
  try {
    aiTestResult.value = await aiChat({
      system: "你是 ParkVision 智能停车助手，回答简洁。",
      messages: [{ role: "user", content: "用一句话确认你已经就绪。" }],
      context: { freeCount: getters.freeCount.value, totalSlots: state.slots.length },
    });
  } finally {
    aiTesting.value = false;
  }
}

const healthCards = computed(() => [
  {
    label: "后端模式",
    value: zhText(state.onlineMode),
    detail: "前端优先调用真实 API，服务不可用时切换到本地兜底数据。",
    tone: state.onlineMode.includes("Fallback") ? "warning" : "stable",
  },
  {
    label: "安全联锁",
    value: state.emergency ? "急停中" : "正常",
    detail: "急停会同步影响视觉、闸机和 AGV 放行条件。",
    tone: state.emergency ? "warning" : "stable",
  },
  {
    label: "设备事件",
    value: state.devices.events.length,
    detail: "摄像头、闸机、充电桩事件作为演示证据展示。",
    tone: "stable",
  },
  {
    label: "调度队列",
    value: state.queue.length,
    detail: "车主请求、预调度和 VIP 插队都会进入同一队列。",
    tone: "stable",
  },
]);

const linkRows = computed(() => [
  ["车牌识别", state.visionResult.cameraId, state.visionResult.plate, zhText(state.visionResult.action)],
  ["车主订单", state.indoorRoute.orderNo, state.indoorRoute.slotId, zhText(state.indoorRoute.status)],
  ["计费预览", state.pricingPreview.orderNo, `￥${Number(state.pricingPreview.totalAmount || 0).toFixed(2)}`, zhText(state.pricingPreview.pricingWindow)],
  ["室内导航", state.indoorRoute.targetGate, `${state.indoorRoute.remainingMeters}m`, zhText(state.indoorRoute.safetyMessage)],
]);

const deviceRows = computed(() => [
  ...state.devices.cameras.map((camera) => [
    camera.cameraId,
    "摄像头",
    camera.profile,
    zhText(camera.status),
    `${camera.fps} FPS / ${camera.codec} / ${camera.lastPlate}`,
  ]),
  ...state.devices.gates.map((gate) => [
    gate.gateId,
    "闸机",
    gate.protocol,
    zhText(gate.gateState),
    `${gate.endpoint} / 排队 ${gate.queueDepth} / 急停 ${gate.estopArmed ? "是" : "否"}`,
  ]),
  ...state.devices.chargers.map((charger) => [
    charger.chargerId,
    "充电桩",
    charger.protocol,
    zhText(charger.connectorStatus),
    `${charger.endpoint} / ${charger.powerKw} kW / ${charger.sessionKwh} kWh`,
  ]),
]);

const evidence = [
  ["前端演示", "用户点击模拟入场、取车、临停取物、VIP 插队"],
  ["业务服务", "订单、调度、计费、导航状态被统一刷新"],
  ["设备网关", "摄像头/闸机/充电桩遥测转成结构化事件"],
  ["数据库台账", "订单、支付、告警、计费明细在管理台可追溯"],
];
</script>

<template>
  <section class="system-status-page">
    <div class="system-health-grid">
      <article v-for="card in healthCards" :key="card.label" class="surface system-health-card" :class="card.tone">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <p>{{ card.detail }}</p>
      </article>
    </div>

    <article class="surface ai-config-card">
      <div class="section-head">
        <div>
          <h2><i class="fa-solid fa-robot" style="color:var(--brand); margin-right:8px;"></i>AI 大模型接入</h2>
          <p>填写任意 OpenAI 兼容接口（DeepSeek / 智谱 / 通义 / Kimi…）或 Anthropic Claude，即可驱动「AI 视觉中枢」车牌识别、车主智能助手与运营报表。未配置时全部走内置模拟，演示不中断。</p>
        </div>
        <span class="status-pill" :class="aiLive ? 'stable' : 'warning'">{{ aiStatus }}</span>
      </div>

      <div class="ai-form">
        <label class="ai-field ai-toggle-field">
          <span>启用真实接口</span>
          <button type="button" class="ai-switch" :class="{ on: aiForm.enabled }" @click="aiForm.enabled = !aiForm.enabled">
            <i></i>
          </button>
        </label>

        <label class="ai-field">
          <span>服务商</span>
          <select v-model="aiForm.provider" @change="onProviderChange">
            <option value="mock">内置模拟 (无需联网)</option>
            <option value="openai">OpenAI 兼容 (DeepSeek / 智谱 / 通义 …)</option>
            <option value="anthropic">Anthropic Claude</option>
          </select>
        </label>

        <label class="ai-field">
          <span>接口地址 Base URL</span>
          <input v-model.trim="aiForm.baseURL" type="text" :placeholder="activePreset.baseURL || '例如 https://api.deepseek.com/v1'" :disabled="aiForm.provider === 'mock'" />
        </label>

        <label class="ai-field">
          <span>API Key</span>
          <input v-model.trim="aiForm.apiKey" type="password" autocomplete="off" placeholder="仅保存在本机浏览器" :disabled="aiForm.provider === 'mock'" />
        </label>

        <label class="ai-field">
          <span>模型名称</span>
          <input v-model.trim="aiForm.model" type="text" :placeholder="activePreset.model || '模型 ID'" :disabled="aiForm.provider === 'mock'" />
        </label>

        <label class="ai-field">
          <span>温度 {{ Number(aiForm.temperature).toFixed(1) }}</span>
          <input v-model.number="aiForm.temperature" type="range" min="0" max="1" step="0.1" />
        </label>
      </div>

      <p v-if="activePreset.hint" class="ai-hint"><i class="fa-solid fa-circle-info"></i> {{ activePreset.hint }}</p>

      <div class="ai-actions">
        <button class="primary-button small" @click="persistAi">
          <i class="fa-solid fa-floppy-disk"></i> {{ aiSaved ? "已保存" : "保存配置" }}
        </button>
        <button class="ghost-button small" :disabled="aiTesting" @click="testAi">
          <i class="fa-solid" :class="aiTesting ? 'fa-spinner fa-spin' : 'fa-plug'"></i>
          {{ aiTesting ? "测试中..." : "测试连接" }}
        </button>
        <button class="ghost-button small" @click="resetAi"><i class="fa-solid fa-rotate-left"></i> 重置</button>
        <span class="ai-key-note"><i class="fa-solid fa-lock"></i> 密钥仅存于本机，不会上传</span>
      </div>

      <div v-if="aiTestResult" class="ai-test-result" :class="aiTestResult.source === 'api' ? 'ok' : 'mock'">
        <b>
          <i class="fa-solid" :class="aiTestResult.source === 'api' ? 'fa-circle-check' : 'fa-circle-exclamation'"></i>
          {{ aiTestResult.source === "api" ? `真实接口已连通 · ${aiTestResult.model}` : "已回退到内置模拟" }}
        </b>
        <p>{{ aiTestResult.text }}</p>
        <small v-if="aiTestResult.error">原因：{{ aiTestResult.error }}</small>
      </div>
    </article>

    <section class="system-layout">
      <article class="surface">
        <div class="section-head">
          <div>
            <h2>数据链路总览</h2>
            <p>这个页面不再做“配置项堆叠”，而是说明前面每个演示画面背后对应哪条数据。</p>
          </div>
          <span class="status-pill stable">可追溯</span>
        </div>
        <div class="table-wrap">
          <DataTable :headers="['业务链路', '来源', '当前值', '状态说明']" :rows="linkRows" />
        </div>
      </article>

      <aside class="surface">
        <div class="section-head compact">
          <div>
            <h2>演示证据链</h2>
            <p>讲解时可以按这四步说明系统不是静态页面。</p>
          </div>
        </div>
        <div class="evidence-flow">
          <div v-for="([title, detail], index) in evidence" :key="title">
            <span>{{ String(index + 1).padStart(2, "0") }}</span>
            <b>{{ title }}</b>
            <small>{{ detail }}</small>
          </div>
        </div>
      </aside>
    </section>

    <section class="system-layout">
      <article class="surface">
        <div class="section-head">
          <div>
            <h2>服务节点健康</h2>
            <p>展示边缘视觉、PLC 控制、缓存/数据库同步等关键节点是否支撑当前演示。</p>
          </div>
        </div>
        <div class="node-grid">
          <div
            v-for="node in state.systemNodes"
            :key="node.name"
            class="node-card"
            :class="{ warning: node.level === 'warning' }"
          >
            <div>
              <b>{{ node.name }}</b>
              <span>{{ zhText(node.detail) }}</span>
            </div>
            <strong>{{ zhText(node.latency) }}</strong>
          </div>
        </div>
      </article>

      <article class="surface">
        <div class="section-head">
          <div>
            <h2>近期设备事件</h2>
            <p>设备事件可以解释 AI 识别、闸机放行和充电计费从哪里来。</p>
          </div>
        </div>
        <div class="queue-list">
          <div v-for="event in state.devices.events" :key="event.eventId" class="queue-item system-event">
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

    <article class="surface">
      <div class="section-head">
        <div>
          <h2>现场设备接入</h2>
          <p>不是让观众配置设备，而是证明摄像头、闸机、充电桩都参与了业务闭环。</p>
        </div>
      </div>
      <div class="table-wrap">
        <DataTable :headers="['设备', '类型', '协议', '状态', '遥测']" :rows="deviceRows" />
      </div>
    </article>
  </section>
</template>

<style scoped>
.system-status-page {
  display: grid;
  gap: 20px;
}

.system-health-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.system-health-card {
  min-height: 148px;
}

.system-health-card span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.system-health-card strong {
  display: block;
  margin: 10px 0 8px;
  color: var(--text-main);
  font-family: "Outfit", "Noto Sans SC", sans-serif;
  font-size: 28px;
}

.system-health-card p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.55;
}

.system-health-card.stable {
  border-left: 4px solid var(--safety-green);
}

.system-health-card.warning {
  border-left: 4px solid var(--danger-red);
}

.system-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(340px, 0.85fr);
  gap: 20px;
}

.evidence-flow {
  display: grid;
  gap: 12px;
}

.evidence-flow div,
.node-card {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: linear-gradient(180deg, #fff, #f8fafc);
}

.evidence-flow span {
  display: inline-flex;
  margin-bottom: 10px;
  color: var(--brand);
  font-family: "Outfit", sans-serif;
  font-size: 12px;
  font-weight: 800;
}

.evidence-flow b,
.evidence-flow small {
  display: block;
}

.evidence-flow b {
  color: var(--text-main);
  font-size: 15px;
}

.evidence-flow small {
  margin-top: 5px;
  color: var(--text-muted);
  line-height: 1.5;
}

.node-grid {
  display: grid;
  gap: 12px;
}

.node-card {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  border-left: 4px solid var(--safety-green);
}

.node-card.warning {
  border-left-color: var(--danger-red);
  background: rgba(239, 68, 68, 0.05);
}

.node-card b,
.node-card span {
  display: block;
}

.node-card b {
  color: var(--text-main);
}

.node-card span {
  margin-top: 5px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
}

.node-card strong {
  color: var(--brand);
  font-family: "Outfit", sans-serif;
  white-space: nowrap;
}

.node-card.warning strong {
  color: var(--danger-red);
}

.system-event {
  grid-template-columns: minmax(0, 1fr) auto;
}

.ai-config-card {
  border-top: 3px solid var(--brand);
}

.ai-form {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.ai-field {
  display: grid;
  gap: 7px;
}

.ai-field > span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.ai-field input[type="text"],
.ai-field input[type="password"],
.ai-field select {
  height: 40px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-main);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease;
}

.ai-field input:focus,
.ai-field select:focus {
  border-color: var(--brand);
}

.ai-field input:disabled,
.ai-field select:disabled {
  background: rgba(0, 0, 0, 0.03);
  color: var(--text-muted);
  cursor: not-allowed;
}

.ai-field input[type="range"] {
  width: 100%;
  accent-color: var(--brand);
}

.ai-toggle-field {
  align-content: start;
}

.ai-switch {
  width: 50px;
  height: 28px;
  border-radius: 999px;
  border: none;
  background: rgba(0, 0, 0, 0.12);
  position: relative;
  cursor: pointer;
  transition: background 0.2s ease;
  padding: 0;
}

.ai-switch.on {
  background: var(--brand);
}

.ai-switch i {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fff;
  transition: left 0.2s ease;
}

.ai-switch.on i {
  left: 25px;
}

.ai-hint {
  margin: 14px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

.ai-hint i {
  color: var(--brand);
  margin-right: 6px;
}

.ai-actions {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.ai-key-note {
  color: var(--text-muted);
  font-size: 12px;
}

.ai-key-note i {
  margin-right: 5px;
}

.ai-test-result {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: rgba(0, 0, 0, 0.015);
}

.ai-test-result.ok {
  border-color: rgba(16, 185, 129, 0.4);
  background: rgba(16, 185, 129, 0.06);
}

.ai-test-result.mock {
  border-color: rgba(245, 158, 11, 0.4);
  background: rgba(245, 158, 11, 0.06);
}

.ai-test-result b {
  display: block;
  color: var(--text-main);
  font-size: 14px;
}

.ai-test-result.ok b i {
  color: var(--safety-green);
  margin-right: 6px;
}

.ai-test-result.mock b i {
  color: var(--warning-yellow);
  margin-right: 6px;
}

.ai-test-result p {
  margin: 8px 0 0;
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.6;
}

.ai-test-result small {
  display: block;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 12px;
}

@media (max-width: 1320px) {
  .system-health-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .system-layout {
    grid-template-columns: 1fr;
  }

  .ai-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .system-health-grid {
    grid-template-columns: 1fr;
  }

  .ai-form {
    grid-template-columns: 1fr;
  }
}
</style>
