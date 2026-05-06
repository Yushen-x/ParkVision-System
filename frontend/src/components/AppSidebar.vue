<script setup>
defineProps({ mode: { type: String, default: "联调演示中" } });

const navItems = [
  ["dashboard", "/", "fa-solid fa-chart-line", "总览驾驶舱"],
  ["twin", "/twin", "fa-solid fa-cubes", "数字孪生"],
  ["ai", "/ai", "fa-solid fa-eye", "AI 感知"],
  ["dispatch", "/dispatch", "fa-solid fa-network-wired", "调度中枢"],
  ["admin", "/admin", "fa-solid fa-chart-pie", "报表与记录"],
  ["pricing", "/pricing", "fa-solid fa-tags", "动态定价"],
  ["system", "/system", "fa-solid fa-server", "网关与配置"],
  ["owner", "/owner", "fa-solid fa-mobile-screen-button", "车主主页"],
  ["indoor-map", "/indoor-map", "fa-solid fa-map-location-dot", "接续导航"],
];
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">PV</div>
      <div>
        <strong>ParkVision</strong>
        <span>CPS 智慧停车平台</span>
      </div>
    </div>
    <nav class="nav-list" aria-label="主导航">
      <RouterLink v-for="[name, path, icon, label] in navItems" :key="name" class="nav-item" :to="path">
        <span class="nav-icon"><i :class="icon"></i></span>
        <span>{{ label }}</span>
      </RouterLink>
    </nav>
    <div class="sidebar-panel">
      <span class="panel-label"><i class="fa-solid fa-circle-dot" style="color:var(--safety-green); margin-right:6px;"></i>演示状态</span>
      <strong>{{ mode }}</strong>
      <p>边缘侧端云协同模式，微服务集群在线。</p>
    </div>
  </aside>
</template>
