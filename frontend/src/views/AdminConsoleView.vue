<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import * as echarts from "echarts/core";
import { LineChart } from "echarts/charts";
import { GridComponent, LegendComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import DataTable from "../components/DataTable.vue";
import { generateAdminReport, loadAdminAlertDetail, loadAdminCustomerDetail, loadAdminOrderDetail, loadBillingComponents, resetAdminFilters, state, updateAdminFilters } from "../stores/parkingStore";
import { zhMoney, zhText } from "../utils/localize";

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

const tab = ref("report");
const query = ref("对比最近 7 天 VIP 取车对收入的影响");
const searchTerm = ref("");
const selectedBillingOrder = ref(state.selectedBillingOrderNo || "");
const chartRef = ref(null);
const localFilters = reactive({
  orderStatus: "",
  orderDateFrom: "",
  orderDateTo: "",
  alertLevel: "",
  alertStatus: "",
  profileEnergyType: "",
  profileMemberLevel: "",
  paymentStatus: "",
  paymentMethod: "",
  paymentDateFrom: "",
  paymentDateTo: "",
});
let chart;

const keywordFilterKeys = {
  orders: "orderKeyword",
  alerts: "alertKeyword",
  profiles: "profileKeyword",
  payments: "paymentKeyword",
};

const tabNames = {
  report: "AI 报表",
  orders: "订单台账",
  pricing: "计费规则",
  alerts: "告警记录",
  access: "准入名单",
  profiles: "客户车辆",
  payments: "支付流水",
  billing: "计费明细",
};

const overviewCards = computed(() => [
  { label: "进行中订单", value: state.adminOverview.activeOrders, hint: "当前未结算流程" },
  { label: "已结算订单", value: state.adminOverview.settledOrders, hint: "已完成离场闭环" },
  { label: "客户 / 车辆", value: `${state.adminOverview.customerCount} / ${state.adminOverview.vehicleCount}`, hint: "账户与车辆档案" },
  { label: "支付流水", value: state.adminOverview.paymentCount, hint: "支付确认记录" },
  { label: "实时告警", value: state.adminOverview.liveAlerts, hint: "设备与订单异常" },
  { label: "VIP 任务", value: state.adminOverview.vipTasks, hint: "优先调度队列" },
  { label: "累计实收", value: zhMoney(state.adminOverview.collectedRevenue), hint: "按支付流水统计" },
]);

const billingOrderOptions = computed(() =>
  state.adminOrders.map((order) => ({
    label: `${order.orderNo} / ${order.plateNo}`,
    value: order.orderNo,
  })),
);

const detailEnabledViews = ["orders", "payments", "billing"];

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
    profiles: {
      headers: ["车主", "联系方式", "会员等级", "状态", "车牌", "能源", "会员类型", "准入"],
      rows: state.customerVehicles.map((row) => [
        `${row.ownerName} (${row.ownerId})`,
        row.phoneMasked,
        zhText(row.memberLevel),
        zhText(row.accountStatus),
        row.plateNo,
        zhText(row.energyType),
        zhText(row.membershipType),
        zhText(row.accessType),
      ]),
    },
    payments: {
      headers: ["流水号", "订单号", "车牌", "金额", "方式", "状态", "支付时间"],
      rows: state.payments.map((payment) => [
        payment.paymentNo,
        payment.orderNo,
        payment.plateNo,
        zhMoney(payment.amount),
        zhText(payment.method),
        zhText(payment.status),
        zhText(payment.paidAt),
      ]),
    },
    billing: {
      headers: ["明细号", "订单号", "费用类型", "说明", "金额", "生成时间"],
      rows: state.billingComponents.map((component) => [
        component.componentNo,
        component.orderNo,
        zhText(component.componentType),
        zhText(component.description),
        zhMoney(component.amount),
        zhText(component.createdAt),
      ]),
    },
  };

  return configs[tab.value] || configs.orders;
});

const filteredRows = computed(() => {
  if (hasBackendFilters.value) return tableConfig.value.rows;
  const keyword = searchTerm.value.trim().toLowerCase();
  if (!keyword) return tableConfig.value.rows;
  return tableConfig.value.rows.filter((row) =>
    row.some((cell) => String(cell).toLowerCase().includes(keyword)),
  );
});

