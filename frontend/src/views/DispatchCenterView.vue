<script setup>
import { state } from "../stores/parkingStore";
</script>

<template>
  <section class="dispatch-layout">
    <article class="surface">
      <div class="section-head">
        <div>
          <h2>大模型柔性调度引擎</h2>
          <p>将自然语言策略转化为 AGV 任务队列，动态插队处理。</p>
        </div>
      </div>
      <div class="queue-list" style="margin-top:16px;">
        <div v-for="(task, index) in state.queue" :key="index" class="queue-item" :class="{ vip: task.vip }">
          <div class="queue-rank">{{ index + 1 }}</div>
          <div>
            <b>{{ task.type }} - {{ task.plate }}</b>
            <span><i class="fa-regular fa-clock"></i> 预计等待: {{ task.wait }}</span>
          </div>
          <span class="queue-tag"><i class="fa-solid fa-tag"></i> {{ task.tag }}</span>
        </div>
      </div>
    </article>

    <aside>
      <article class="surface" style="height: 100%;">
        <div class="section-head compact">
          <h2>生效中的策略规则</h2>
        </div>
        <div style="margin-top:16px;">
            <div class="strategy-card">
            <strong><i class="fa-solid fa-bolt" style="color:var(--warning-yellow); margin-right:6px;"></i>VIP 优先级穿透</strong>
            <span>针对加急取车订单，直接插入 AGV 调度队列首位，绝对优先。</span>
            </div>
            <div class="strategy-card">
            <strong><i class="fa-solid fa-plug-circle-check" style="color:var(--safety-green); margin-right:6px;"></i>充电桩潮汐释放</strong>
            <span>检测新能源车电量满80%且排队增多时，触发移库。</span>
            </div>
            <div class="strategy-card">
            <strong><i class="fa-solid fa-forward-step" style="color:var(--brand); margin-right:6px;"></i>Pre-Dispatch 预调</strong>
            <span>根据 Prophet 流量预测，提前 20 分钟将热门库位车辆移出。</span>
            </div>
        </div>
      </article>
    </aside>
  </section>
</template>
