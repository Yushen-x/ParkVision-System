<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute } from "vue-router";
import AppSidebar from "./components/AppSidebar.vue";
import RouteSkeleton from "./components/RouteSkeleton.vue";
import TopBar from "./components/TopBar.vue";
import {
  connectTwinStream,
  disconnectTwinStream,
  hydrate,
  pollRealtime,
  resetSystem,
  simulateEntry,
  state,
  toggleEmergency,
  triggerPreDispatch,
} from "./stores/parkingStore";

const route = useRoute();
const title = computed(() => route.meta.title || "运营首页");
const timer = ref(null);

async function handleEntry() {
  const result = await simulateEntry();
  if (result.ok) {
    const order = result.order;
    const detail = order ? `车牌 ${order.plateNo} 已分配到车位 ${order.slotId}。` : "车辆已登记入场。";
    const modeNote = result.mode === "local" ? "\n\n（后端不可用，已在本地模拟入库。）" : "";
    window.alert(
      `登记入场：模拟车辆到达入口，系统自动分配空闲车位并创建停车订单。\n\n${detail}${modeNote}`,
    );
    return;
  }
  window.alert(result.error || "登记入场失败，请确认有空闲车位且后端已启动。");
}

async function handlePreDispatch() {
  const result = await triggerPreDispatch();
  if (result.ok) {
    const detail = result.plateNo
      ? `已为 ${result.plateNo} 创建预调度移位任务，AGV 将把车辆移向缓冲车道。`
      : "预调度任务已加入 AGV 队列。";
    const modeNote = result.mode === "local" ? "\n\n（后端不可用，已在本地模拟预调度。）" : "";
    window.alert(
      `触发预调度：将深层车位车辆提前移至缓冲车道，缩短高峰时段取车等待。\n\n${detail}${modeNote}`,
    );
    return;
  }
  window.alert(result.error || "预调度失败，请确认有在场订单且后端已启动。");
}

async function handleReset() {
  if (!window.confirm("确定将订单、车位、告警等演示数据恢复到初始状态吗？")) return;

  const result = await resetSystem();
  if (result.ok) {
    window.alert(
      result.mode === "local"
        ? "演示数据已在本地恢复。如需同步重置后端，请使用管理员账号重新登录后再试。"
        : "演示数据已重置为初始状态。",
    );
    return;
  }
  window.alert(result.error || "重置失败，请确认已使用管理员账号登录且后端已启动。");
}

onMounted(async () => {
  await hydrate();
  connectTwinStream();
  timer.value = window.setInterval(() => {
    void pollRealtime();
  }, 5000);
});

onUnmounted(() => {
  if (timer.value) window.clearInterval(timer.value);
  disconnectTwinStream();
});
</script>

<template>
  <RouterView v-if="route.meta.public || route.meta.owner" />
  <div v-else class="app-shell">
    <AppSidebar />
    <main class="main">
      <TopBar
        :title="title"
        :emergency="state.emergency"
        :entry-busy="state.busy.entry"
        :dispatch-busy="state.busy.preDispatch"
        :reset-busy="state.busy.reset"
        @entry="handleEntry"
        @pre-dispatch="handlePreDispatch"
        @emergency="toggleEmergency"
        @reset="handleReset"
      />
      <RouterView v-slot="{ Component }">
        <Suspense timeout="0">
          <component :is="Component" />
          <template #fallback>
            <RouteSkeleton :title="title" />
          </template>
        </Suspense>
      </RouterView>
    </main>
  </div>
</template>
