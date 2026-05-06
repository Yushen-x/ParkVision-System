<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute } from "vue-router";
import AppSidebar from "./components/AppSidebar.vue";
import TopBar from "./components/TopBar.vue";
import { hydrate, moveAgvs, simulateEntry, state, toggleEmergency, triggerPreDispatch } from "./stores/parkingStore";

const route = useRoute();
const title = computed(() => route.meta.title || "总览驾驶舱");
const timer = ref(null);

onMounted(async () => {
  await hydrate();
  timer.value = window.setInterval(moveAgvs, 900);
});

onUnmounted(() => {
  if (timer.value) window.clearInterval(timer.value);
});
</script>

<template>
  <div class="app-shell">
    <AppSidebar :mode="state.emergency ? '安全急停' : '联调演示中'" />
    <main class="main">
      <TopBar
        :title="title"
        :emergency="state.emergency"
        @entry="simulateEntry"
        @pre-dispatch="triggerPreDispatch"
        @emergency="toggleEmergency"
      />
      <RouterView />
    </main>
  </div>
</template>
