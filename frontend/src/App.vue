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
        @entry="simulateEntry"
        @pre-dispatch="triggerPreDispatch"
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
