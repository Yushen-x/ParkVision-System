import os

base_dir = r"D:\Desktop\G3-2course\专业综合项目\ParkVision-System\frontend\src"

files = {}

# 1. DispatchCenterView.vue (FIX BUG)
files["views/DispatchCenterView.vue"] = """<script setup>
import { state } from "../stores/parkingStore";
</script>

<template>
  <section class="dispatch-layout">
    <article class="surface">
      <div class="section-head">
        <div>
          <h2>大模型柔性调度引擎</h2>
          <p>将自然语言策略转化为 AGV 任务队列，动态插队处理。</p>
        </div>
      </div>
      <div class="queue-list" style="margin-top:16px;">
        <div v-for="(task, index) in state.queue" :key="index" class="queue-item" :class="{ vip: task.vip }">
          <div class="queue-rank">{{ index + 1 }}</div>
          <div>
            <b>{{ task.type }} - {{ task.plate }}</b>
            <span><i class="fa-regular fa-clock"></i> 预计等待: {{ task.wait }}</span>
          </div>
          <span class="queue-tag"><i class="fa-solid fa-tag"></i> {{ task.tag }}</span>
        </div>
      </div>
    </article>

    <aside>
      <article class="surface" style="height: 100%;">
        <div class="section-head compact">
          <h2>生效中的策略规则</h2>
        </div>
        <div style="margin-top:16px;">
            <div class="strategy-card">
            <strong><i class="fa-solid fa-bolt" style="color:var(--warning-yellow); margin-right:6px;"></i>VIP 优先级穿透</strong>
            <span>针对加急取车订单，直接插入 AGV 调度队列首位，绝对优先。</span>
            </div>
            <div class="strategy-card">
            <strong><i class="fa-solid fa-plug-circle-check" style="color:var(--safety-green); margin-right:6px;"></i>充电桩潮汐释放</strong>
            <span>检测新能源车电量满80%且排队增多时，触发移库。</span>
            </div>
            <div class="strategy-card">
            <strong><i class="fa-solid fa-forward-step" style="color:var(--brand); margin-right:6px;"></i>Pre-Dispatch 预调</strong>
            <span>根据 Prophet 流量预测，提前 20 分钟将热门库位车辆移出。</span>
            </div>
        </div>
      </article>
    </aside>
  </section>
</template>
"""

# 2. DemoGuideView.vue (REWRITE)
files["views/DemoGuideView.vue"] = """<template>
  <article class="surface demo-panel">
    <div class="section-head">
      <div>
        <h2>全链路系统答辩演练指南</h2>
        <p>汇报逻辑闭环：端侧感知 -> 柔性调度 -> 后台管控 -> 车主体验</p>
      </div>
    </div>
    <div class="demo-grid" style="margin-top:20px;">
      <div class="demo-step">
        <span>1</span><b><i class="fa-solid fa-bullseye" style="color:var(--brand);"></i> 痛点与破局</b>
        <p>讲述高并发取车堵塞、视觉防重叠、充电桩霸占难题，引入基于大模型预调度的 ParkVision。</p>
      </div>
      <div class="demo-step">
        <span>2</span><b><i class="fa-solid fa-mobile-screen" style="color:var(--brand);"></i> C 端无感交互</b>
        <p>体验查库容、Touch-and-Go 临时取物倒计时、VIP 柔性加急等。</p>
      </div>
      <div class="demo-step">
        <span>3</span><b><i class="fa-solid fa-chart-pie" style="color:var(--brand);"></i> B 端智能运营</b>
        <p>演示 Text-to-SQL 自动生成报表，动态调整计费策略与黑白名单。</p>
      </div>
      <div class="demo-step">
        <span>4</span><b><i class="fa-solid fa-cubes" style="color:var(--brand);"></i> 3D 数字孪生</b>
        <p>结合 3D 渲染库位，展示 AGV 微秒级坐标跟踪和车辆实时调度轨迹。</p>
      </div>
      <div class="demo-step">
        <span>5</span><b><i class="fa-solid fa-eye" style="color:var(--brand);"></i> 边缘 AI 介入</b>
        <p>展示 YOLOv8 车位检测与活体防入侵。点击急停，观察沙盘全屏红框闪烁！</p>
      </div>
      <div class="demo-step">
        <span>6</span><b><i class="fa-solid fa-rotate" style="color:var(--brand);"></i> 最终全栈演示</b>
        <p>点击“模拟入场”->“预调度”->“VIP 插队”->观察沙盘和队列的动态变化。</p>
      </div>
    </div>
  </article>
</template>
"""

