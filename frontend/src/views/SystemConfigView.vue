<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { getters, loadAuditLogs, setDeviceStatus, state } from "../stores/parkingStore";
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
    label: "服务连接",
    value: zhText(state.onlineMode),
    detail: "优先连接后端服务，断连时使用本地数据保障可用。",
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
    detail: "摄像头、闸机、充电桩事件实时汇总。",
    tone: "stable",
  },
  {
    label: "调度队列",
    value: state.queue.length,
    detail: "车主请求、预调度和 VIP 插队进入同一队列。",
    tone: "stable",
  },
]);

const nodeKeyword = ref("");
const deviceKeyword = ref("");
const eventKeyword = ref("");

const filteredNodes = computed(() => {
  const k = nodeKeyword.value.trim().toLowerCase();
  return state.systemNodes.filter(
    (n) => !k || `${n.name} ${zhText(n.detail)} ${zhText(n.latency)}`.toLowerCase().includes(k),
  );
});

const filteredEvents = computed(() => {
  const k = eventKeyword.value.trim().toLowerCase();
  return state.devices.events.filter(
    (e) => !k || `${e.eventCode} ${e.deviceId} ${zhText(e.message)} ${e.severity}`.toLowerCase().includes(k),
  );
});

function rawStatusOf(value) {
  return String(value || "").toUpperCase();
}

function isOnline(value) {
  const s = rawStatusOf(value);
  return s === "ONLINE" || s === "READY" || s === "OPEN" || s === "CLOSED" || s === "AVAILABLE" || s === "CHARGING";
}

function statusTone(value) {
  const s = rawStatusOf(value);
  if (s === "OFFLINE") return "danger";
  if (s === "MAINTENANCE") return "warning";
  return "stable";
}

const deviceControls = computed(() => [
  ...state.devices.cameras.map((c) => ({
    type: "camera", id: c.cameraId, kind: "摄像头", protocol: c.profile,
    status: c.status, telemetry: `${c.fps} FPS / ${c.codec}`,
  })),
  ...state.devices.gates.map((g) => ({
    type: "gate", id: g.gateId, kind: "闸机", protocol: g.protocol,
    status: g.gateState, telemetry: `排队 ${g.queueDepth} / 急停 ${g.estopArmed ? "是" : "否"}`,
  })),
  ...state.devices.chargers.map((c) => ({
    type: "charger", id: c.chargerId, kind: "充电桩", protocol: c.protocol,
    status: c.connectorStatus, telemetry: `${c.powerKw} kW / ${c.sessionKwh} kWh`,
  })),
]);

const filteredDeviceControls = computed(() => {
  const k = deviceKeyword.value.trim().toLowerCase();
  return deviceControls.value.filter(
    (d) => !k || `${d.id} ${d.kind} ${d.protocol} ${d.status}`.toLowerCase().includes(k),
  );
});

const deviceBusy = ref("");
const deviceError = ref("");

async function changeDeviceStatus(device, status) {
  deviceBusy.value = device.id;
  deviceError.value = "";
  const result = await setDeviceStatus(device.type, device.id, status);
  if (!result.ok) deviceError.value = result.error;
  deviceBusy.value = "";
  await loadAuditLogs();
}

const auditKeyword = ref("");
const filteredAuditLogs = computed(() => {
  const k = auditKeyword.value.trim().toLowerCase();
  return state.auditLogs.filter(
    (log) => !k || `${log.username} ${log.method} ${log.path} ${log.status}`.toLowerCase().includes(k),
  );
});

function formatAuditTime(value) {
  if (!value) return "--";
  return String(value).replace("T", " ").slice(0, 19);
}

