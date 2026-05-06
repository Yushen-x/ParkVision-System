<script setup>
import { computed, ref, onMounted } from "vue";
import DataTable from "../components/DataTable.vue";
import { accessList, pricingRules } from "../data/mockData";
import { state } from "../stores/parkingStore";
import * as echarts from 'echarts';

const tab = ref("report");
const report = ref("");
const isGenerating = ref(false);

const tableConfig = computed(() => {
  const configs = {
    orders: { headers: ["订单号", "车牌", "事件", "位置", "状态", "费用"], rows: state.orders },
    pricing: { headers: ["规则名", "适用时段", "计费方式", "附加策略", "状态"], rows: pricingRules },
    alerts: { headers: ["告警号", "类型", "内容", "处理状态", "级别"], rows: state.alerts },
    access: { headers: ["车牌", "名单类型", "用户类型", "有效期", "备注"], rows: accessList },
  };
  return configs[tab.value] || configs['orders'];
});

function generateReport() {
  isGenerating.value = true;
  setTimeout(() => {
    report.value = "AI 摘要：根据数据分析，过去7天 VIP加急服务总收益达 ¥3,250，周对比增长 24.5%。主要高峰集中在周五和周日晚间。建议提前 25 分钟执行 8 辆车的深浅库位迁移。";
    isGenerating.value = false;
    renderChart();
  }, 1500);
}

function renderChart() {
  const dom = document.getElementById('echartsMain');
  if(!dom) return;
  const myChart = echarts.init(dom, 'dark', {background: 'transparent'});
  myChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['上周收益', '本周收益'], textStyle: { color: '#94a3b8' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'], axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } } },
    yAxis: { type: 'value', axisLabel: { formatter: '¥ {value}' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } } },
    series: [
        { name: '上周收益', type: 'line', data: [120, 132, 101, 134, 290, 430, 410], smooth: true, itemStyle: {color: '#94a3b8'} },
        { name: '本周收益', type: 'line', data: [220, 182, 191, 234, 490, 530, 610], smooth: true, areaStyle: { opacity: 0.2 }, itemStyle: {color: '#38bdf8'} }
    ]
  });
  window.addEventListener('resize', () => myChart.resize());
}
</script>

<template>
  <section class="admin-grid">
    <article class="surface wide" style="padding: 0; background: transparent; border: none; box-shadow: none; backdrop-filter: none;">
      <div class="admin-tabs">
        <button class="tab" :class="{ active: tab === 'report' }" @click="tab = 'report'"><i class="fa-solid fa-wand-magic-sparkles" style="margin-right:6px;"></i>AI 智能报表</button>
        <button class="tab" :class="{ active: tab === 'orders' }" @click="tab = 'orders'">出入场记录</button>
        <button class="tab" :class="{ active: tab === 'pricing' }" @click="tab = 'pricing'">计费规则</button>
        <button class="tab" :class="{ active: tab === 'alerts' }" @click="tab = 'alerts'">异常告警</button>
        <button class="tab" :class="{ active: tab === 'access' }" @click="tab = 'access'">名单管理</button>
      </div>
    </article>

    <!-- Report View -->
    <template v-if="tab === 'report'">
        <article class="surface wide">
            <div class="section-head compact">
                <h2>AI 数据中台 (Text-to-SQL)</h2>
                <div style="display:flex; align-items:center; gap:8px;"><i class="fa-solid fa-circle-user" style="color:var(--brand); font-size:24px;"></i> Admin</div>
            </div>
            <div class="query-box">
                <p><i class="fa-solid fa-robot" style="color:var(--brand); margin-right:8px;"></i> 用自然语言查询业务数据并生成可视化报表</p>
                <div class="fake-input">
                    <input type="text" value="对比过去七天插队服务(VIP加急)带来的额外收益趋势" readonly>
                    <button class="primary-button" @click="generateReport" :disabled="isGenerating">
                        <i class="fa-solid" :class="isGenerating ? 'fa-spinner fa-spin' : 'fa-bolt'"></i> {{ isGenerating ? '生成中...' : '生成报表' }}
                    </button>
                </div>
            </div>
            
            <div v-if="report" class="report-output" style="animation: fadeIn 0.5s;">
                <strong><i class="fa-solid fa-chart-line"></i> 查询结果分析</strong>
                <p style="margin-top:10px; margin-bottom: 20px; color: #e2e8f0;">{{ report }}</p>
                <div id="echartsMain" style="width: 100%; height: 350px;"></div>
            </div>
        </article>
    </template>

    <!-- Table View -->
    <template v-else>
        <article class="surface wide">
            <div class="section-head">
                <div>
                <h2 style="text-transform: capitalize;">{{ tab }} 管理</h2>
                <p>实时同步底层业务流数据，支持高级筛选与导出。</p>
                </div>
                <button class="primary-button small">导出数据</button>
            </div>
            <div class="table-wrap">
                <DataTable :headers="tableConfig.headers" :rows="tableConfig.rows" />
            </div>
        </article>
    </template>
  </section>
</template>
