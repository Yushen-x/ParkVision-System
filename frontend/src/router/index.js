import { createRouter, createWebHistory } from "vue-router";
import { getters, state } from "../stores/parkingStore";

const routes = [
  { path: "/login", name: "login", component: () => import("../views/LoginView.vue"), meta: { title: "登录", public: true } },
  // 大屏管理端
  { path: "/", name: "dashboard", component: () => import("../views/DashboardView.vue"), meta: { title: "运营首页" } },
  { path: "/twin", name: "twin", component: () => import("../views/TwinView.vue"), meta: { title: "数字孪生" } },
  { path: "/ai", name: "ai", component: () => import("../views/AiVisionView.vue"), meta: { title: "AI 视觉中枢" } },
  { path: "/dispatch", name: "dispatch", component: () => import("../views/DispatchCenterView.vue"), meta: { title: "履约中枢" } },
  { path: "/admin", name: "admin", component: () => import("../views/AdminConsoleView.vue"), meta: { title: "管理台账" } },
  { path: "/system", name: "system", component: () => import("../views/SystemConfigView.vue"), meta: { title: "系统状态" } },
  // 车主端（手机）
  { path: "/owner", name: "owner", component: () => import("../views/OwnerPortalView.vue"), meta: { title: "车主端", owner: true } },
  // 旧入口收敛
  { path: "/pricing", redirect: "/admin" },
  { path: "/indoor-map", redirect: "/owner" },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

function homeFor(role) {
  return role === "owner" ? "/owner" : "/";
}

router.beforeEach((to) => {
  if (to.meta.public) {
    if (to.name === "login" && getters.isAuthenticated.value) return homeFor(state.auth.user?.role);
    return true;
  }
  if (!getters.isAuthenticated.value) {
    return { name: "login", query: to.fullPath !== "/" ? { redirect: to.fullPath } : {} };
  }
  const role = state.auth.user?.role || "admin";
  if (role === "owner" && !to.meta.owner) return "/owner";
  if (role === "admin" && to.meta.owner) return "/";
  return true;
});

export default router;