const hasBackendFilters = computed(() => Object.hasOwn(keywordFilterKeys, tab.value));
const searchPlaceholder = computed(() => (hasBackendFilters.value ? "输入关键词后应用筛选" : "搜索当前表格"));
const isTableClickable = computed(() => ["orders", "payments", "profiles", "alerts"].includes(tab.value));
const selectedTableRowKey = computed(() => {
  if (tab.value === "orders" || tab.value === "payments") {
    return state.selectedAdminOrderNo;
  }
  if (tab.value === "alerts") {
    return state.selectedAlertNo;
  }
  return null;
});
const tableRowKeys = computed(() => {
  if (tab.value === "orders") {
    return state.adminOrders.map((order) => order.orderNo);
  }
  if (tab.value === "payments") {
    return state.payments.map((payment) => payment.orderNo);
  }
  if (tab.value === "profiles") {
    return state.customerVehicles.map((row) => `${row.ownerId}:${row.plateNo}`);
  }
  if (tab.value === "alerts") {
    return state.alerts.map((alert) => alert.alertNo);
  }
  return [];
});
const detailSummaryCards = computed(() => {
  const detail = state.adminOrderDetail;
  if (!detail) return [];
  return [
    { label: "订单号", value: detail.orderNo },
    { label: "车牌", value: detail.plateNo },
    { label: "车位", value: detail.slotId },
    { label: "状态", value: zhText(detail.status) },
    { label: "订单金额", value: zhMoney(detail.amount) },
    { label: "优惠金额", value: zhMoney(detail.discountAmount) },
    { label: "支付状态", value: zhText(detail.paymentStatus) },
    { label: "停放时长", value: detail.durationMinutes ? `${detail.durationMinutes} 分钟` : "暂无" },
  ];
});
const customerSummaryCards = computed(() => {
  const detail = state.adminCustomerDetail;
  if (!detail) return [];
  return [
    { label: "车主", value: `${detail.ownerName} / ${detail.ownerId}` },
    { label: "会员等级", value: zhText(detail.memberLevel) },
    { label: "账户状态", value: zhText(detail.accountStatus) },
    { label: "车辆总数", value: `${detail.totalVehicles}` },
    { label: "新能源车辆", value: `${detail.evVehicles}` },
    { label: "进行中订单", value: `${detail.activeOrders}` },
    { label: "已完成订单", value: `${detail.settledOrders}` },
    { label: "累计支付", value: zhMoney(detail.totalPaid) },
  ];
});
const alertSummaryCards = computed(() => {
  const detail = state.adminAlertDetail;
  if (!detail) return [];
  return [
    { label: "告警号", value: detail.alertNo },
    { label: "告警类型", value: zhText(detail.type) },
    { label: "处理状态", value: zhText(detail.status) },
    { label: "告警级别", value: zhText(detail.level) },
    { label: "关联事件", value: `${detail.relatedEventCount}` },
    { label: "已确认事件", value: `${detail.acknowledgedEventCount}` },
    { label: "升级状态", value: zhText(detail.escalationState) },
  ];
});

function syncSearchTermFromFilters(view = tab.value) {
  const keywordKey = keywordFilterKeys[view];
  searchTerm.value = keywordKey ? state.adminFilters[keywordKey] : "";
}

async function handleGenerateReport() {
  await generateAdminReport(query.value);
  await nextTick();
  renderChart();
}

async function handleBillingOrderChange() {
  if (!selectedBillingOrder.value) return;
  await loadBillingComponents(selectedBillingOrder.value);
  await loadAdminOrderDetail(selectedBillingOrder.value);
}

async function handleAdminRowClick({ index }) {
  if (tab.value === "orders") {
    const orderNo = state.adminOrders[index]?.orderNo;
    if (orderNo) {
      await loadAdminOrderDetail(orderNo);
    }
    return;
  }

  if (tab.value === "payments") {
    const orderNo = state.payments[index]?.orderNo;
    if (orderNo) {
      await loadAdminOrderDetail(orderNo);
    }
    return;
  }

  if (tab.value === "profiles") {
    const ownerId = state.customerVehicles[index]?.ownerId;
    if (ownerId) {
      await loadAdminCustomerDetail(ownerId);
    }
    return;
  }

  if (tab.value === "alerts") {
    const alertNo = state.alerts[index]?.alertNo;
    if (alertNo) {
      await loadAdminAlertDetail(alertNo);
    }
  }
}

