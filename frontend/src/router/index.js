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
  { path: "/", name: "dashboard", component: DashboardView, meta: { title: "Operations Dashboard" } },
  { path: "/twin", name: "twin", component: TwinView, meta: { title: "Digital Twin" } },
  { path: "/ai", name: "ai", component: AiVisionView, meta: { title: "AI Vision" } },
  { path: "/dispatch", name: "dispatch", component: DispatchCenterView, meta: { title: "Dispatch Center" } },
  { path: "/admin", name: "admin", component: AdminConsoleView, meta: { title: "Reports and Records" } },
  { path: "/pricing", name: "pricing", component: PricingEngineView, meta: { title: "Pricing Engine" } },
  { path: "/system", name: "system", component: SystemConfigView, meta: { title: "System Gateway" } },
  { path: "/owner", name: "owner", component: OwnerPortalView, meta: { title: "Owner Portal" } },
  { path: "/indoor-map", name: "indoor-map", component: IndoorMapView, meta: { title: "Indoor Navigation" } },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
