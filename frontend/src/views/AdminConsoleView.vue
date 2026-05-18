<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";
import DataTable from "../components/DataTable.vue";
import { generateAdminReport, state } from "../stores/parkingStore";

const tab = ref("report");
const query = ref("Compare the revenue impact of VIP retrieval in the last 7 days");
const chartRef = ref(null);
let chart;

const tableConfig = computed(() => {
  const configs = {
    orders: {
      headers: ["Order no", "Plate", "Event", "Slot", "Status", "Amount"],
      rows: state.adminOrders.map((order) => [
        order.orderNo,
        order.plateNo,
        order.event,
        order.slotId,
        order.status,
        order.amount,
      ]),
    },
    pricing: {
      headers: ["Rule", "Window", "Method", "Extra policy", "Status"],
      rows: state.pricingRules.map((rule) => [
        rule.name,
        rule.timeRange,
        rule.method,
        rule.extraPolicy,
        rule.status,
      ]),
    },
    alerts: {
      headers: ["Alert no", "Type", "Content", "Status", "Level"],
      rows: state.alerts.map((alert) => [
        alert.alertNo,
        alert.type,
        alert.content,
        alert.status,
        alert.level,
      ]),
    },
    access: {
      headers: ["Plate", "List type", "User type", "Valid until", "Remark"],
      rows: state.accessList.map((item) => [
        item.plateNo,
        item.listType,
        item.userType,
        item.validUntil,
        item.remark,
      ]),
    },
  };
  return configs[tab.value] || configs.orders;
});

async function handleGenerateReport() {
  await generateAdminReport(query.value);
  await nextTick();
  renderChart();
}

function renderChart() {
  if (!chartRef.value || !state.adminReport) return;

  if (!chart) {
    chart = echarts.init(chartRef.value, null, { backgroundColor: "transparent" });
  }

  chart.setOption({
    tooltip: { trigger: "axis" },
    legend: {
      data: ["Previous week", "Current week"],
      textStyle: { color: "#94a3b8" },
    },
    grid: { left: "3%", right: "4%", bottom: "3%", top: "15%", containLabel: true },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: state.adminReport.labels,
      axisLine: { lineStyle: { color: "rgba(255,255,255,0.1)" } },
      axisLabel: { color: "#94a3b8" },
    },
    yAxis: {
      type: "value",
      axisLabel: { formatter: "CNY {value}", color: "#94a3b8" },
      splitLine: { lineStyle: { color: "rgba(255,255,255,0.05)" } },
    },
    series: [
      {
        name: "Previous week",
        type: "line",
        data: state.adminReport.previousWeekRevenue,
        smooth: true,
        itemStyle: { color: "#94a3b8" },
      },
      {
        name: "Current week",
        type: "line",
        data: state.adminReport.currentWeekRevenue,
        smooth: true,
        areaStyle: { opacity: 0.2 },
        itemStyle: { color: "#38bdf8" },
      },
    ],
  });
}

function handleResize() {
  chart?.resize();
}

onMounted(async () => {
  await nextTick();
  renderChart();
  window.addEventListener("resize", handleResize);
});

watch(
  () => state.adminReport,
  async () => {
    await nextTick();
    renderChart();
  },
  { deep: true },
);

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  chart?.dispose();
});
</script>

<template>
  <section class="admin-grid">
    <article class="surface wide" style="padding: 0; background: transparent; border: none; box-shadow: none; backdrop-filter: none;">
      <div class="admin-tabs">
        <button class="tab" :class="{ active: tab === 'report' }" @click="tab = 'report'"><i class="fa-solid fa-wand-magic-sparkles" style="margin-right:6px;"></i>AI report</button>
        <button class="tab" :class="{ active: tab === 'orders' }" @click="tab = 'orders'">Orders</button>
        <button class="tab" :class="{ active: tab === 'pricing' }" @click="tab = 'pricing'">Pricing</button>
        <button class="tab" :class="{ active: tab === 'alerts' }" @click="tab = 'alerts'">Alerts</button>
        <button class="tab" :class="{ active: tab === 'access' }" @click="tab = 'access'">Access list</button>
      </div>
    </article>

    <template v-if="tab === 'report'">
      <article class="surface wide">
        <div class="section-head compact">
          <div>
            <h2>AI operations report</h2>
            <p>Generate a summary from live operational data without dropping back to static text.</p>
          </div>
          <div style="display:flex; align-items:center; gap:8px;"><i class="fa-solid fa-circle-user" style="color:var(--brand); font-size:24px;"></i>Admin</div>
        </div>

        <div class="query-box">
          <p><i class="fa-solid fa-robot" style="color:var(--brand); margin-right:8px;"></i>Query backend-backed business data and render a report card plus chart.</p>
          <div class="fake-input">
            <input v-model="query" type="text" placeholder="Describe the report you want to generate" />
            <button class="primary-button" :disabled="state.busy.report" @click="handleGenerateReport">
              <i class="fa-solid" :class="state.busy.report ? 'fa-spinner fa-spin' : 'fa-bolt'"></i>
              {{ state.busy.report ? "Generating..." : "Generate report" }}
            </button>
          </div>
        </div>

        <div v-if="state.adminReport" class="report-output">
          <strong><i class="fa-solid fa-chart-line"></i> Query analysis</strong>
          <p style="margin-top:10px; margin-bottom: 6px; color: #e2e8f0;"><b>Query:</b> {{ state.adminReport.query }}</p>
          <p style="margin-top:10px; margin-bottom: 20px; color: #e2e8f0;">{{ state.adminReport.summary }}</p>
          <div ref="chartRef" style="width: 100%; height: 350px;"></div>
        </div>
      </article>
    </template>

    <template v-else>
      <article class="surface wide">
        <div class="section-head">
          <div>
            <h2 style="text-transform: capitalize;">{{ tab }}</h2>
            <p>All records in this section are sourced from the store contract that now reflects backend payloads first.</p>
          </div>
          <button class="primary-button small">Export</button>
        </div>
        <div class="table-wrap">
          <DataTable :headers="tableConfig.headers" :rows="tableConfig.rows" />
        </div>
      </article>
    </template>
  </section>
</template>
