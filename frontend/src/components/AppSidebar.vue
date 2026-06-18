<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { logout, state } from "../stores/parkingStore";

const router = useRouter();
const currentUser = computed(() => state.auth.user);

function doLogout() {
  logout();
  router.replace({ name: "login" });
}

const navItems = [
  ["dashboard", "/", "fa-solid fa-chart-line", "运营首页"],
  ["twin", "/twin", "fa-solid fa-cubes", "数字孪生"],
  ["ai", "/ai", "fa-solid fa-robot", "AI 视觉中枢"],
  ["dispatch", "/dispatch", "fa-solid fa-route", "履约中枢"],
  ["admin", "/admin", "fa-solid fa-chart-pie", "管理台账"],
  ["system", "/system", "fa-solid fa-server", "系统状态"],
];
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">PV</div>
      <div>
        <strong>ParkVision</strong>
        <span>智能停车履约管理系统</span>
      </div>
    </div>
    <nav class="nav-list" aria-label="主导航">
      <RouterLink v-for="[name, path, icon, label] in navItems" :key="name" class="nav-item" :to="path">
        <span class="nav-icon"><i :class="icon"></i></span>
        <span>{{ label }}</span>
      </RouterLink>
    </nav>
    <div class="sidebar-footer">
      <div v-if="currentUser" class="sidebar-user">
        <div class="sidebar-user-info">
          <span class="sidebar-user-avatar"><i class="fa-solid" :class="currentUser.role === 'admin' ? 'fa-user-shield' : 'fa-user'"></i></span>
          <div>
            <strong>{{ currentUser.displayName || currentUser.username }}</strong>
            <span>{{ currentUser.role === "admin" ? "管理端" : "车主端" }}</span>
          </div>
        </div>
        <button class="sidebar-logout" title="退出登录" @click="doLogout"><i class="fa-solid fa-arrow-right-from-bracket"></i></button>
      </div>
      <span class="version-tag">PV-CPS v1.2.0</span>
    </div>
  </aside>
</template>

<style scoped>
.sidebar-user {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
  border-radius: 12px;
  background: rgba(79, 70, 229, 0.06);
  border: 1px solid rgba(79, 70, 229, 0.12);
}

.sidebar-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.sidebar-user-avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
}

.sidebar-user-info strong {
  display: block;
  font-size: 13px;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 110px;
}

.sidebar-user-info span {
  font-size: 11px;
  color: var(--text-muted);
}

.sidebar-logout {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
  border-radius: 9px;
  background: #fff;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.sidebar-logout:hover {
  color: var(--danger-red);
  border-color: rgba(239, 68, 68, 0.4);
}
</style>