async function applyCurrentFilters() {
  const keyword = searchTerm.value.trim();
  if (tab.value === "orders") {
    await updateAdminFilters({
      orderStatus: localFilters.orderStatus,
      orderKeyword: keyword,
      orderDateFrom: localFilters.orderDateFrom,
      orderDateTo: localFilters.orderDateTo,
    });
  } else if (tab.value === "alerts") {
    await updateAdminFilters({
      alertLevel: localFilters.alertLevel,
      alertStatus: localFilters.alertStatus,
      alertKeyword: keyword,
    });
  } else if (tab.value === "profiles") {
    await updateAdminFilters({
      profileEnergyType: localFilters.profileEnergyType,
      profileMemberLevel: localFilters.profileMemberLevel,
      profileKeyword: keyword,
    });
  } else if (tab.value === "payments") {
    await updateAdminFilters({
      paymentStatus: localFilters.paymentStatus,
      paymentMethod: localFilters.paymentMethod,
      paymentKeyword: keyword,
      paymentDateFrom: localFilters.paymentDateFrom,
      paymentDateTo: localFilters.paymentDateTo,
    });
  }
}

async function clearCurrentFilters() {
  if (tab.value === "orders") {
    localFilters.orderStatus = "";
    localFilters.orderDateFrom = "";
    localFilters.orderDateTo = "";
  } else if (tab.value === "alerts") {
    localFilters.alertLevel = "";
    localFilters.alertStatus = "";
  } else if (tab.value === "profiles") {
    localFilters.profileEnergyType = "";
    localFilters.profileMemberLevel = "";
  } else if (tab.value === "payments") {
    localFilters.paymentStatus = "";
    localFilters.paymentMethod = "";
    localFilters.paymentDateFrom = "";
    localFilters.paymentDateTo = "";
  }
  if (hasBackendFilters.value) {
    searchTerm.value = "";
  }
  await resetAdminFilters(tab.value);
}

function handleExportCsv() {
  const exportLines = [
    ["导出视图", tabNames[tab.value] || tab.value],
    ["导出时间", formatExportTimestamp(new Date())],
    ["结果数量", `${filteredRows.value.length}`],
    ...buildExportFilterLines(),
    ...buildExportMetricLines(),
    [],
    tableConfig.value.headers,
    ...filteredRows.value,
  ];
  const csv = exportLines.map((row) => toCsvLine(row)).join("\n");
  const blob = new Blob([`\uFEFF${csv}`], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `parkvision-${tab.value}-${formatExportFileStamp(new Date())}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function buildExportFilterLines() {
  const lines = [];
  if (tab.value === "orders") {
    appendFilterLine(lines, "订单状态", localFilters.orderStatus && zhText(localFilters.orderStatus));
    appendFilterLine(lines, "开始日期", localFilters.orderDateFrom);
    appendFilterLine(lines, "结束日期", localFilters.orderDateTo);
    appendFilterLine(lines, "关键词", state.adminFilters.orderKeyword || searchTerm.value.trim());
  } else if (tab.value === "alerts") {
    appendFilterLine(lines, "告警级别", localFilters.alertLevel);
    appendFilterLine(lines, "处理状态", localFilters.alertStatus);
    appendFilterLine(lines, "关键词", state.adminFilters.alertKeyword || searchTerm.value.trim());
  } else if (tab.value === "profiles") {
    appendFilterLine(lines, "能源类型", localFilters.profileEnergyType && zhText(localFilters.profileEnergyType));
    appendFilterLine(lines, "会员等级", localFilters.profileMemberLevel && zhText(localFilters.profileMemberLevel));
    appendFilterLine(lines, "关键词", state.adminFilters.profileKeyword || searchTerm.value.trim());
  } else if (tab.value === "payments") {
    appendFilterLine(lines, "支付状态", localFilters.paymentStatus && zhText(localFilters.paymentStatus));
    appendFilterLine(lines, "支付方式", localFilters.paymentMethod && zhText(localFilters.paymentMethod));
    appendFilterLine(lines, "开始日期", localFilters.paymentDateFrom);
    appendFilterLine(lines, "结束日期", localFilters.paymentDateTo);
    appendFilterLine(lines, "关键词", state.adminFilters.paymentKeyword || searchTerm.value.trim());
  } else if (searchTerm.value.trim()) {
    appendFilterLine(lines, "表内搜索", searchTerm.value.trim());
  }
  return lines;
}

function buildExportMetricLines() {
  if (tab.value === "orders") {
    const totalAmount = state.adminOrders.reduce((sum, order) => sum + parseMoney(order.amount), 0);
    const closedCount = state.adminOrders.filter((order) => String(order.status).includes("Closed")).length;
    return [
      ["摘要", "订单金额合计", zhMoney(totalAmount)],
      ["摘要", "已关闭订单", `${closedCount}`],
    ];
  }

  if (tab.value === "payments") {
    const totalAmount = state.payments.reduce((sum, payment) => sum + parseMoney(payment.amount), 0);
    const successCount = state.payments.filter((payment) => payment.status === "SUCCESS").length;
    return [
      ["摘要", "支付金额合计", zhMoney(totalAmount)],
      ["摘要", "成功支付笔数", `${successCount}`],
    ];
  }

  if (tab.value === "profiles") {
    const uniqueOwners = new Set(state.customerVehicles.map((row) => row.ownerId)).size;
    const evVehicles = state.customerVehicles.filter((row) => row.energyType === "EV").length;
    return [
      ["摘要", "客户数量", `${uniqueOwners}`],
      ["摘要", "新能源车辆", `${evVehicles}`],
    ];
  }

  if (tab.value === "alerts") {
    const highLevel = state.alerts.filter((alert) => alert.level === "高").length;
    const pendingCount = state.alerts.filter((alert) => alert.status === "待复核" || alert.status === "处理中").length;
    return [
      ["摘要", "高等级告警", `${highLevel}`],
      ["摘要", "待处理告警", `${pendingCount}`],
    ];
  }

  if (tab.value === "billing") {
    const totalAmount = state.billingComponents.reduce((sum, component) => sum + parseMoney(component.amount), 0);
    return [["摘要", "计费项金额合计", zhMoney(totalAmount)]];
  }

  return [];
}

function appendFilterLine(lines, label, value) {
  if (!value) return;
  lines.push(["筛选条件", label, value]);
}

function toCsvLine(row) {
  return row.map((cell) => `"${String(cell ?? "").replace(/"/g, '""')}"`).join(",");
}

