<script setup>
import EventStream from "../components/EventStream.vue";
import MetricCard from "../components/MetricCard.vue";
import SlotGrid from "../components/SlotGrid.vue";
import TrafficChart from "../components/TrafficChart.vue";
import { state } from "../stores/parkingStore";
</script>

<template>
  <section>
    <div class="kpi-grid">
      <MetricCard label="车位占用率" :value="`${state.summary.occupancyRate}%`" hint="车位状态由停车 API 实时刷新。" />
      <MetricCard label="今日车流量" :value="state.summary.trafficTotal" hint="统计入场、取车和预约出场活动。" />
      <MetricCard label="AGV 在线" :value="state.summary.agvOnline" hint="坐标由调度接口同步到数字孪生。" />
      <MetricCard label="实时告警" :value="state.summary.alertCount" hint="覆盖安全、设备和订单异常。" tone="alert" />
    </div>

    <div class="dashboard-layout">
      <article class="surface">
        <div class="section-head compact">
          <div>
            <h2>车流预测</h2>
            <p>历史车流和未来六个预测窗口会直接驱动高峰预调度决策。</p>
          </div>
          <span class="status-pill stable">API 已连接</span>
        </div>
        <TrafficChart :history="state.forecast.history" :prediction="state.forecast.prediction" />
      </article>

      <article class="surface">
        <div class="section-head compact">
          <h2>业务闭环</h2>
        </div>
        <div class="flow-stack" style="margin-top:16px;">
          <div class="flow-step done"><b>边缘 AI 感知</b><span>车牌 OCR 和安全区识别会把结构化事件写入后端。</span></div>
          <div class="flow-step done"><b>订单编排</b><span>订单、计费和车主操作都通过 API 落到数据库。</span></div>
          <div class="flow-step active"><b>调度决策</b><span>预调度和 VIP 取车任务会插入实时 AGV 队列。</span></div>
          <div class="flow-step"><b>孪生同步</b><span>车位、AGV 坐标和告警与管理台使用同一份后端数据。</span></div>
        </div>
      </article>
    </div>

    <div class="dashboard-layout lower">
      <article class="surface">
        <div class="section-head compact">
          <h2>车位概览</h2>
          <RouterLink class="text-button" to="/twin">打开数字孪生 ></RouterLink>
        </div>
        <SlotGrid :slots="state.slots" />
      </article>
      <article class="surface">
        <div class="section-head compact">
          <h2>事件流</h2>
          <span class="status-pill">实时</span>
        </div>
        <EventStream :events="state.events" />
      </article>
    </div>
  </section>
</template>
