<script setup>
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import {
  acknowledgeAlert,
  generateAdminReport,
  loadAdminAlertDetail,
  loadAdminCustomerDetail,
  loadAdminOrderDetail,
  loadBillingComponents,
  removeVehicle,
  resolveAlert,
  state,
  upsertVehicle,
} from "../stores/parkingStore";
import { zhMoney, zhText } from "../utils/localize";
import { aiChat, aiStatusLabel, isAiLive } from "../services/aiClient";

const pageRoute = useRoute();
const aiReport = ref(null);
const reportBusy = ref(false);
const reportStatus = computed(() => aiStatusLabel());
const orderKeyword = ref("");
const customerKeyword = ref("");
const alertKeyword = ref("");
const selectedOrderNo = ref(state.selectedAdminOrderNo || state.adminOrders[0]?.orderNo || "");
const selectedOwnerId = ref(state.selectedCustomerOwnerId || state.customerVehicles[0]?.ownerId || "");
const selectedAlertNo = ref(state.selectedAlertNo || state.alerts[0]?.alertNo || "");
const reportQuery = ref("今天停车收入、VIP 服务和异常处理有什么需要关注？");

const sections = [
  ["overview", "经营概览", "看今天发生了什么"],
  ["orders", "订单结算", "查订单、支付、计费"],
  ["customers", "客户与车辆", "查车主、会员、准入"],
  ["exceptions", "异常处理", "处理告警和复核"],
];
const sectionKeys = sections.map(([key]) => key);
const section = ref(sectionKeys.includes(pageRoute.query.section) ? pageRoute.query.section : "overview");

const overviewCards = computed(() => [
  { label: "进行中订单", value: state.adminOverview.activeOrders, hint: "需要持续跟踪的停车流程" },
  { label: "已结算订单", value: state.adminOverview.settledOrders, hint: "已完成放行和支付闭环" },
  { label: "累计实收", value: zhMoney(state.adminOverview.collectedRevenue), hint: "来自支付流水统计" },
  { label: "待处理异常", value: pendingAlerts.value, hint: "告警、设备或订单复核" },
  { label: "客户 / 车辆", value: `${state.adminOverview.customerCount} / ${state.adminOverview.vehicleCount}`, hint: "账户和车牌档案" },
  { label: "VIP 任务", value: state.adminOverview.vipTasks, hint: "优先取车服务量" },
]);

const pendingAlerts = computed(() =>
  state.alerts.filter((alert) => !["已恢复", "已关闭", "Resolved", "Closed"].includes(alert.status)).length,
);

const filteredOrders = computed(() => {
  const keyword = orderKeyword.value.trim().toLowerCase();
  return state.adminOrders.filter((order) =>
    !keyword ||
    order.orderNo.toLowerCase().includes(keyword) ||
    order.plateNo.toLowerCase().includes(keyword) ||
    order.slotId.toLowerCase().includes(keyword) ||
    zhText(order.status).toLowerCase().includes(keyword),
  );
});

const selectedOrder = computed(() => {
  const order = state.adminOrders.find((item) => item.orderNo === selectedOrderNo.value);
  return order || state.adminOrders[0] || null;
});

const orderDetail = computed(() => state.adminOrderDetail);

const selectedBillingRows = computed(() => {
  const detail = orderDetail.value;
  if (detail?.billingComponents?.length) return detail.billingComponents;
  return state.billingComponents.filter((item) => item.orderNo === selectedOrderNo.value);
});

const orderTimeline = computed(() => {
  const detail = orderDetail.value;
  if (!detail) return [];
  return [
    ["入场", detail.entryTime, detail.event],
    ["计费", detail.durationMinutes ? `${detail.durationMinutes} 分钟` : "暂无时长", detail.paymentStatus],
    ["支付", detail.paidAt, detail.paymentMethod || "未支付"],
    ["离场", detail.exitTime, detail.status],
  ];
});

const filteredCustomers = computed(() => {
  const keyword = customerKeyword.value.trim().toLowerCase();
  return state.customerVehicles.filter((row) =>
    !keyword ||
    row.ownerId.toLowerCase().includes(keyword) ||
    row.ownerName.toLowerCase().includes(keyword) ||
    row.plateNo.toLowerCase().includes(keyword) ||
    row.phoneMasked.toLowerCase().includes(keyword) ||
    zhText(row.memberLevel).toLowerCase().includes(keyword),
  );
});

const selectedCustomer = computed(() => {
  const customer = state.customerVehicles.find((item) => item.ownerId === selectedOwnerId.value);
  return customer || state.customerVehicles[0] || null;
});

const customerVehicles = computed(() =>
  state.customerVehicles.filter((vehicle) => vehicle.ownerId === selectedOwnerId.value),
);