onMounted(() => {
  loadAuditLogs();
});

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
          <p>填写任意 OpenAI 兼容接口（DeepSeek / 智谱 / 通义 / Kimi…）或 Anthropic Claude，即可驱动「AI 视觉中枢」车牌识别、车主智能助手与运营报表。未配置时由系统内置引擎提供识别与问答能力，服务不中断。</p>
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
            <option value="mock">系统内置引擎 (无需联网)</option>
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
          {{ aiTestResult.source === "api" ? `真实接口已连通 · ${aiTestResult.model}` : "系统内置引擎已应答" }}
        </b>
        <p>{{ aiTestResult.text }}</p>
        <small v-if="aiTestResult.error">原因：{{ aiTestResult.error }}</small>
      </div>
    </article>

    <section class="system-layout">
      <article class="surface">
        <div class="section-head">
          <div>
            <h2>服务节点</h2>
            <p>边缘视觉、PLC 控制、缓存与数据库同步等关键节点状态。</p>
          </div>
          <div class="search-field">
            <i class="fa-solid fa-magnifying-glass"></i>
            <input v-model="nodeKeyword" placeholder="搜索节点名称或状态" />
          </div>
        </div>
        <div class="node-grid">
          <div
            v-for="node in filteredNodes"
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
          <p v-if="!filteredNodes.length" class="system-empty">没有匹配的节点。</p>
        </div>
      </article>

      <article class="surface">
        <div class="section-head">
          <div>
            <h2>设备事件</h2>
            <p>摄像头、闸机、充电桩上报的实时事件。</p>
          </div>
          <div class="search-field">
            <i class="fa-solid fa-magnifying-glass"></i>
            <input v-model="eventKeyword" placeholder="搜索设备、事件码或内容" />
          </div>
        </div>
        <div class="queue-list">
          <div v-for="event in filteredEvents" :key="event.eventId" class="queue-item system-event">
            <div>
              <b>{{ zhText(event.eventCode) }} - {{ event.deviceId }}</b>
              <span>{{ zhText(event.message) }}</span>
            </div>
            <span class="status-pill" :class="event.severity === 'critical' || event.severity === 'high' ? 'warning' : 'stable'">
              {{ zhText(event.severity) }}
            </span>
          </div>
          <p v-if="!filteredEvents.length" class="system-empty">没有匹配的事件。</p>
        </div>
      </article>
    </section>

    <article class="surface">
      <div class="section-head">
        <div>
          <h2>现场设备清单</h2>
          <p>接入系统的摄像头、闸机、充电桩及其协议与遥测。</p>
        </div>
        <div class="search-field">
          <i class="fa-solid fa-magnifying-glass"></i>
          <input v-model="deviceKeyword" placeholder="搜索设备、类型、协议或状态" />
        </div>
      </div>
      <p v-if="deviceError" class="system-empty" style="color: var(--danger-red)">{{ deviceError }}</p>
      <div class="device-control-list">
        <div v-for="d in filteredDeviceControls" :key="`${d.type}-${d.id}`" class="device-control-row">
          <div class="device-control-main">
            <b>{{ d.id }}</b>
            <span>{{ d.kind }} · {{ d.protocol }} · {{ d.telemetry }}</span>
          </div>
          <span class="status-pill" :class="statusTone(d.status)">{{ zhText(d.status) }}</span>
          <div class="device-control-actions">
            <button
              class="ghost-button small"
              :disabled="deviceBusy === d.id || isOnline(d.status)"
              @click="changeDeviceStatus(d, 'ONLINE')"
            >
              <i class="fa-solid fa-play"></i> 启用
            </button>
            <button
              class="ghost-button small"
              :disabled="deviceBusy === d.id || rawStatusOf(d.status) === 'MAINTENANCE'"
              @click="changeDeviceStatus(d, 'MAINTENANCE')"
            >
              <i class="fa-solid fa-screwdriver-wrench"></i> 维护
            </button>
            <button
              class="ghost-button small danger"
              :disabled="deviceBusy === d.id || rawStatusOf(d.status) === 'OFFLINE'"
              @click="changeDeviceStatus(d, 'OFFLINE')"
            >
              <i class="fa-solid fa-power-off"></i> 停用
            </button>
          </div>
        </div>
      </div>
      <p v-if="!filteredDeviceControls.length" class="system-empty">没有匹配的设备。</p>
    </article>

    <article class="surface">
      <div class="section-head">
        <div>
          <h2>操作审计日志</h2>
          <p>记录每一次管理端写操作：操作账号、动作、目标接口与结果状态。</p>
        </div>
        <div class="search-field">
          <i class="fa-solid fa-magnifying-glass"></i>
          <input v-model="auditKeyword" placeholder="搜索账号、方法、路径或状态" />
        </div>
      </div>
      <div class="audit-list">
        <div class="audit-row audit-head">
          <span>时间</span><span>账号</span><span>动作</span><span>目标</span><span>结果</span>
        </div>
        <div v-for="log in filteredAuditLogs" :key="log.id" class="audit-row">
          <span>{{ formatAuditTime(log.createdAt) }}</span>
          <span><b>{{ log.username }}</b><em>{{ log.role }}</em></span>
          <span class="audit-method" :class="log.method.toLowerCase()">{{ log.method }}</span>
          <span class="audit-path">{{ log.path }}</span>
          <span class="status-pill" :class="log.status < 400 ? 'stable' : 'danger'">{{ log.status }}</span>
        </div>
      </div>
      <p v-if="!filteredAuditLogs.length" class="system-empty">暂无审计记录。</p>
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
  grid-template-columns: minmax(0, 1fr) minmax(340px, 1fr);
  gap: 20px;
}

.search-field {
  min-width: 220px;
  min-height: 38px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: #fff;
}

.search-field i {
  color: var(--text-muted);
}

.search-field input {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--text-main);
  outline: none;
  font-size: 13px;
}

.system-empty {
  margin: 6px 0 0;
  padding: 14px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
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

.device-control-list {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.device-control-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: #fff;
}

.device-control-main {
  flex: 1;
  min-width: 0;
}

.device-control-main b {
  display: block;
  color: var(--text-main);
  font-size: 14px;
}

.device-control-main span {
  color: var(--text-muted);
  font-size: 12px;
}

.device-control-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.status-pill.danger {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.12);
}

.ghost-button.small.danger {
  color: #b91c1c;
  border-color: rgba(239, 68, 68, 0.35);
}

.ghost-button.small.danger:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.08);
}

@media (max-width: 720px) {
  .device-control-row {
    flex-wrap: wrap;
  }
}

.audit-list {
  margin-top: 14px;
  display: grid;
  gap: 4px;
}

.audit-row {
  display: grid;
  grid-template-columns: 160px 140px 80px minmax(0, 1fr) 70px;
  gap: 12px;
  align-items: center;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.audit-row:not(.audit-head):hover {
  background: rgba(15, 23, 42, 0.03);
}

.audit-head {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  border-bottom: 1px solid var(--border-color);
  border-radius: 0;
}

.audit-row b {
  display: block;
  color: var(--text-main);
}

.audit-row em {
  font-style: normal;
  font-size: 11px;
  color: var(--text-muted);
}

.audit-method {
  font-weight: 700;
  font-size: 12px;
}

.audit-method.post {
  color: #2563eb;
}

.audit-method.delete {
  color: #dc2626;
}

.audit-method.put,
.audit-method.patch {
  color: #d97706;
}

.audit-path {
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .audit-row {
    grid-template-columns: 1fr 1fr;
  }

  .audit-head {
    display: none;
  }
}
</style>
