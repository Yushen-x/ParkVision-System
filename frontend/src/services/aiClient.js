import { reactive } from "vue";
import { parkvisionApi } from "../api/parkvisionApi";

/**
 * Unified, provider-agnostic AI client for ParkVision.
 *
 * Goals
 * - Work out-of-the-box with a built-in **mock** so every demo screen has data
 *   even with no network / no key.
 * - Let the user plug in a real LLM by filling the config form (System 页面).
 *   Three provider shapes are supported:
 *     - "mock"      : local canned responses, no network.
 *     - "openai"    : OpenAI-compatible /chat/completions (DeepSeek, 智谱 GLM,
 *                     通义千问 兼容模式, Kimi, OpenAI, 本地 vLLM/Ollama …).
 *     - "anthropic" : Claude Messages API (/v1/messages).
 *
 * NOTE (browser/CORS): calling a third-party LLM directly from the browser may
 * be blocked by CORS depending on the provider. Anthropic needs the
 * `anthropic-dangerous-direct-browser-access` header (set below); some OpenAI
 * compatible endpoints allow browser origins, others require a tiny backend
 * proxy. When a real call fails we transparently fall back to the mock so the
 * demo never breaks — the UI surfaces which path was used.
 */

const STORAGE_KEY = "pv-ai-config";

const DEFAULT_CONFIG = {
  enabled: false, // master switch; when false we always use the mock
  provider: "mock", // "mock" | "openai" | "anthropic"
  baseURL: "", // e.g. https://api.deepseek.com/v1  或  https://api.anthropic.com
  apiKey: "",
  model: "", // e.g. deepseek-chat / glm-4-flash / gpt-4o-mini / claude-opus-4-8
  temperature: 0.4,
};

// Sensible defaults per provider, surfaced as placeholders / quick-fill in the UI.
export const PROVIDER_PRESETS = {
  openai: {
    label: "OpenAI 兼容 (DeepSeek / 智谱 / 通义 / Kimi …)",
    baseURL: "https://api.deepseek.com/v1",
    model: "deepseek-chat",
    hint: "填写 /v1 结尾的接口地址，客户端会自动追加 /chat/completions。",
  },
  anthropic: {
    label: "Anthropic Claude",
    baseURL: "https://api.anthropic.com",
    model: "claude-opus-4-8",
    hint: "浏览器直连需服务端允许跨域；已附带 direct-browser-access 头。",
  },
  mock: {
    label: "系统内置引擎",
    baseURL: "",
    model: "parkvision-engine",
    hint: "未接入外部模型时，使用系统内置的识别与问答引擎。",
  },
};

function loadConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return { ...DEFAULT_CONFIG };
    return { ...DEFAULT_CONFIG, ...JSON.parse(raw) };
  } catch {
    return { ...DEFAULT_CONFIG };
  }
}

export const aiConfig = reactive(loadConfig());

export function saveAiConfig(patch = {}) {
  Object.assign(aiConfig, patch);
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...aiConfig }));
  } catch {
    /* storage blocked (private mode) — keep in-memory only */
  }
  return { ...aiConfig };
}

export function resetAiConfig() {
  return saveAiConfig({ ...DEFAULT_CONFIG });
}

/**
 * Server-side LLM proxy status. The backend keeps the API key (e.g. DeepSeek)
 * and forwards chat calls, so the browser never holds the key and CORS is a
 * non-issue. Populated by {@link probeBackendAi}.
 */
export const backendAi = reactive({ live: false, model: "" });

/** Ask the backend whether a server-side chat model is configured. */
export async function probeBackendAi() {
  try {
    const status = await parkvisionApi.aiStatus();
    backendAi.live = Boolean(status?.live);
    backendAi.model = status?.model || "";
  } catch {
    backendAi.live = false;
  }
  return backendAi.live;
}

/** True when the user explicitly configured a browser-side provider + key. */
function manualLive() {
  return Boolean(aiConfig.enabled && aiConfig.provider !== "mock" && aiConfig.apiKey && aiConfig.baseURL);
}

/** True when any real model is reachable (browser config or backend proxy). */
export function isAiLive() {
  return manualLive() || backendAi.live;
}

export function aiStatusLabel() {
  if (manualLive()) return `已接入 · ${aiConfig.model || PROVIDER_PRESETS[aiConfig.provider]?.model || aiConfig.provider}`;
  if (backendAi.live) return `已接入 · ${backendAi.model || "大模型"}`;
  return "AI 引擎就绪";
}

async function callBackendProxy({ system, messages, temperature }) {
  const data = await parkvisionApi.aiChat({ system, messages, temperature });
  const text = data?.text;
  if (typeof text !== "string" || !text) throw new Error("服务端代理无有效回复");
  return text.trim();
}

