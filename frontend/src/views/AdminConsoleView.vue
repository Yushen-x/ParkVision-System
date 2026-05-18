<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";
import DataTable from "../components/DataTable.vue";
import { generateAdminReport, state } from "../stores/parkingStore";
import { zhMoney, zhText } from "../utils/localize";

const tab = ref("report");
const query = ref("对比最近 7 天 VIP 取车对收入的影响");
const chartRef = ref(null);
let chart;

const tabNames = {
  orders: "订单台账",
  pricing: "计费规则",
  alerts: "告警记录",
  access: "准入名单",
};

const tableConfig = computed(() => {
  const configs = {
    orders: {
      headers: ["订单号", "车牌", "事件", "车位", "状态", "金额"],
      rows: state.adminOrders.map((order) => [
        order.orderNo,
        order.plateNo,
        zhText(order.event),
        order.slotId,
        zhText(order.status),
        zhMoney(order.amount),
      ]),
    },
    pricing: {
      headers: ["规则", "时段", "计费方式", "附加策略", "状态"],
      rows: state.pricingRules.map((rule) => [
        zhText(rule.name),
        zhText(rule.timeRange),
        zhText(rule.method),
        zhText(rule.extraPolicy),
        zhText(rule.status),
      ]),
    },
    alerts: {
      headers: ["告警号", "类型", "内容", "状态", "级别"],
      rows: state.alerts.map((alert) => [
        alert.alertNo,
        zhText(alert.type),
        zhText(alert.content),
        zhText(alert.status),
        zhText(alert.level),
      ]),
    },
    access: {
      headers: ["车牌", "名单类型", "用户类型", "有效期", "备注"],
      rows: state.accessList.map((item) => [
        item.plateNo,
        zhText(item.listType),
        zhText(item.userType),
        zhText(item.validUntil),
        zhText(item.remark),
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
      data: ["上周", "本周"],
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
      axisLabel: { formatter: "￥{value}", color: "#94a3b8" },
      splitLine: { lineStyle: { color: "rgba(255,255,255,0.05)" } },
    },
    series: [
      {
        name: "上周",
        type: "line",
        data: state.adminReport.previousWeekRevenue,
        smooth: true,
        itemStyle: { color: "#94a3b8" },
      },
      {
        name: "本周",
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
        <button class="tab" :class="{ active: tab === 'report' }" @click="tab = 'report'"><i class="fa-solid fa-wand-magic-sparkles" style="margin-right:6px;"></i>AI 报表</button>
        <button class="tab" :class="{ active: tab === 'orders' }" @click="tab = 'orders'">订单台账</button>
        <button class="tab" :class="{ active: tab === 'pricing' }" @click="tab = 'pricing'">计费规则</button>
        <button class="tab" :class="{ active: tab === 'alerts' }" @click="tab = 'alerts'">告警记录</button>
        <button class="tab" :class="{ active: tab === 'access' }" @click="tab = 'access'">准入名单</button>
      </div>
    </article>

    <template v-if="tab === 'report'">
      <article class="surface wide">
        <div class="section-head compact">
          <div>
            <h2>AI 运营报表</h2>
            <p>基于实时业务数据生成摘要，不再回退到静态展示文本。</p>
          </div>
          <div style="display:flex; align-items:center; gap:8px;"><i class="fa-solid fa-circle-user" style="color:var(--brand); font-size:24px;"></i>管理员</div>
        </div>

        <div class="query-box">
          <p><i class="fa-solid fa-robot" style="color:var(--brand); margin-right:8px;"></i>查询后端业务数据，并生成报表卡片和趋势图。</p>
          <div class="fake-input">
            <input v-model="query" type="text" placeholder="请输入需要生成的报表主题" />
            <button class="primary-button" :disabled="state.busy.report" @click="handleGenerateReport">
              <i class="fa-solid" :class="state.busy.report ? 'fa-spinner fa-spin' : 'fa-bolt'"></i>
              {{ state.busy.report ? "生成中..." : "生成报表" }}
            </button>
          </div>
        </div>

        <div v-if="state.adminReport" class="report-output">
          <strong><i class="fa-solid fa-chart-line"></i> 查询分析</strong>
          <p style="margin-top:10px; margin-bottom: 6px; color: #e2e8f0;"><b>查询：</b> {{ zhText(state.adminReport.query) }}</p>
          <p style="margin-top:10px; margin-bottom: 20px; color: #e2e8f0;">{{ zhText(state.adminReport.summary) }}</p>
          <div ref="chartRef" style="width: 100%; height: 350px;"></div>
        </div>
      </article>
    </template>

    <template v-else>
      <article class="surface wide">
        <div class="section-head">
          <div>
            <h2 style="text-transform: none;">{{ tabNames[tab] }}</h2>
            <p>本模块记录优先来自后端接口和数据库，再同步到前端状态。</p>
          </div>
          <button class="primary-button small">导出</button>
        </div>
        <div class="table-wrap">
          <DataTable :headers="tableConfig.headers" :rows="tableConfig.rows" />
        </div>
      </article>
    </template>
  </section>
</template>
