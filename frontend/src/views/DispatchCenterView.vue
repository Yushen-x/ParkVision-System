<script setup>
import { state } from "../stores/parkingStore";
</script>

<template>
  <section class="dispatch-layout">
    <article class="surface">
      <div class="section-head">
        <div>
          <h2>Live dispatch queue</h2>
          <p>Tasks are sourced from the backend queue first, including pre-dispatch and VIP insertions.</p>
        </div>
      </div>
      <div class="queue-list" style="margin-top:16px;">
        <div v-for="(task, index) in state.queue" :key="`${task.plateNo}-${index}`" class="queue-item" :class="{ vip: task.vip }">
          <div class="queue-rank">{{ index + 1 }}</div>
          <div>
            <b>{{ task.type }} - {{ task.plateNo }}</b>
            <span><i class="fa-regular fa-clock"></i> Estimated wait: {{ task.wait }}</span>
          </div>
          <span class="queue-tag"><i class="fa-solid fa-tag"></i> {{ task.tag }}</span>
        </div>
      </div>
    </article>

    <aside>
      <article class="surface" style="height: 100%;">
        <div class="section-head compact">
          <h2>Active policies</h2>
        </div>
        <div style="margin-top:16px;">
          <div class="strategy-card">
            <strong><i class="fa-solid fa-bolt" style="color:var(--warning-yellow); margin-right:6px;"></i>VIP queue override</strong>
            <span>Priority retrieval tasks are inserted at the head of the queue and immediately claim the lead AGV.</span>
          </div>
          <div class="strategy-card">
            <strong><i class="fa-solid fa-plug-circle-check" style="color:var(--safety-green); margin-right:6px;"></i>Charging bay release</strong>
            <span>Charging vehicles can be rotated out of premium bays when load is high and the queue length rises.</span>
          </div>
          <div class="strategy-card">
            <strong><i class="fa-solid fa-forward-step" style="color:var(--brand); margin-right:6px;"></i>Pre-dispatch relocation</strong>
            <span>Forecast-driven relocation pulls deep-slot vehicles toward the shallow buffer before the next surge window.</span>
          </div>
        </div>
      </article>
    </aside>
  </section>
</template>