function effectiveModel() {
  return aiConfig.model || PROVIDER_PRESETS[aiConfig.provider]?.model || "";
}

function joinUrl(base, path) {
  return `${String(base || "").replace(/\/+$/, "")}${path}`;
}

// ---------------------------------------------------------------------------
// Real provider calls
// ---------------------------------------------------------------------------

async function callOpenAiChat({ system, messages, signal, temperature, maxTokens }) {
  const body = {
    model: effectiveModel(),
    temperature: temperature ?? aiConfig.temperature,
    max_tokens: maxTokens ?? 1024,
    stream: false,
    messages: [...(system ? [{ role: "system", content: system }] : []), ...messages],
  };
  const res = await fetch(joinUrl(aiConfig.baseURL, "/chat/completions"), {
    method: "POST",
    signal,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${aiConfig.apiKey}`,
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`OpenAI 兼容接口返回 ${res.status}`);
  const data = await res.json();
  const text = data?.choices?.[0]?.message?.content;
  if (typeof text !== "string") throw new Error("接口响应缺少 choices[0].message.content");
  return text.trim();
}

async function callAnthropicChat({ system, messages, signal, temperature, maxTokens }) {
  const body = {
    model: effectiveModel() || "claude-opus-4-8",
    max_tokens: maxTokens ?? 1024,
    ...(system ? { system } : {}),
    messages: messages.map((m) => ({ role: m.role === "assistant" ? "assistant" : "user", content: m.content })),
  };
  // temperature is accepted on Claude < 4.7; harmless to send on compatible proxies.
  if (typeof temperature === "number") body.temperature = temperature;
  const res = await fetch(joinUrl(aiConfig.baseURL || "https://api.anthropic.com", "/v1/messages"), {
    method: "POST",
    signal,
    headers: {
      "content-type": "application/json",
      "x-api-key": aiConfig.apiKey,
      "anthropic-version": "2023-06-01",
      "anthropic-dangerous-direct-browser-access": "true",
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`Anthropic 接口返回 ${res.status}`);
  const data = await res.json();
  const text = Array.isArray(data?.content)
    ? data.content.filter((b) => b.type === "text").map((b) => b.text).join("").trim()
    : "";
  if (!text) throw new Error("Anthropic 响应缺少文本块");
  return text;
}

// ---------------------------------------------------------------------------
// Mock responders (context-aware, rule-based)
// ---------------------------------------------------------------------------

function lastUserText(messages) {
  for (let i = messages.length - 1; i >= 0; i -= 1) {
    if (messages[i].role === "user") return String(messages[i].content || "");
  }
  return "";
}

function mockAssistantReply(messages, context = {}) {
  const q = lastUserText(messages);
  const ctx = context || {};
  const free = ctx.freeCount ?? "若干";
  const total = ctx.totalSlots ?? "—";
  const plate = ctx.plate || "当前车辆";
  const order = ctx.orderNo || "当前订单";
  const fee = ctx.fee != null ? `￥${Number(ctx.fee).toFixed(2)}` : "按时段动态计算";

  const has = (...kw) => kw.some((k) => q.includes(k));

  if (has("取车", "提车", "出库")) {
    return `已为${order}创建取车任务，搬运台车正前往对应车位，预计 1–2 分钟将 ${plate} 送达交接区。可在“履约中枢”查看 AGV 实时进度。`;
  }
  if (has("VIP", "优先", "插队", "快一点", "加急")) {
    return `已将${order}标记为 VIP 优先，任务已插入调度队首。高峰期可优先放行，预计较普通取车提前约 50%。`;
  }
  if (has("临停", "取物", "拿东西", "拿个")) {
    return `临停取物任务已下发，车辆会被调度到交接区且计费会话保持开启；办完后系统会自动回送复位入库。`;
  }
  if (has("空位", "车位", "停哪", "有位")) {
    return `当前约有 ${free}/${total} 个空闲车位。新能源车将优先匹配带充电桩的车位，入场时由视觉识别自动分配。`;
  }
  if (has("充电", "电量", "续航")) {
    return `本库每个车位均配充电桩。${plate} 若为新能源车，入库后可一键开启充电，费用并入停车账单，可在“动态计费”查看明细。`;
  }
  if (has("多少钱", "费用", "计费", "收费", "价格", "账单")) {
    return `${order}当前预估费用为 ${fee}，由“基础费 + 高峰系数 + 新能源/VIP 调整”动态合成，明细见“动态计费”页面。`;
  }
  if (has("报表", "统计", "数据", "营收", "周转")) {
    return `可在“管理台账”生成运营报表：涵盖车位周转率、营收、告警与充电桩利用率。需要我按某个时间段汇总吗？`;
  }
  if (has("你好", "您好", "在吗", "hi", "hello")) {
    return `您好，我是 ParkVision 智能助手。可以帮您取车、临停取物、查询空位与费用、申请 VIP 优先。请问需要什么？`;
  }
  return `已收到：“${q}”。我可以协助取车 / 临停取物 / VIP 优先 / 查询空位与费用。当前 ${free}/${total} 个空位，${order}预估 ${fee}。`;
}

const MOCK_PLATES = ["沪A·7686Z", "沪D·5218N", "苏M·9021X", "沪K·1314Q", "沪V·7780L", "浙B·6602H"];

function mockPlateResult() {
  const plate = MOCK_PLATES[Math.floor(Math.random() * MOCK_PLATES.length)];
  const confidence = Number((0.93 + Math.random() * 0.06).toFixed(3));
  return {
    plate,
    confidence,
    color: plate.includes("D") ? "绿牌(新能源)" : "蓝牌",
    boxes: [
      { label: "车辆", confidence: Number((0.95 + Math.random() * 0.04).toFixed(2)), box: [214, 112, 438, 286] },
      { label: "车牌", confidence, box: [278, 246, 382, 278] },
    ],
  };
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/**
 * Chat completion with graceful fallback.
 * @returns {Promise<{text:string, source:'api'|'mock', model:string, error?:string}>}
 */
export async function aiChat({ system, messages, context, signal, temperature, maxTokens } = {}) {
  const msgs = Array.isArray(messages) ? messages : [];
  if (!isAiLive()) {
    return { text: mockAssistantReply(msgs, context), source: "mock", model: "parkvision-engine" };
  }
  try {
    if (manualLive()) {
      const text =
        aiConfig.provider === "anthropic"
          ? await callAnthropicChat({ system, messages: msgs, signal, temperature, maxTokens })
          : await callOpenAiChat({ system, messages: msgs, signal, temperature, maxTokens });
      return { text, source: "api", model: effectiveModel() };
    }
    // Backend proxy path (server holds the key, e.g. DeepSeek).
    const text = await callBackendProxy({ system, messages: msgs, temperature });
    return { text, source: "api", model: backendAi.model || "大模型" };
  } catch (error) {
    return {
      text: mockAssistantReply(msgs, context),
      source: "mock",
      model: "parkvision-engine",
      error: error?.message || String(error),
    };
  }
}

/**
 * License-plate recognition from an image (data URL or remote URL).
 * Uses the configured vision model when live, otherwise a mock result.
 * @returns {Promise<{plate:string, confidence:number, color?:string, boxes:Array, source:'api'|'mock', raw?:string, error?:string}>}
 */
export async function aiVisionPlate({ imageDataUrl } = {}) {
  if (!isAiLive() || !imageDataUrl) {
    return { ...mockPlateResult(), source: "mock" };
  }

  const instruction =
    "你是车牌识别引擎。识别图片中机动车的车牌号与颜色，只返回 JSON：" +
    '{"plate":"省简称+号牌","confidence":0-1之间小数,"color":"蓝牌/绿牌(新能源)/黄牌"}。不要输出多余文字。';

  try {
    let raw = "";
    if (aiConfig.provider === "anthropic") {
      const m = /^data:(.+?);base64,(.*)$/.exec(imageDataUrl);
      const source = m
        ? { type: "base64", media_type: m[1], data: m[2] }
        : { type: "url", url: imageDataUrl };
      raw = await callAnthropicChat({
        system: instruction,
        messages: [{ role: "user", content: [{ type: "image", source }, { type: "text", text: "识别这张图片的车牌。" }] }],
        maxTokens: 300,
      });
    } else {
      raw = await callOpenAiChat({
        system: instruction,
        messages: [
          {
            role: "user",
            content: [
              { type: "image_url", image_url: { url: imageDataUrl } },
              { type: "text", text: "识别这张图片的车牌，只返回 JSON。" },
            ],
          },
        ],
        maxTokens: 300,
      });
    }

    const parsed = extractJson(raw);
    if (!parsed?.plate) throw new Error("模型未返回有效车牌");
    return {
      plate: String(parsed.plate),
      confidence: Number(parsed.confidence ?? 0.9),
      color: parsed.color || "—",
      boxes: Array.isArray(parsed.boxes) ? parsed.boxes : mockPlateResult().boxes,
      source: "api",
      raw,
    };
  } catch (error) {
    return { ...mockPlateResult(), source: "mock", error: error?.message || String(error) };
  }
}

function extractJson(text) {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    const match = text.match(/\{[\s\S]*\}/);
    if (!match) return null;
    try {
      return JSON.parse(match[0]);
    } catch {
      return null;
    }
  }
}