const accessForCustomer = computed(() =>
  state.accessList.filter((access) => customerVehicles.value.some((vehicle) => vehicle.plateNo === access.plateNo)),
);

const customerOrders = computed(() =>
  state.adminOrders.filter((order) => customerVehicles.value.some((vehicle) => vehicle.plateNo === order.plateNo)),
);

const filteredAlerts = computed(() => {
  const keyword = alertKeyword.value.trim().toLowerCase();
  return state.alerts.filter((alert) =>
    !keyword ||
    alert.alertNo.toLowerCase().includes(keyword) ||
    zhText(alert.type).toLowerCase().includes(keyword) ||
    zhText(alert.content).toLowerCase().includes(keyword) ||
    zhText(alert.status).toLowerCase().includes(keyword),
  );
});

const selectedAlert = computed(() => {
  const alert = state.alerts.find((item) => item.alertNo === selectedAlertNo.value);
  return alert || state.alerts[0] || null;
});

const highPriorityAlerts = computed(() => state.alerts.filter((alert) => zhText(alert.level) === "高").length);

const settlementSummary = computed(() => {
  const orderCount = state.adminOrders.length;
  const totalOrderAmount = state.adminOrders.reduce((sum, order) => sum + parseMoney(order.amount), 0);
  const paidAmount = state.payments.reduce((sum, payment) => sum + parseMoney(payment.amount), 0);
  return [
    ["订单数", `${orderCount}`],
    ["订单金额", zhMoney(totalOrderAmount)],
    ["已收金额", zhMoney(paidAmount)],
    ["支付流水", `${state.payments.length}`],
  ];
});

async function selectOrder(orderNo) {
  selectedOrderNo.value = orderNo;
  await loadBillingComponents(orderNo);
  await loadAdminOrderDetail(orderNo);
}

async function selectCustomer(ownerId) {
  selectedOwnerId.value = ownerId;
  await loadAdminCustomerDetail(ownerId);
}

async function selectAlert(alertNo) {
  selectedAlertNo.value = alertNo;
  await loadAdminAlertDetail(alertNo);
}

const RESOLVED_STATES = ["已恢复", "已关闭", "Resolved", "Closed"];
const alertResolved = computed(() => RESOLVED_STATES.includes(selectedAlert.value?.status));
const alertAcknowledged = computed(() => selectedAlert.value?.status === "处理中" || alertResolved.value);

function ackSelectedAlert() {
  if (selectedAlert.value) acknowledgeAlert(selectedAlert.value.alertNo);
}

function resolveSelectedAlert() {
  if (selectedAlert.value) resolveAlert(selectedAlert.value.alertNo);
}

// --- 车辆 / 客户档案 CRUD ---
const showVehicleForm = ref(false);
const vehicleEditing = ref(false);
const vehicleForm = reactive({
  plateNo: "",
  ownerName: "",
  phoneMasked: "",
  energyType: "Fuel",
  memberLevel: "Standard",
  accessType: "Allow",
});

function openNewVehicle() {
  Object.assign(vehicleForm, {
    plateNo: "",
    ownerName: "",
    phoneMasked: "",
    energyType: "Fuel",
    memberLevel: "Standard",
    accessType: "Allow",
  });
  vehicleEditing.value = false;
  showVehicleForm.value = true;
}

function editVehicle(vehicle) {
  Object.assign(vehicleForm, {
    plateNo: vehicle.plateNo,
    ownerName: vehicle.ownerName,
    phoneMasked: vehicle.phoneMasked,
    energyType: vehicle.energyType || "Fuel",
    memberLevel: vehicle.memberLevel || "Standard",
    accessType: vehicle.accessType || "Allow",
  });
  vehicleEditing.value = true;
  showVehicleForm.value = true;
}

function saveVehicle() {
  if (!vehicleForm.plateNo.trim()) return;
  const row = upsertVehicle({ ...vehicleForm });
  showVehicleForm.value = false;
  if (row) selectCustomer(row.ownerId);
}

function deleteVehicle(plateNo) {
  removeVehicle(plateNo);
  if (selectedOwnerId.value) selectCustomer(selectedOwnerId.value);
}

function buildReportContext() {
  const o = state.adminOverview;
  const totalRevenue = state.payments.reduce((sum, payment) => sum + parseMoney(payment.amount), 0);
  return [
    `进行中订单 ${o.activeOrders} 单`,
    `已结算订单 ${o.settledOrders} 单`,
    `累计实收 ￥${o.collectedRevenue}`,
    `支付流水合计 ￥${totalRevenue.toFixed(2)}（${state.payments.length} 笔）`,
    `VIP 优先取车 ${o.vipTasks} 次`,
    `客户/车辆档案 ${o.customerCount}/${o.vehicleCount}`,
    `待处理异常 ${pendingAlerts.value} 条（其中高优先级 ${highPriorityAlerts.value} 条）`,
    `车位占用率 ${state.summary.occupancyRate}%`,
    `当前调度队列 ${state.queue.length} 项`,
  ].join("；");
}

