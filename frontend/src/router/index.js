import { createRouter, createWebHistory } from "vue-router";
import DashboardView from "../views/DashboardView.vue";
import OwnerPortalView from "../views/OwnerPortalView.vue";
import AdminConsoleView from "../views/AdminConsoleView.vue";
import TwinView from "../views/TwinView.vue";
import AiVisionView from "../views/AiVisionView.vue";
import DispatchCenterView from "../views/DispatchCenterView.vue";
import IndoorMapView from "../views/IndoorMapView.vue";
import PricingEngineView from "../views/PricingEngineView.vue";
import SystemConfigView from "../views/SystemConfigView.vue";

const routes = [
  { path: "/", name: "dashboard", component: DashboardView, meta: { title: "运营首页" } },
  { path: "/twin", name: "twin", component: TwinView, meta: { title: "数字孪生" } },
  { path: "/ai", name: "ai", component: AiVisionView, meta: { title: "AI 视觉感知" } },
  { path: "/dispatch", name: "dispatch", component: DispatchCenterView, meta: { title: "调度中心" } },
  { path: "/admin", name: "admin", component: AdminConsoleView, meta: { title: "管理报表与台账" } },
  { path: "/pricing", name: "pricing", component: PricingEngineView, meta: { title: "动态计费引擎" } },
  { path: "/system", name: "system", component: SystemConfigView, meta: { title: "系统网关" } },
  { path: "/owner", name: "owner", component: OwnerPortalView, meta: { title: "车主端" } },
  { path: "/indoor-map", name: "indoor-map", component: IndoorMapView, meta: { title: "室内导航" } },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