function parseMoney(value) {
  const normalized = Number(String(value ?? "").replace(/[^\d.-]/g, ""));
  return Number.isFinite(normalized) ? normalized : 0;
}

function formatExportTimestamp(value) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  const hour = String(value.getHours()).padStart(2, "0");
  const minute = String(value.getMinutes()).padStart(2, "0");
  const second = String(value.getSeconds()).padStart(2, "0");
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
}

function formatExportFileStamp(value) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  const hour = String(value.getHours()).padStart(2, "0");
  const minute = String(value.getMinutes()).padStart(2, "0");
  const second = String(value.getSeconds()).padStart(2, "0");
  return `${year}${month}${day}-${hour}${minute}${second}`;
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
      textStyle: { color: "#64748b" },
    },
    grid: { left: "3%", right: "4%", bottom: "3%", top: "15%", containLabel: true },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: state.adminReport.labels,
      axisLine: { lineStyle: { color: "rgba(0, 0, 0, 0.06)" } },
      axisLabel: { color: "#64748b" },
    },
    yAxis: {
      type: "value",
      axisLabel: { formatter: "¥{value}", color: "#64748b" },
      splitLine: { lineStyle: { color: "rgba(0, 0, 0, 0.05)" } },
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
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: "#4f46e5" },
      },
    ],
  });
}

function handleResize() {
  chart?.resize();
}

watch(
  () => state.adminReport,
  async () => {
    await nextTick();
    renderChart();
  },
  { deep: true },
);

watch(
  () => state.selectedBillingOrderNo,
  (value) => {
    if (value) selectedBillingOrder.value = value;
  },
);

watch(
  () => state.adminFilters,
  (filters) => {
    localFilters.orderStatus = filters.orderStatus;
    localFilters.orderDateFrom = filters.orderDateFrom;
    localFilters.orderDateTo = filters.orderDateTo;
    localFilters.alertLevel = filters.alertLevel;
    localFilters.alertStatus = filters.alertStatus;
    localFilters.profileEnergyType = filters.profileEnergyType;
    localFilters.profileMemberLevel = filters.profileMemberLevel;
    localFilters.paymentStatus = filters.paymentStatus;
    localFilters.paymentMethod = filters.paymentMethod;
    localFilters.paymentDateFrom = filters.paymentDateFrom;
    localFilters.paymentDateTo = filters.paymentDateTo;
    if (hasBackendFilters.value) {
      syncSearchTermFromFilters();
    }
  },
  { immediate: true, deep: true },
);