# 3. AppSidebar.vue (ADD ICONS)
files["components/AppSidebar.vue"] = """<script setup>
defineProps({ mode: { type: String, default: "联调演示中" } });

const navItems = [
  ["dashboard", "/", "fa-solid fa-chart-line", "总览驾驶舱"],
  ["owner", "/owner", "fa-solid fa-mobile-screen-button", "车主端 H5"],
  ["admin", "/admin", "fa-solid fa-server", "运营后台"],
  ["twin", "/twin", "fa-solid fa-cubes", "数字孪生"],
  ["ai", "/ai", "fa-solid fa-eye", "AI 感知"],
  ["dispatch", "/dispatch", "fa-solid fa-network-wired", "调度中枢"],
  ["demo", "/demo", "fa-solid fa-clapperboard", "答辩脚本"],
];
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">PV</div>
      <div>
        <strong>ParkVision</strong>
        <span>CPS 智慧停车平台</span>
      </div>
    </div>
    <nav class="nav-list" aria-label="主导航">
      <RouterLink v-for="[name, path, icon, label] in navItems" :key="name" class="nav-item" :to="path">
        <span class="nav-icon"><i :class="icon"></i></span>
        <span>{{ label }}</span>
      </RouterLink>
    </nav>
    <div class="sidebar-panel">
      <span class="panel-label"><i class="fa-solid fa-circle-dot" style="color:var(--safety-green); margin-right:6px;"></i>演示状态</span>
      <strong>{{ mode }}</strong>
      <p>边缘侧端云协同模式，微服务集群在线。</p>
    </div>
  </aside>
</template>
"""

# 4. TopBar.vue (ICONS)
files["components/TopBar.vue"] = """<script setup>
import { onMounted, onUnmounted, ref } from "vue";
defineProps({ title: { type: String, required: true }, emergency: { type: Boolean, default: false } });
defineEmits(["entry", "pre-dispatch", "emergency"]);
const clock = ref("--:--:--");
let timer;
function tick() { clock.value = new Date().toLocaleTimeString("zh-CN", { hour12: false }); }
onMounted(() => { tick(); timer = window.setInterval(tick, 1000); });
onUnmounted(() => window.clearInterval(timer));
</script>

<template>
  <header class="topbar">
    <div>
      <p class="eyebrow"><i class="fa-solid fa-microchip"></i> AGV 自动立体车库 · 云边协同调度</p>
      <h1>{{ title }}</h1>
    </div>
    <div class="topbar-actions">
      <div class="time-chip" style="background: rgba(255,255,255,0.05); border: 1px solid var(--border-color); padding: 8px 16px; border-radius: 8px;">
        <span style="color:var(--text-muted); font-size:11px;">本地时间</span>
        <strong style="color:#fff; font-family:'Orbitron', sans-serif;">{{ clock }}</strong>
      </div>
      <button class="ghost-button" @click="$emit('entry')"><i class="fa-solid fa-car"></i> 模拟入场</button>
      <button class="primary-button" @click="$emit('pre-dispatch')"><i class="fa-solid fa-forward-fast"></i> 流量预调</button>
      <button class="danger-button" @click="$emit('emergency')">
        <i class="fa-solid" :class="emergency ? 'fa-lock-open' : 'fa-triangle-exclamation'"></i> {{ emergency ? "解除急停" : "全域急停" }}
      </button>
    </div>
  </header>
</template>
"""

# 5. MetricCard.vue
files["components/MetricCard.vue"] = """<script setup>
defineProps({ label: String, value: [String, Number], hint: String, tone: String });
</script>
<template>
  <div class="kpi-card" :class="{ alert: tone === 'alert' }">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <small><i class="fa-solid fa-chart-simple" style="margin-right:4px;"></i>{{ hint }}</small>
  </div>
</template>
"""

# Write all files
for rel_path, content in files.items():
    full_path = os.path.join(base_dir, rel_path.replace("/", "\\"))
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)

print("Vue Frontend Component & Bugfix Pass Completed Successfully.")
