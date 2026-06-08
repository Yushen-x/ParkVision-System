<script setup>
import { zhText } from "../utils/localize";

defineProps({ mode: { type: String, default: "后端优先" } });

const navItems = [
  ["dashboard", "/", "fa-solid fa-chart-line", "运营首页"],
  ["twin", "/twin", "fa-solid fa-cubes", "数字孪生"],
  ["ai", "/ai", "fa-solid fa-eye", "AI 视觉感知"],
  ["dispatch", "/dispatch", "fa-solid fa-network-wired", "调度中心"],
  ["admin", "/admin", "fa-solid fa-chart-pie", "管理台"],
  ["pricing", "/pricing", "fa-solid fa-tags", "动态计费"],
  ["system", "/system", "fa-solid fa-server", "系统配置"],
  ["owner", "/owner", "fa-solid fa-mobile-screen-button", "车主端"],
  ["indoor-map", "/indoor-map", "fa-solid fa-map-location-dot", "室内导航"],
];
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">PV</div>
      <div>
        <strong>ParkVision</strong>
        <span>智能停车 CPS 演示系统</span>
      </div>
    </div>
    <nav class="nav-list" aria-label="主导航">
      <RouterLink v-for="[name, path, icon, label] in navItems" :key="name" class="nav-item" :to="path">
        <span class="nav-icon"><i :class="icon"></i></span>
        <span>{{ label }}</span>
      </RouterLink>
    </nav>
    <div class="sidebar-footer">
      <div class="system-status">
        <span class="status-indicator-dot"></span>
        <span>系统在线</span>
      </div>
      <span class="version-tag">PV-CPS v1.2.0</span>
    </div>
  </aside>
</template>