watch(
  billingOrderOptions,
  async (options) => {
    if (!options.length) return;
    if (!selectedBillingOrder.value || !options.some((option) => option.value === selectedBillingOrder.value)) {
      selectedBillingOrder.value = options[0].value;
      if (tab.value === "billing") {
        await handleBillingOrderChange();
      }
    }
  },
  { immediate: true },
);

watch(
  () => state.customerVehicles,
  async (rows) => {
    if (!rows.length) {
      state.adminCustomerDetail = null;
      return;
    }
    const selectedExists = state.selectedCustomerOwnerId && rows.some((row) => row.ownerId === state.selectedCustomerOwnerId);
    if ((!selectedExists || !state.adminCustomerDetail) && tab.value === "profiles") {
      await loadAdminCustomerDetail(rows[0].ownerId);
    }
  },
  { immediate: true, deep: true },
);

watch(
  () => state.alerts,
  async (rows) => {
    if (!rows.length) {
      state.adminAlertDetail = null;
      return;
    }
    const selectedExists = state.selectedAlertNo && rows.some((row) => row.alertNo === state.selectedAlertNo);
    if ((!selectedExists || !state.adminAlertDetail) && tab.value === "alerts") {
      await loadAdminAlertDetail(rows[0].alertNo);
    }
  },
  { immediate: true, deep: true },
);

watch(tab, async (value) => {
  syncSearchTermFromFilters(value);
  if (
    value === "billing" &&
    selectedBillingOrder.value &&
    (!state.billingComponents.length || state.selectedAdminOrderNo !== selectedBillingOrder.value)
  ) {
    await handleBillingOrderChange();
  }
  if (value === "profiles" && state.customerVehicles.length) {
    const ownerId =
      (state.selectedCustomerOwnerId && state.customerVehicles.some((row) => row.ownerId === state.selectedCustomerOwnerId)
        ? state.selectedCustomerOwnerId
        : state.customerVehicles[0].ownerId);
    await loadAdminCustomerDetail(ownerId);
  }
  if (value === "alerts" && state.alerts.length) {
    const alertNo =
      (state.selectedAlertNo && state.alerts.some((row) => row.alertNo === state.selectedAlertNo)
        ? state.selectedAlertNo
        : state.alerts[0].alertNo);
    await loadAdminAlertDetail(alertNo);
  }
});

onMounted(async () => {
  await nextTick();
  renderChart();
  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  chart?.dispose();
});
</script>

