<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute } from "vue-router";
import AppSidebar from "./components/AppSidebar.vue";
import RouteSkeleton from "./components/RouteSkeleton.vue";
import TopBar from "./components/TopBar.vue";
import { hydrate, pollRealtime, simulateEntry, state, toggleEmergency, triggerPreDispatch } from "./stores/parkingStore";

const route = useRoute();
const title = computed(() => route.meta.title || "运营首页");
const timer = ref(null);

onMounted(async () => {
  await hydrate();
  timer.value = window.setInterval(() => {
    void pollRealtime();
  }, 5000);
});

onUnmounted(() => {
  if (timer.value) window.clearInterval(timer.value);
});
</script>

<template>
  <RouterView v-if="route.meta.public || route.meta.owner" />
  <div v-else class="app-shell">
    <AppSidebar :mode="state.onlineMode" />
    <main class="main">
      <TopBar
        :title="title"
        :emergency="state.emergency"
        :entry-busy="state.busy.entry"
        :dispatch-busy="state.busy.preDispatch"
        @entry="simulateEntry"
        @pre-dispatch="triggerPreDispatch"
        @emergency="toggleEmergency"
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
