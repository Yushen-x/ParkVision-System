import { createRouter, createWebHistory } from "vue-router";

const routes = [
  { path: "/", name: "dashboard", component: () => import("../views/DashboardView.vue"), meta: { title: "运营首页" } },
  { path: "/twin", name: "twin", component: () => import("../views/TwinView.vue"), meta: { title: "数字孪生" } },
  { path: "/ai", name: "ai", component: () => import("../views/AiVisionView.vue"), meta: { title: "AI 视觉感知" } },
  { path: "/dispatch", name: "dispatch", component: () => import("../views/DispatchCenterView.vue"), meta: { title: "调度中心" } },
  { path: "/admin", name: "admin", component: () => import("../views/AdminConsoleView.vue"), meta: { title: "管理报表与台账" } },
  { path: "/pricing", name: "pricing", component: () => import("../views/PricingEngineView.vue"), meta: { title: "动态计费引擎" } },
  { path: "/system", name: "system", component: () => import("../views/SystemConfigView.vue"), meta: { title: "系统网关" } },
  { path: "/owner", name: "owner", component: () => import("../views/OwnerPortalView.vue"), meta: { title: "车主端" } },
  { path: "/indoor-map", name: "indoor-map", component: () => import("../views/IndoorMapView.vue"), meta: { title: "室内导航" } },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