<template>
  <section class="admin-grid">
    <article class="surface wide" style="padding: 0; background: transparent; border: none; box-shadow: none; backdrop-filter: none;">
      <div class="admin-tabs">
        <button
          v-for="(label, key) in tabNames"
          :key="key"
          class="tab"
          :class="{ active: tab === key }"
          @click="tab = key"
        >
          {{ label }}
        </button>
      </div>
    </article>

    <article class="surface wide">
      <div class="section-head compact">
        <div>
          <h2>管理总览</h2>
          <p>汇总数据来自管理接口，覆盖订单、账户、支付和告警。</p>
        </div>
      </div>
      <div class="metric-list admin-metric-grid">
        <div v-for="card in overviewCards" :key="card.label">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.hint }}</small>
        </div>
      </div>
    </article>

    <template v-if="tab === 'report'">
      <article class="surface wide">
        <div class="section-head compact">
          <div>
            <h2>AI 运营报表</h2>
            <p>基于实时业务数据生成摘要、收入趋势和调度观察。</p>
          </div>
          <div style="display:flex; align-items:center; gap:8px;">
            <i class="fa-solid fa-circle-user" style="color:var(--brand); font-size:24px;"></i>管理员
          </div>
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
          <p style="margin-top:10px; margin-bottom: 6px; color: var(--text-muted);"><b>查询:</b> {{ zhText(state.adminReport.query) }}</p>
          <p style="margin-top:10px; margin-bottom: 20px; color: var(--text-main); line-height: 1.6;">{{ zhText(state.adminReport.summary) }}</p>
          <div ref="chartRef" style="width: 100%; height: 350px;"></div>
        </div>
      </article>
    </template>

    <template v-else>
      <article class="surface wide">
        <div class="section-head">
          <div>
            <h2 style="text-transform: none;">{{ tabNames[tab] }}</h2>
            <p>数据来自后端接口与持久化数据库，支持表内搜索、定向筛选和导出。</p>
          </div>
          <button class="primary-button small" @click="handleExportCsv">导出 CSV</button>
        </div>

        <div v-if="hasBackendFilters" class="admin-toolbar">
          <template v-if="tab === 'orders'">
            <select v-model="localFilters.orderStatus" class="panel-select">
              <option value="">全部订单状态</option>
              <option value="PARKED">停车中</option>
              <option value="RETRIEVING">调度中</option>
              <option value="TOUCHING">交接区等待</option>
              <option value="PAYING">等待支付</option>
              <option value="FINISHED">已关闭</option>
              <option value="ABNORMAL">需复核</option>
            </select>
            <input v-model="localFilters.orderDateFrom" class="panel-select" type="date" />
            <input v-model="localFilters.orderDateTo" class="panel-select" type="date" />
          </template>

          <template v-if="tab === 'alerts'">
            <select v-model="localFilters.alertLevel" class="panel-select">
              <option value="">全部告警级别</option>
              <option value="高">高</option>
              <option value="中">中</option>
              <option value="低">低</option>
            </select>
            <select v-model="localFilters.alertStatus" class="panel-select">
              <option value="">全部处理状态</option>
              <option value="急停中">急停中</option>
              <option value="处理中">处理中</option>
              <option value="待复核">待复核</option>
              <option value="已开启">已开启</option>
              <option value="已恢复">已恢复</option>
              <option value="监控中">监控中</option>
              <option value="已升级">已升级</option>
            </select>
          </template>

          <template v-if="tab === 'profiles'">
            <select v-model="localFilters.profileEnergyType" class="panel-select">
              <option value="">全部能源类型</option>
              <option value="EV">新能源</option>
              <option value="FUEL">燃油</option>
            </select>
            <select v-model="localFilters.profileMemberLevel" class="panel-select">
              <option value="">全部会员等级</option>
              <option value="VIP">VIP</option>
              <option value="MONTHLY">月卡</option>
              <option value="STANDARD">标准</option>
            </select>
          </template>

          <template v-if="tab === 'payments'">
            <select v-model="localFilters.paymentStatus" class="panel-select">
              <option value="">全部支付状态</option>
              <option value="SUCCESS">成功</option>
            </select>
            <select v-model="localFilters.paymentMethod" class="panel-select">
              <option value="">全部支付方式</option>
              <option value="AUTO_SETTLEMENT">自动结算</option>
            </select>
            <input v-model="localFilters.paymentDateFrom" class="panel-select" type="date" />
            <input v-model="localFilters.paymentDateTo" class="panel-select" type="date" />
          </template>

          <button class="ghost-button small" @click="applyCurrentFilters">应用筛选</button>
          <button class="ghost-button small" @click="clearCurrentFilters">清空筛选</button>
        </div>

        <div class="admin-toolbar">
          <input
            v-model="searchTerm"
            class="filter-input"
            type="text"
            :placeholder="searchPlaceholder"
          />
          <div v-if="tab === 'billing'" class="billing-toolbar">
            <select v-model="selectedBillingOrder" class="panel-select" @change="handleBillingOrderChange">
              <option v-for="option in billingOrderOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <button class="ghost-button small" :disabled="state.busy.billing" @click="handleBillingOrderChange">
              {{ state.busy.billing ? "加载中..." : "刷新明细" }}
            </button>
          </div>
          <span class="admin-count">结果 {{ filteredRows.length }} 条</span>
        </div>

        <div class="table-wrap">
          <DataTable
            :headers="tableConfig.headers"
            :rows="filteredRows"
            :row-keys="tableRowKeys"
            :clickable="isTableClickable"
            :selected-row-key="selectedTableRowKey"
            @row-click="handleAdminRowClick"
          />
        </div>

        <section v-if="detailEnabledViews.includes(tab) && state.adminOrderDetail" class="detail-panel">
          <div class="section-head compact">
            <div>
              <h2 style="text-transform: none;">订单运营详情</h2>
              <p>把订单、车主、车辆、支付和计费明细放到同一个视图里，方便追踪完整业务闭环。</p>
            </div>
            <span class="admin-count">{{ state.busy.detail ? "加载中..." : `当前订单 ${state.adminOrderDetail.orderNo}` }}</span>
          </div>

          <div class="detail-grid">
            <div v-for="card in detailSummaryCards" :key="card.label">
              <span>{{ card.label }}</span>
              <strong>{{ card.value }}</strong>
            </div>
          </div>

          <div class="detail-layout">
            <div class="detail-section">
              <span>时间线</span>
              <div class="detail-list">
                <div class="detail-item">
                  <b>入场时间</b>
                  <small>{{ zhText(state.adminOrderDetail.entryTime) }}</small>
                </div>
                <div class="detail-item">
                  <b>离场时间</b>
                  <small>{{ zhText(state.adminOrderDetail.exitTime) }}</small>
                </div>
                <div class="detail-item">
                  <b>支付时间</b>
                  <small>{{ zhText(state.adminOrderDetail.paidAt) }}</small>
                </div>
                <div class="detail-item">
                  <b>业务事件</b>
                  <small>{{ zhText(state.adminOrderDetail.event) }}</small>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <span>客户账户</span>
              <template v-if="state.adminOrderDetail.customer">
                <div class="detail-list">
                  <div class="detail-item">
                    <b>车主</b>
                    <small>{{ state.adminOrderDetail.customer.ownerName }} / {{ state.adminOrderDetail.customer.ownerId }}</small>
                  </div>
                  <div class="detail-item">
                    <b>联系方式</b>
                    <small>{{ zhText(state.adminOrderDetail.customer.phoneMasked) }}</small>
                  </div>
                  <div class="detail-item">
                    <b>会员等级</b>
                    <small>{{ zhText(state.adminOrderDetail.customer.memberLevel) }}</small>
                  </div>
                  <div class="detail-item">
                    <b>账户状态</b>
                    <small>{{ zhText(state.adminOrderDetail.customer.accountStatus) }}</small>
                  </div>
                  <div class="detail-item">
                    <b>账户余额</b>
                    <small>{{ state.adminOrderDetail.customer.balance == null ? "暂无" : zhMoney(state.adminOrderDetail.customer.balance) }}</small>
                  </div>
                </div>
              </template>
              <p v-else class="detail-empty">当前订单还没有匹配到车主账户。</p>
            </div>

            <div class="detail-section">
              <span>车辆档案</span>
              <template v-if="state.adminOrderDetail.vehicle">
                <div class="detail-list">
                  <div class="detail-item">
                    <b>车辆类型</b>
                    <small>{{ zhText(state.adminOrderDetail.vehicle.vehicleType) }}</small>
                  </div>
                  <div class="detail-item">
                    <b>能源类型</b>
                    <small>{{ zhText(state.adminOrderDetail.vehicle.energyType) }}</small>
                  </div>
                  <div class="detail-item">
                    <b>会员权益</b>
                    <small>{{ zhText(state.adminOrderDetail.vehicle.membershipType) }}</small>
                  </div>
                  <div class="detail-item">
                    <b>默认准入</b>
                    <small>{{ zhText(state.adminOrderDetail.vehicle.defaultAuthStatus) }}</small>
                  </div>
                </div>
              </template>
              <p v-else class="detail-empty">当前订单还没有关联车辆档案。</p>
            </div>

            <div class="detail-section">
              <span>支付与计费</span>
              <template v-if="state.adminOrderDetail.payment">
                <div class="detail-list">
                  <div class="detail-item">
                    <b>支付流水</b>
                    <small>{{ state.adminOrderDetail.payment.paymentNo }}</small>
                  </div>
                  <div class="detail-item">
                    <b>支付方式</b>
                    <small>{{ zhText(state.adminOrderDetail.payment.method) }}</small>
                  </div>
                  <div class="detail-item">
                    <b>支付结果</b>
                    <small>{{ zhText(state.adminOrderDetail.payment.status) }}</small>
                  </div>
                  <div class="detail-item">
                    <b>支付金额</b>
                    <small>{{ zhMoney(state.adminOrderDetail.payment.amount) }}</small>
                  </div>
                </div>
              </template>
              <p v-else class="detail-empty">当前订单还没有成功支付流水。</p>

              <div class="detail-list">
                <div
                  v-for="component in state.adminOrderDetail.billingComponents"
                  :key="component.componentNo"
                  class="detail-item"
                >
                  <b>{{ zhText(component.componentType) }}</b>
                  <small>{{ zhText(component.description) }} / {{ zhMoney(component.amount) }}</small>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section v-if="tab === 'profiles' && state.adminCustomerDetail" class="detail-panel">
          <div class="section-head compact">
            <div>
              <h2 style="text-transform: none;">客户账户画像</h2>
              <p>把账户、车辆、最近订单和支付汇总放在一起，方便从车主维度查看运营情况。</p>
            </div>
            <span class="admin-count">{{ state.busy.customerDetail ? "加载中..." : `当前客户 ${state.adminCustomerDetail.ownerId}` }}</span>
          </div>

          <div class="detail-grid">
            <div v-for="card in customerSummaryCards" :key="card.label">
              <span>{{ card.label }}</span>
              <strong>{{ card.value }}</strong>
            </div>
          </div>

          <div class="detail-layout">
            <div class="detail-section">
              <span>账户概况</span>
              <div class="detail-list">
                <div class="detail-item">
                  <b>联系方式</b>
                  <small>{{ zhText(state.adminCustomerDetail.phoneMasked) }}</small>
                </div>
                <div class="detail-item">
                  <b>账户余额</b>
                  <small>{{ state.adminCustomerDetail.balance == null ? "暂无" : zhMoney(state.adminCustomerDetail.balance) }}</small>
                </div>
                <div class="detail-item">
                  <b>开户时间</b>
                  <small>{{ zhText(state.adminCustomerDetail.createdAt) }}</small>
                </div>
                <div class="detail-item">
                  <b>最近支付</b>
                  <small>{{ zhText(state.adminCustomerDetail.lastPaymentAt) }}</small>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <span>车辆档案</span>
              <div class="detail-list">
                <div
                  v-for="vehicle in state.adminCustomerDetail.vehicles"
                  :key="vehicle.plateNo"
                  class="detail-item"
                >
                  <b>{{ vehicle.plateNo }}</b>
                  <small>{{ zhText(vehicle.energyType) }} / {{ zhText(vehicle.membershipType) }} / {{ zhText(vehicle.accessType) }}</small>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <span>最近订单</span>
              <div class="detail-list">
                <div
                  v-for="order in state.adminCustomerDetail.recentOrders"
                  :key="order.orderNo"
                  class="detail-item"
                >
                  <b>{{ order.orderNo }}</b>
                  <small>{{ order.plateNo }} / {{ zhText(order.status) }} / {{ zhMoney(order.amount) }}</small>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section v-if="tab === 'alerts' && state.adminAlertDetail" class="detail-panel">
          <div class="section-head compact">
            <div>
              <h2 style="text-transform: none;">告警处置详情</h2>
              <p>把告警主体、关联设备事件、订单线索和建议动作放在一起，方便做异常追踪和答辩演示。</p>
            </div>
            <span class="admin-count">{{ state.busy.alertDetail ? "加载中..." : `当前告警 ${state.adminAlertDetail.alertNo}` }}</span>
          </div>

          <div class="detail-grid">
            <div v-for="card in alertSummaryCards" :key="card.label">
              <span>{{ card.label }}</span>
              <strong>{{ card.value }}</strong>
            </div>
          </div>

          <div class="detail-layout">
            <div class="detail-section">
              <span>告警内容</span>
              <div class="detail-list">
                <div class="detail-item">
                  <b>告警描述</b>
                  <small>{{ zhText(state.adminAlertDetail.content) }}</small>
                </div>
                <div class="detail-item">
                  <b>建议动作</b>
                  <small>{{ zhText(state.adminAlertDetail.recommendedAction) }}</small>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <span>设备事件</span>
              <div class="detail-list">
                <div
                  v-for="event in state.adminAlertDetail.deviceEvents"
                  :key="event.eventId"
                  class="detail-item"
                >
                  <b>{{ event.deviceId }} / {{ zhText(event.eventCode) }}</b>
                  <small>{{ zhText(event.message) }} / {{ zhText(event.eventTime) }}</small>
                </div>
              </div>
              <p v-if="!state.adminAlertDetail.deviceEvents.length" class="detail-empty">当前告警没有匹配到设备事件。</p>
            </div>

            <div class="detail-section">
              <span>订单线索</span>
              <div class="detail-list">
                <div
                  v-for="order in state.adminAlertDetail.relatedOrders"
                  :key="order.orderNo"
                  class="detail-item"
                >
                  <b>{{ order.orderNo }}</b>
                  <small>{{ order.plateNo }} / {{ zhText(order.status) }} / {{ zhText(order.paymentStatus) }}</small>
                </div>
              </div>
              <p v-if="!state.adminAlertDetail.relatedOrders.length" class="detail-empty">当前告警没有明显关联订单。</p>
            </div>
          </div>
        </section>
      </article>
    </template>
  </section>
</template>
