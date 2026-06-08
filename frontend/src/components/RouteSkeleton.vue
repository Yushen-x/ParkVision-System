<script setup>
defineProps({
  title: {
    type: String,
    default: "",
  },
});
</script>

<template>
  <section class="route-skeleton" :aria-label="`${title}加载中`">
    <div class="kpi-grid route-skeleton-kpis">
      <article v-for="card in 4" :key="`metric-${card}`" class="surface route-skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line headline"></div>
        <div class="skeleton-line medium"></div>
      </article>
    </div>

    <div class="route-skeleton-layout">
      <section class="surface route-skeleton-panel">
        <div class="route-skeleton-head">
          <div>
            <div class="skeleton-line section-title"></div>
            <div class="skeleton-line medium"></div>
          </div>
          <div class="route-skeleton-actions">
            <div class="skeleton-chip"></div>
            <div class="skeleton-chip wide"></div>
          </div>
        </div>

        <div class="route-skeleton-toolbar">
          <div class="skeleton-input"></div>
          <div class="skeleton-chip"></div>
          <div class="skeleton-chip"></div>
        </div>

        <div class="route-skeleton-table">
          <div class="route-skeleton-row header" v-for="row in 1" :key="`header-${row}`">
            <span v-for="cell in 5" :key="`header-cell-${cell}`" class="skeleton-line"></span>
          </div>
          <div class="route-skeleton-row" v-for="row in 5" :key="`row-${row}`">
            <span v-for="cell in 5" :key="`row-${row}-cell-${cell}`" class="skeleton-line"></span>
          </div>
        </div>
      </section>

      <aside class="route-skeleton-stack">
        <section class="surface route-skeleton-panel">
          <div class="route-skeleton-head compact">
            <div class="skeleton-line section-title"></div>
            <div class="skeleton-chip"></div>
          </div>
          <div class="route-skeleton-list">
            <div v-for="item in 4" :key="`summary-${item}`" class="route-skeleton-list-item">
              <div class="skeleton-dot"></div>
              <div class="route-skeleton-copy">
                <div class="skeleton-line medium"></div>
                <div class="skeleton-line short"></div>
              </div>
            </div>
          </div>
        </section>

        <section class="surface route-skeleton-panel">
          <div class="route-skeleton-head compact">
            <div class="skeleton-line section-title"></div>
          </div>
          <div class="route-skeleton-chart">
            <div class="skeleton-bar" v-for="bar in 7" :key="`bar-${bar}`" :style="{ '--bar-height': `${36 + bar * 10}px` }"></div>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>
