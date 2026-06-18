import { onMounted, onUnmounted, ref } from "vue";

/** Reactive clock for live duration labels that must tick every second. */
export function useNow(intervalMs = 1000) {
  const now = ref(Date.now());
  let timerId = null;

  onMounted(() => {
    timerId = window.setInterval(() => {
      now.value = Date.now();
    }, intervalMs);
  });

  onUnmounted(() => {
    if (timerId) window.clearInterval(timerId);
  });

  return now;
}
