<script setup>
defineProps({ mode: { type: String, default: "Fallback-ready" } });

const navItems = [
  ["dashboard", "/", "fa-solid fa-chart-line", "Dashboard"],
  ["twin", "/twin", "fa-solid fa-cubes", "Digital Twin"],
  ["ai", "/ai", "fa-solid fa-eye", "AI Vision"],
  ["dispatch", "/dispatch", "fa-solid fa-network-wired", "Dispatch"],
  ["admin", "/admin", "fa-solid fa-chart-pie", "Admin Console"],
  ["pricing", "/pricing", "fa-solid fa-tags", "Pricing"],
  ["system", "/system", "fa-solid fa-server", "System"],
  ["owner", "/owner", "fa-solid fa-mobile-screen-button", "Owner Portal"],
  ["indoor-map", "/indoor-map", "fa-solid fa-map-location-dot", "Indoor Map"],
];
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">PV</div>
      <div>
        <strong>ParkVision</strong>
        <span>API-Driven Parking CPS</span>
      </div>
    </div>
    <nav class="nav-list" aria-label="Primary navigation">
      <RouterLink v-for="[name, path, icon, label] in navItems" :key="name" class="nav-item" :to="path">
        <span class="nav-icon"><i :class="icon"></i></span>
        <span>{{ label }}</span>
      </RouterLink>
    </nav>
    <div class="sidebar-panel">
      <span class="panel-label"><i class="fa-solid fa-circle-dot" style="color:var(--safety-green); margin-right:6px;"></i>Runtime mode</span>
      <strong>{{ mode }}</strong>
      <p>The frontend prefers live APIs first and falls back to local data only when backend endpoints are unavailable.</p>
    </div>
  </aside>
</template>
