<script setup>
import { computed } from "vue";
import { getters, state } from "../stores/parkingStore";

const currentOrder = getters.currentOrder;
const preview = computed(() => state.pricingPreview);
const formulaChips = computed(() => [
  { label: "Base", accent: "rgba(255,255,255,0.1)", color: "#fff" },
  {
    label: `Peak (${Number(preview.value.peakMultiplier || 1).toFixed(2)}x)`,
    accent: "rgba(239,68,68,0.2)",
    color: "var(--danger-red)",
  },
  { label: "EV", accent: "rgba(16,185,129,0.2)", color: "var(--safety-green)" },
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
          <h2>Dynamic pricing engine</h2>
          <p>{{ preview.explanation }}</p>
        </div>
        <span class="status-pill stable">{{ preview.pricingWindow }}</span>
      </div>
      <div style="padding: 20px; background: rgba(0,0,0,0.4); border-radius: 12px; margin-top: 16px; border: 1px solid rgba(255,255,255,0.05);">
        <p style="color:var(--text-muted); font-size:13px; margin-bottom:10px; text-transform:uppercase;">Backend-calculated invoice preview</p>
        <div style="font-size: 28px; font-family:'Orbitron', sans-serif; color: #fff; display:flex; align-items:center; gap:15px; flex-wrap:wrap;">
          <span style="color:var(--brand);">TOTAL</span>
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
            <h2><i class="fa-solid fa-receipt"></i> Live order context</h2>
            <p>The pricing page now reads the active order, charger session, and dispatch priority from backend state.</p>
          </div>
        </div>
        <div class="metric-list" style="grid-template-columns: 1fr;">
          <div><span>Order</span><strong>{{ preview.orderNo || currentOrder?.orderNo || "N/A" }}</strong></div>
          <div><span>Plate</span><strong>{{ preview.plateNo || currentOrder?.plateNo || "N/A" }}</strong></div>
          <div><span>Duration</span><strong>{{ durationLabel }}</strong></div>
          <div><span>Base amount</span><strong>CNY {{ Number(preview.baseAmount || 0).toFixed(2) }}</strong></div>
          <div><span>Current estimate</span><strong style="color:var(--brand);">CNY {{ Number(preview.totalAmount || 0).toFixed(2) }}</strong></div>
        </div>
      </article>

      <article class="surface">
        <div class="section-head">
          <div>
            <h2><i class="fa-solid fa-layer-group"></i> Applied components</h2>
            <p>Each component below is returned from the pricing preview endpoint instead of being hard-coded in the view.</p>
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
                {{ component.label }}
              </b>
              <span>{{ component.formula }}</span>
            </div>
            <strong style="color:#fff;">CNY {{ Number(component.amount || 0).toFixed(2) }}</strong>
          </div>
        </div>
      </article>
    </div>

    <article class="surface wide">
      <div class="section-head compact">
        <div>
          <h2>Rule source of truth</h2>
          <p>The operational preview above is explained by the same rule dataset shown in the admin console.</p>
        </div>
      </div>
      <div class="queue-list" style="margin-top:16px;">
        <div v-for="rule in state.pricingRules" :key="rule.name" class="queue-item" style="grid-template-columns: minmax(0, 1fr) auto;">
          <div>
            <b>{{ rule.name }}</b>
            <span>{{ rule.timeRange }} | {{ rule.method }} | {{ rule.extraPolicy }}</span>
          </div>
          <span class="status-pill stable">{{ rule.status }}</span>
        </div>
      </div>
    </article>
  </section>
</template>