async function generateOverviewReport() {
  // 接入真实大模型时走 LLM 智能问数；否则回退到内置报表生成（buildMockReport）。
  if (isAiLive()) {
    reportBusy.value = true;
    try {
      const { text, source, error } = await aiChat({
        system:
          "你是停车场运营分析助手。请只依据给定的经营数据，用简洁专业的中文回答值班主管的问题，" +
          "给出 3-5 条带要点符号的关注事项与建议，不要编造数据，不要超过 180 字。",
        messages: [{ role: "user", content: `问题：${reportQuery.value}\n\n今日经营数据：${buildReportContext()}` }],
        maxTokens: 600,
      });
      aiReport.value = { query: reportQuery.value, text, source, error };
    } finally {
      reportBusy.value = false;
    }
  } else {
    aiReport.value = null;
    await generateAdminReport(reportQuery.value);
  }
}

function exportCurrentView() {
  const rows = buildExportRows();
  const csv = rows.map((row) => row.map((cell) => `"${String(cell ?? "").replace(/"/g, '""')}"`).join(",")).join("\n");
  const blob = new Blob([`\uFEFF${csv}`], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `parkvision-${section.value}-${Date.now()}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function buildExportRows() {
  if (section.value === "orders") {
    return [
      ["订单号", "车牌", "事件", "车位", "状态", "金额"],
      ...filteredOrders.value.map((order) => [order.orderNo, order.plateNo, zhText(order.event), order.slotId, zhText(order.status), zhMoney(order.amount)]),
    ];
  }

  if (section.value === "customers") {
    return [
      ["车主", "联系方式", "会员等级", "状态", "车牌", "能源", "准入"],
      ...filteredCustomers.value.map((row) => [row.ownerName, row.phoneMasked, zhText(row.memberLevel), zhText(row.accountStatus), row.plateNo, zhText(row.energyType), zhText(row.accessType)]),
    ];
  }

  if (section.value === "exceptions") {
    return [
      ["告警号", "类型", "内容", "状态", "级别"],
      ...filteredAlerts.value.map((alert) => [alert.alertNo, zhText(alert.type), zhText(alert.content), zhText(alert.status), zhText(alert.level)]),
    ];
  }

  return [
    ["指标", "数值"],
    ...overviewCards.value.map((card) => [card.label, card.value]),
  ];
}

function parseMoney(value) {
  const numeric = Number(String(value ?? "").replace(/[^\d.-]/g, ""));
  return Number.isFinite(numeric) ? numeric : 0;
}

onMounted(async () => {
  if (selectedOrderNo.value) {
    await selectOrder(selectedOrderNo.value);
  }
  if (selectedOwnerId.value) {
    await selectCustomer(selectedOwnerId.value);
  }
  if (selectedAlertNo.value) {
    await selectAlert(selectedAlertNo.value);
  }
  await nextTick();
});
</script>

<template>
  <section class="ledger-workbench">
    <article class="surface ledger-nav">
      <div>
        <h2>管理台账</h2>
        <p>按员工任务重组：先看经营状态，再查订单、客户或异常，不再让人面对一排数据库表。</p>
      </div>
      <div class="ledger-tabs">
        <button
          v-for="[key, label, hint] in sections"
          :key="key"
          class="ledger-tab"
          :class="{ active: section === key }"
          @click="section = key"
        >
          <b>{{ label }}</b>
          <span>{{ hint }}</span>
        </button>
      </div>
    </article>

    <template v-if="section === 'overview'">
      <div class="metric-list admin-metric-grid ledger-overview">
        <div v-for="card in overviewCards" :key="card.label">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.hint }}</small>
        </div>
      </div>

      <section class="ledger-layout">
        <article class="surface">
          <div class="section-head compact">
            <div>
              <h2>今日经营摘要 · AI 智能问数</h2>
              <p>用自然语言提问，AI 基于实时经营数据生成值班关注点与处置建议。</p>
            </div>
            <div class="report-head-actions">
              <span class="status-pill" :class="reportStatus.includes('已接入') ? 'stable' : 'warning'">{{ reportStatus }}</span>
              <button class="primary-button small" :disabled="reportBusy || state.busy.report" @click="generateOverviewReport">
                <i class="fa-solid" :class="reportBusy || state.busy.report ? 'fa-spinner fa-spin' : 'fa-wand-magic-sparkles'"></i>
                生成摘要
              </button>
            </div>
          </div>
          <div class="query-line">
            <input v-model="reportQuery" placeholder="输入想要分析的经营问题，例如：今天有哪些异常需要优先处理？" @keyup.enter="generateOverviewReport" />
          </div>
          <div class="report-card ai-live">
            <template v-if="aiReport">
              <b>
                <i class="fa-solid fa-robot"></i>
                {{ aiReport.query }}
                <small class="report-source">AI 智能分析</small>
              </b>
              <p class="report-body">{{ aiReport.text }}</p>
            </template>
            <template v-else>
              <b>{{ zhText(state.adminReport?.query || "运营摘要") }}</b>
              <p>{{ zhText(state.adminReport?.summary || "点击生成摘要后，会基于订单、收入、VIP 和告警数据给出值班关注点。") }}</p>
            </template>
          </div>
        </article>

        <article class="surface">
          <div class="section-head compact">
            <div>
              <h2>需要优先处理</h2>
              <p>把员工最该关注的对象聚合出来，而不是让人自己找 Tab。</p>
            </div>
          </div>
          <div class="priority-list">
            <button class="priority-item" @click="section = 'exceptions'">
              <span>高优先级告警</span>
              <strong>{{ highPriorityAlerts }}</strong>
              <small>安全、设备或订单异常需要复核</small>
            </button>
            <button class="priority-item" @click="section = 'orders'">
              <span>未完成订单</span>
              <strong>{{ state.adminOverview.activeOrders }}</strong>
              <small>调度、支付或放行未完成</small>
            </button>
            <button class="priority-item" @click="section = 'customers'">
              <span>客户车辆档案</span>
              <strong>{{ state.customerVehicles.length }}</strong>
              <small>会员、准入和车辆信息</small>
            </button>
          </div>
        </article>
      </section>
    </template>

    <template v-if="section === 'orders'">
      <article class="surface ledger-toolbar">
        <div>
          <h2>订单结算</h2>
          <p>合并原来的订单台账、支付流水和计费明细。员工查一个订单就能看到完整结算链路。</p>
        </div>
        <div class="toolbar-actions">
          <div class="search-field">
            <i class="fa-solid fa-magnifying-glass"></i>
            <input v-model="orderKeyword" placeholder="搜索订单号、车牌、车位或状态" />
          </div>
          <button class="ghost-button small" @click="exportCurrentView">导出</button>
        </div>
      </article>

      <section class="ledger-layout detail">
        <aside class="surface ledger-list">
          <div class="section-head compact">
            <h2>订单</h2>
            <span class="admin-count">{{ filteredOrders.length }} 条</span>
          </div>
          <button
            v-for="order in filteredOrders"
            :key="order.orderNo"
            class="ledger-row"
            :class="{ active: selectedOrder?.orderNo === order.orderNo }"
            @click="selectOrder(order.orderNo)"
          >
            <b>{{ order.plateNo }}</b>
            <span>{{ order.orderNo }} / {{ order.slotId }}</span>
            <small>{{ zhText(order.status) }} / {{ zhMoney(order.amount) }}</small>
          </button>
        </aside>

        <article class="surface ledger-detail">
          <div class="detail-title">
            <div>
              <span>当前订单</span>
              <h2>{{ orderDetail?.plateNo || selectedOrder?.plateNo || "请选择订单" }}</h2>
              <p>{{ orderDetail?.orderNo || selectedOrder?.orderNo }} / 车位 {{ orderDetail?.slotId || selectedOrder?.slotId }}</p>
            </div>
            <button class="primary-button small" @click="exportCurrentView">导出订单视图</button>
          </div>

          <div class="summary-strip">
            <div v-for="[label, value] in settlementSummary" :key="label">
              <span>{{ label }}</span>
              <strong>{{ value }}</strong>
            </div>
          </div>

          <div class="detail-section-grid">
            <section>
              <h3>订单时间线</h3>
              <div class="timeline-list">
                <div v-for="[title, time, desc] in orderTimeline" :key="title">
                  <b>{{ title }}</b>
                  <span>{{ zhText(time) }}</span>
                  <small>{{ zhText(desc) }}</small>
                </div>
              </div>
            </section>

            <section>
              <h3>车主与车辆</h3>
              <div class="info-list">
                <div><span>车主</span><b>{{ orderDetail?.customer?.ownerName || "未关联" }}</b></div>
                <div><span>联系方式</span><b>{{ orderDetail?.customer?.phoneMasked || "暂无" }}</b></div>
                <div><span>会员等级</span><b>{{ zhText(orderDetail?.customer?.memberLevel) }}</b></div>
                <div><span>能源类型</span><b>{{ zhText(orderDetail?.vehicle?.energyType) }}</b></div>
              </div>
            </section>

            <section>
              <h3>支付信息</h3>
              <div class="info-list">
                <div><span>支付状态</span><b>{{ zhText(orderDetail?.paymentStatus) }}</b></div>
                <div><span>支付流水</span><b>{{ orderDetail?.payment?.paymentNo || "暂无" }}</b></div>
                <div><span>支付方式</span><b>{{ zhText(orderDetail?.payment?.method) }}</b></div>
                <div><span>支付金额</span><b>{{ zhMoney(orderDetail?.payment?.amount || orderDetail?.amount) }}</b></div>
              </div>
            </section>

            <section>
              <h3>计费明细</h3>
              <div class="info-list">
                <div v-for="item in selectedBillingRows" :key="item.componentNo || item.description">
                  <span>{{ zhText(item.componentType || item.label) }}</span>
                  <b>{{ zhMoney(item.amount) }}</b>
                  <small>{{ zhText(item.description || item.formula) }}</small>
                </div>
              </div>
            </section>
          </div>
        </article>
      </section>
    </template>

    <template v-if="section === 'customers'">
      <article class="surface ledger-toolbar">
        <div>
          <h2>客户与车辆</h2>
          <p>合并原来的客户车辆和准入名单。围绕车主查看车辆、会员权益、白名单/黑名单和最近订单。</p>
        </div>
        <div class="toolbar-actions">
          <div class="search-field">
            <i class="fa-solid fa-magnifying-glass"></i>
            <input v-model="customerKeyword" placeholder="搜索车主、手机号、车牌或会员等级" />
          </div>
          <button class="primary-button small" @click="openNewVehicle"><i class="fa-solid fa-plus"></i> 新增车辆</button>
          <button class="ghost-button small" @click="exportCurrentView">导出</button>
        </div>
      </article>

      <article v-if="showVehicleForm" class="surface vehicle-form-card">
        <div class="section-head compact">
          <h2>{{ vehicleEditing ? "编辑车辆档案" : "新增车辆档案" }}</h2>
          <button class="ghost-button small" @click="showVehicleForm = false"><i class="fa-solid fa-xmark"></i> 关闭</button>
        </div>
        <div class="vehicle-form">
          <label><span>车牌号</span><input v-model.trim="vehicleForm.plateNo" :disabled="vehicleEditing" placeholder="沪A12345" /></label>
          <label><span>车主姓名</span><input v-model.trim="vehicleForm.ownerName" placeholder="张三" /></label>
          <label><span>联系电话</span><input v-model.trim="vehicleForm.phoneMasked" placeholder="138****0000" /></label>
          <label>
            <span>能源类型</span>
            <select v-model="vehicleForm.energyType">
              <option value="Fuel">燃油</option>
              <option value="Electric">新能源</option>
            </select>
          </label>
          <label>
            <span>会员等级</span>
            <select v-model="vehicleForm.memberLevel">
              <option value="Standard">普通</option>
              <option value="Gold">金卡</option>
              <option value="VIP">VIP</option>
            </select>
          </label>
          <label>
            <span>准入策略</span>
            <select v-model="vehicleForm.accessType">
              <option value="Allow">放行</option>
              <option value="Blocklist">黑名单</option>
            </select>
          </label>
        </div>
        <div class="vehicle-form-actions">
          <button class="primary-button small" :disabled="!vehicleForm.plateNo.trim()" @click="saveVehicle">
            <i class="fa-solid fa-floppy-disk"></i> {{ vehicleEditing ? "保存修改" : "登记车辆" }}
          </button>
        </div>
      </article>

      <section class="ledger-layout detail">
        <aside class="surface ledger-list">
          <div class="section-head compact">
            <h2>客户</h2>
            <span class="admin-count">{{ filteredCustomers.length }} 条</span>
          </div>
          <button
            v-for="customer in filteredCustomers"
            :key="`${customer.ownerId}-${customer.plateNo}`"
            class="ledger-row"
            :class="{ active: selectedCustomer?.ownerId === customer.ownerId }"
            @click="selectCustomer(customer.ownerId)"
          >
            <b>{{ customer.ownerName }}</b>
            <span>{{ customer.phoneMasked }} / {{ customer.plateNo }}</span>
            <small>{{ zhText(customer.memberLevel) }} / {{ zhText(customer.accessType) }}</small>
          </button>
        </aside>

        <article class="surface ledger-detail">
          <div class="detail-title">
            <div>
              <span>当前客户</span>
              <h2>{{ state.adminCustomerDetail?.ownerName || selectedCustomer?.ownerName || "请选择客户" }}</h2>
              <p>{{ state.adminCustomerDetail?.ownerId || selectedCustomer?.ownerId }} / {{ state.adminCustomerDetail?.phoneMasked || selectedCustomer?.phoneMasked }}</p>
            </div>
            <span class="status-pill stable">{{ zhText(state.adminCustomerDetail?.accountStatus || selectedCustomer?.accountStatus) }}</span>
          </div>

          <div class="detail-section-grid customer">
            <section>
              <h3>账户信息</h3>
              <div class="info-list">
                <div><span>会员等级</span><b>{{ zhText(state.adminCustomerDetail?.memberLevel || selectedCustomer?.memberLevel) }}</b></div>
                <div><span>账户余额</span><b>{{ zhMoney(state.adminCustomerDetail?.balance) }}</b></div>
                <div><span>最近支付</span><b>{{ zhText(state.adminCustomerDetail?.lastPaymentAt) }}</b></div>
                <div><span>开户时间</span><b>{{ zhText(state.adminCustomerDetail?.createdAt) }}</b></div>
              </div>
            </section>

            <section>
              <h3>车辆档案</h3>
              <div class="info-list">
                <div v-for="vehicle in customerVehicles" :key="vehicle.plateNo" class="vehicle-row">
                  <div>
                    <span>{{ vehicle.plateNo }}</span>
                    <b>{{ zhText(vehicle.energyType) }} / {{ zhText(vehicle.membershipType) }}</b>
                    <small>{{ zhText(vehicle.accessType) }}</small>
                  </div>
                  <div class="vehicle-row-actions">
                    <button title="编辑" @click="editVehicle(vehicle)"><i class="fa-solid fa-pen"></i></button>
                    <button title="删除" class="danger" @click="deleteVehicle(vehicle.plateNo)"><i class="fa-solid fa-trash"></i></button>
                  </div>
                </div>
                <p v-if="!customerVehicles.length" class="empty-state">该客户暂无车辆档案。</p>
              </div>
            </section>

            <section>
              <h3>准入权限</h3>
              <div class="info-list">
                <div v-for="access in accessForCustomer" :key="access.plateNo">
                  <span>{{ access.plateNo }}</span>
                  <b>{{ zhText(access.listType) }}</b>
                  <small>{{ zhText(access.validUntil) }} / {{ zhText(access.remark) }}</small>
                </div>
                <p v-if="!accessForCustomer.length" class="empty-state">暂无单独准入规则。</p>
              </div>
            </section>

            <section>
              <h3>最近订单</h3>
              <div class="info-list">
                <div v-for="order in customerOrders" :key="order.orderNo">
                  <span>{{ order.orderNo }}</span>
                  <b>{{ order.plateNo }} / {{ zhText(order.status) }}</b>
                  <small>{{ zhMoney(order.amount) }}</small>
                </div>
                <p v-if="!customerOrders.length" class="empty-state">暂无订单记录。</p>
              </div>
            </section>
          </div>
        </article>
      </section>
    </template>

    <template v-if="section === 'exceptions'">
      <article class="surface ledger-toolbar">
        <div>
          <h2>异常处理</h2>
          <p>告警记录不再只是表格。员工需要看到告警内容、关联设备事件、订单线索和建议动作。</p>
        </div>
        <div class="toolbar-actions">
          <div class="search-field">
            <i class="fa-solid fa-magnifying-glass"></i>
            <input v-model="alertKeyword" placeholder="搜索告警号、类型、内容或状态" />
          </div>
          <button class="ghost-button small" @click="exportCurrentView">导出</button>
        </div>
      </article>

      <section class="ledger-layout detail">
        <aside class="surface ledger-list">
          <div class="section-head compact">
            <h2>告警</h2>
            <span class="admin-count">{{ filteredAlerts.length }} 条</span>
          </div>
          <button
            v-for="alert in filteredAlerts"
            :key="alert.alertNo"
            class="ledger-row alert"
            :class="{ active: selectedAlert?.alertNo === alert.alertNo, danger: zhText(alert.level) === '高' }"
            @click="selectAlert(alert.alertNo)"
          >
            <b>{{ zhText(alert.type) }}</b>
            <span>{{ zhText(alert.content) }}</span>
            <small>{{ alert.alertNo }} / {{ zhText(alert.status) }} / {{ zhText(alert.level) }}</small>
          </button>
        </aside>

        <article class="surface ledger-detail">
          <div class="detail-title">
            <div>
              <span>当前异常</span>
              <h2>{{ zhText(state.adminAlertDetail?.type || selectedAlert?.type) }}</h2>
              <p>{{ state.adminAlertDetail?.alertNo || selectedAlert?.alertNo }} / {{ zhText(state.adminAlertDetail?.status || selectedAlert?.status) }}</p>
            </div>
            <span class="status-pill" :class="zhText(state.adminAlertDetail?.level || selectedAlert?.level) === '高' ? 'warning' : 'stable'">
              {{ zhText(state.adminAlertDetail?.level || selectedAlert?.level) }}
            </span>
          </div>

          <div class="exception-callout">
            <b>{{ zhText(state.adminAlertDetail?.content || selectedAlert?.content) }}</b>
            <span>{{ zhText(state.adminAlertDetail?.recommendedAction || "查看关联设备事件并安排人工复核。") }}</span>
          </div>

          <div v-if="selectedAlert" class="alert-actions">
            <button class="ghost-button small" :disabled="alertAcknowledged" @click="ackSelectedAlert">
              <i class="fa-solid fa-user-check"></i> {{ alertAcknowledged ? "已确认" : "确认处理" }}
            </button>
            <button class="primary-button small" :disabled="alertResolved" @click="resolveSelectedAlert">
              <i class="fa-solid fa-circle-check"></i> {{ alertResolved ? "已解除" : "解除告警" }}
            </button>
            <span class="alert-state-pill" :class="alertResolved ? 'done' : alertAcknowledged ? 'ack' : 'open'">
              当前状态：{{ zhText(selectedAlert.status) }}
            </span>
          </div>

          <div class="detail-section-grid exception">
            <section>
              <h3>关联设备事件</h3>
              <div class="info-list">
                <div v-for="event in state.adminAlertDetail?.deviceEvents || []" :key="event.eventId">
                  <span>{{ event.deviceId }} / {{ zhText(event.eventCode) }}</span>
                  <b>{{ zhText(event.severity) }}</b>
                  <small>{{ zhText(event.message) }}</small>
                </div>
                <p v-if="!(state.adminAlertDetail?.deviceEvents || []).length" class="empty-state">暂无设备事件。</p>
              </div>
            </section>

            <section>
              <h3>订单线索</h3>
              <div class="info-list">
                <div v-for="order in state.adminAlertDetail?.relatedOrders || []" :key="order.orderNo">
                  <span>{{ order.orderNo }}</span>
                  <b>{{ order.plateNo }} / {{ order.slotId }}</b>
                  <small>{{ zhText(order.status) }} / {{ zhMoney(order.amount) }}</small>
                </div>
                <p v-if="!(state.adminAlertDetail?.relatedOrders || []).length" class="empty-state">暂无关联订单。</p>
              </div>
            </section>
          </div>
        </article>
      </section>
    </template>
  </section>
</template>

<style scoped>
.ledger-workbench {
  display: grid;
  gap: 20px;
}

.ledger-nav {
  display: grid;
  gap: 18px;
}

.ledger-nav p,
.ledger-toolbar p,
.section-head p,
.detail-title p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.ledger-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.ledger-tab {
  min-height: 78px;
  display: grid;
  align-content: center;
  gap: 6px;
  padding: 14px;
  text-align: left;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: linear-gradient(180deg, #fff, #f8fafc);
}

.ledger-tab.active {
  color: var(--brand);
  border-color: rgba(79, 70, 229, 0.34);
  background: rgba(79, 70, 229, 0.07);
  box-shadow: inset 3px 0 0 var(--brand);
}

.ledger-tab b,
.ledger-tab span {
  display: block;
}

.ledger-tab span {
  color: var(--text-muted);
  font-size: 12px;
}

.ledger-overview {
  margin-bottom: 0;
}

.ledger-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
  gap: 20px;
}

.ledger-layout.detail {
  grid-template-columns: 340px minmax(0, 1fr);
}

.ledger-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
}

.toolbar-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-field,
.query-line {
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: #fff;
}

.search-field {
  min-width: 320px;
}

.search-field i {
  color: var(--text-muted);
}

.search-field input,
.query-line input {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--text-main);
}

.query-line {
  margin-bottom: 14px;
}

.report-card,
.exception-callout {
  padding: 18px;
  border-radius: 12px;
  border: 1px solid rgba(79, 70, 229, 0.18);
  background: rgba(79, 70, 229, 0.05);
}

.report-card b,
.report-card p,
.exception-callout b,
.exception-callout span {
  display: block;
}

.report-card p,
.exception-callout span {
  margin: 8px 0 0;
  color: var(--text-muted);
  line-height: 1.65;
  font-size: 13px;
}

.report-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.report-card.ai-live {
  border-color: rgba(16, 185, 129, 0.3);
  background: rgba(16, 185, 129, 0.05);
}

.report-card b i {
  margin-right: 7px;
  color: var(--brand);
}

.report-card.ai-live b i {
  color: var(--safety-green);
}

.report-source {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 700;
  vertical-align: middle;
}

.report-card p.report-body {
  white-space: pre-wrap;
  color: var(--text-main);
}

.report-error {
  display: block;
  margin-top: 8px;
  color: #b45309;
  font-size: 12px;
}

.priority-list,
.ledger-list,
.info-list,
.timeline-list {
  display: grid;
  gap: 10px;
}

.ledger-list {
  align-content: start;
  max-height: calc(100vh - 260px);
  overflow: auto;
}

.priority-item,
.ledger-row {
  display: grid;
  gap: 5px;
  min-height: 86px;
  align-content: center;
  padding: 12px 14px;
  text-align: left;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: linear-gradient(180deg, #fff, #f8fafc);
}

.priority-item:hover,
.ledger-row:hover {
  border-color: rgba(79, 70, 229, 0.24);
}

.priority-item span,
.priority-item small,
.ledger-row span,
.ledger-row small {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.45;
}

.priority-item strong {
  color: var(--brand);
  font-family: "Outfit", sans-serif;
  font-size: 28px;
}

.ledger-row.active {
  border-color: rgba(79, 70, 229, 0.38);
  background: rgba(79, 70, 229, 0.07);
  box-shadow: inset 3px 0 0 var(--brand);
}

.ledger-row.danger {
  border-left: 4px solid var(--danger-red);
}

.ledger-row b {
  color: var(--text-main);
  font-size: 16px;
}

.detail-title {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.detail-title span:first-child {
  color: var(--brand);
  font-size: 12px;
  font-weight: 800;
}

.detail-title h2 {
  margin: 5px 0;
  font-size: 30px;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.summary-strip div,
.detail-section-grid > section,
.info-list div,
.timeline-list div {
  padding: 14px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: linear-gradient(180deg, #fff, #f8fafc);
}

.summary-strip span,
.info-list span,
.timeline-list span {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}

.summary-strip strong {
  display: block;
  margin-top: 5px;
  color: var(--text-main);
  font-family: "Outfit", sans-serif;
  font-size: 19px;
}

.detail-section-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-section-grid.customer,
.detail-section-grid.exception {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-section-grid h3 {
  margin: 0 0 12px;
  color: var(--text-main);
  font-size: 16px;
}

.info-list b,
.timeline-list b {
  display: block;
  margin-top: 4px;
  color: var(--text-main);
  font-size: 14px;
}

.info-list small,
.timeline-list small {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.45;
}

.alert-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.alert-state-pill {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
}

.alert-state-pill.open {
  color: var(--danger-red);
  background: rgba(239, 68, 68, 0.1);
}

.alert-state-pill.ack {
  color: var(--warning-yellow);
  background: rgba(245, 158, 11, 0.12);
}

.alert-state-pill.done {
  color: var(--safety-green);
  background: rgba(16, 185, 129, 0.12);
}

.vehicle-form-card {
  border-top: 3px solid var(--brand);
}

.vehicle-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 8px;
}

.vehicle-form label {
  display: grid;
  gap: 6px;
}

.vehicle-form label span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.vehicle-form input,
.vehicle-form select {
  height: 40px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: #fff;
  font-size: 14px;
  color: var(--text-main);
  outline: none;
}

.vehicle-form input:focus,
.vehicle-form select:focus {
  border-color: var(--brand);
}

.vehicle-form input:disabled {
  background: rgba(0, 0, 0, 0.03);
  color: var(--text-muted);
}

.vehicle-form-actions {
  margin-top: 16px;
}

.vehicle-row {
  display: flex !important;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.vehicle-row > div {
  padding: 0;
  border: none;
  background: none;
}

.vehicle-row-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.vehicle-row-actions button {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.vehicle-row-actions button:hover {
  color: var(--brand);
  border-color: rgba(79, 70, 229, 0.3);
}

.vehicle-row-actions button.danger:hover {
  color: var(--danger-red);
  border-color: rgba(239, 68, 68, 0.4);
}

.empty-state {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

@media (max-width: 1320px) {
  .ledger-tabs,
  .summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ledger-layout,
  .ledger-layout.detail,
  .detail-section-grid,
  .detail-section-grid.customer,
  .detail-section-grid.exception {
    grid-template-columns: 1fr;
  }

  .ledger-toolbar,
  .detail-title {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .ledger-tabs,
  .summary-strip {
    grid-template-columns: 1fr;
  }

  .search-field {
    min-width: min(320px, 100%);
  }
}
</style>
