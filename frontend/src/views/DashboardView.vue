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
      <MetricCard label="库容占用率" :value="`${state.summary.occupancyRate}%`" hint="车位状态由视觉实时同步" />
      <MetricCard label="今日车辆流量" :value="state.summary.trafficTotal" hint="入场、出场、预约累计" />
      <MetricCard label="AGV 在线数" :value="state.summary.agvOnline" hint="沙盘坐标毫秒级刷新" />
      <MetricCard label="异常告警" :value="state.summary.alertCount" hint="安全、设备链路可追溯" tone="alert" />
    </div>

    <div class="dashboard-layout">
      <article class="surface">
        <div class="section-head compact">
          <div>
            <h2>时序流量与空闲预测</h2>
            <p>基于时序模型预测未来出入库压力</p>
          </div>
          <span class="status-pill stable">AI ONLINE</span>
        </div>
        <TrafficChart :history="state.forecast.history" :prediction="state.forecast.prediction" />
      </article>

      <article class="surface">
        <div class="section-head compact">
          <h2>系统闭环链路</h2>
        </div>
        <div class="flow-stack" style="margin-top:16px;">
          <div class="flow-step done"><b>边缘 AI 感知</b><span>端侧 YOLOv8 车牌/人员识别</span></div>
          <div class="flow-step done"><b>业务中台流转</b><span>柔性计费、订单、C端交互</span></div>
          <div class="flow-step active"><b>大模型智能调度</b><span>Pre-Dispatch 深度前置调度决策</span></div>
          <div class="flow-step"><b>数字孪生与控制</b><span>底层 PLC 下发与 3D 实时反馈</span></div>
        </div>
      </article>
    </div>

    <div class="dashboard-layout lower">
      <article class="surface">
        <div class="section-head compact">
          <h2>实时库区俯视图</h2>
          <RouterLink class="text-button" to="/twin">3D 沙盘 ></RouterLink>
        </div>
        <SlotGrid :slots="state.slots" />
      </article>
      <article class="surface">
        <div class="section-head compact">
          <h2>系统事件总线</h2>
          <span class="status-pill">STREAM</span>
        </div>
        <EventStream :events="state.events" />
      </article>
    </div>
  </section>
</template>
