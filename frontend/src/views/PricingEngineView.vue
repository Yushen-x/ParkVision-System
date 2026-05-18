<script setup>
import { computed } from "vue";
import { getters, state } from "../stores/parkingStore";
import { zhMoney, zhText } from "../utils/localize";

const currentOrder = getters.currentOrder;
const preview = computed(() => state.pricingPreview);
const formulaChips = computed(() => [
  { label: "基础费", accent: "rgba(255,255,255,0.1)", color: "#fff" },
  {
    label: `高峰 (${Number(preview.value.peakMultiplier || 1).toFixed(2)}x)`,
    accent: "rgba(239,68,68,0.2)",
    color: "var(--danger-red)",
  },
  { label: "新能源", accent: "rgba(16,185,129,0.2)", color: "var(--safety-green)" },
  { label: "VIP", accent: "rgba(245,158,11,0.2)", color: "var(--warning-yellow)" },
]);

const durationLabel = computed(() => {
  const minutes = Number(preview.value.durationMinutes || 0);
  const hours = Math.floor(minutes / 60)
    .toString()
    .padStart(2, "0");
  const remain = String(minutes % 60).padStart(2, "0");
  return `${hours}:${remain}`;
});
</script>

<template>
  <section class="admin-grid">
    <article class="surface wide" style="background: linear-gradient(135deg, rgba(30,41,59,0.8), rgba(15,23,42,0.9)); border-color:var(--brand);">
      <div class="section-head compact">
        <div>
          <h2>动态计费引擎</h2>
          <p>{{ zhText(preview.explanation) }}</p>
        </div>
        <span class="status-pill stable">{{ zhText(preview.pricingWindow) }}</span>
      </div>
      <div style="padding: 20px; background: rgba(0,0,0,0.4); border-radius: 12px; margin-top: 16px; border: 1px solid rgba(255,255,255,0.05);">
        <p style="color:var(--text-muted); font-size:13px; margin-bottom:10px; text-transform:uppercase;">后端计算的费用预览</p>
        <div style="font-size: 28px; font-family:'Orbitron', sans-serif; color: #fff; display:flex; align-items:center; gap:15px; flex-wrap:wrap;">
          <span style="color:var(--brand);">合计</span>
          <span>=</span>
          <span
            v-for="chip in formulaChips"
            :key="chip.label"
            :style="`background:${chip.accent}; color:${chip.color}; padding:4px 12px; border-radius:8px;`"
          >
            {{ chip.label }}
          </span>
        </div>
      </div>
    </article>

    <div style="display:grid; grid-template-columns: repeat(2, 1fr); gap:20px; grid-column: 1/-1;">
      <article class="surface">
        <div class="section-head">
          <div>
            <h2><i class="fa-solid fa-receipt"></i> 实时订单上下文</h2>
            <p>计费页面读取后端订单、充电会话和调度优先级，不再使用静态占位。</p>
          </div>
        </div>
        <div class="metric-list" style="grid-template-columns: 1fr;">
          <div><span>订单号</span><strong>{{ preview.orderNo || currentOrder?.orderNo || "暂无" }}</strong></div>
          <div><span>车牌号</span><strong>{{ preview.plateNo || currentOrder?.plateNo || "暂无" }}</strong></div>
          <div><span>停车时长</span><strong>{{ durationLabel }}</strong></div>
          <div><span>基础金额</span><strong>{{ zhMoney(preview.baseAmount) }}</strong></div>
          <div><span>当前预估</span><strong style="color:var(--brand);">{{ zhMoney(preview.totalAmount) }}</strong></div>
        </div>
      </article>

      <article class="surface">
        <div class="section-head">
          <div>
            <h2><i class="fa-solid fa-layer-group"></i> 费用组成</h2>
            <p>下方每一项都由计费预览接口返回，而不是页面硬编码。</p>
          </div>
        </div>
        <div class="queue-list">
          <div v-for="component in preview.components" :key="component.label" class="queue-item" style="grid-template-columns: 1fr auto;">
            <div>
              <b
                :style="
                  component.accent === 'peak'
                    ? 'color:var(--danger-red);'
                    : component.accent === 'charging'
                      ? 'color:var(--safety-green);'
                      : component.accent === 'vip'
                        ? 'color:var(--warning-yellow);'
                        : ''
                "
              >
                {{ zhText(component.label) }}
              </b>
              <span>{{ zhText(component.formula) }}</span>
            </div>
            <strong style="color:#fff;">{{ zhMoney(component.amount) }}</strong>
          </div>
        </div>
      </article>
    </div>

    <article class="surface wide">
      <div class="section-head compact">
        <div>
          <h2>规则数据源</h2>
          <p>上方费用预览由管理台展示的同一套计费规则解释。</p>
        </div>
      </div>
      <div class="queue-list" style="margin-top:16px;">
        <div v-for="rule in state.pricingRules" :key="rule.name" class="queue-item" style="grid-template-columns: minmax(0, 1fr) auto;">
          <div>
            <b>{{ zhText(rule.name) }}</b>
            <span>{{ zhText(rule.timeRange) }} | {{ zhText(rule.method) }} | {{ zhText(rule.extraPolicy) }}</span>
          </div>
          <span class="status-pill stable">{{ zhText(rule.status) }}</span>
        </div>
      </div>
    </article>
  </section>
</template>
