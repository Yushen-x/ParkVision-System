<script setup>
import { computed, ref } from "vue";
import { getters, handleOwnerAction, state } from "../stores/parkingStore";

const duration = ref(156);
const fee = computed(() => (18 + Math.floor(duration.value / 30) * 2).toFixed(2));
const orderStatus = ref("存放中");
const freeCount = getters.freeCount;

const showOverlay = ref(false);
const timer = ref(180);
let timerId = null;

function doAction(action) {
  if (action === 'touch') {
    showOverlay.value = true;
    timer.value = 180;
    timerId = setInterval(() => {
      timer.value--;
      if(timer.value <= 0) {
        clearInterval(timerId);
      }
    }, 1000);
    orderStatus.value = "取物倒计时";
  } else {
    const statusMap = { reserve: "已预约", retrieve: "取车中", pay: "已支付" };
    orderStatus.value = statusMap[action];
  }
  handleOwnerAction(action);
}

function finishTouch() {
  showOverlay.value = false;
  if(timerId) clearInterval(timerId);
  orderStatus.value = "存放中";
}

const formatTime = (seconds) => {
  const m = Math.floor(seconds / 60).toString().padStart(2, '0');
  const s = (seconds % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
};
</script>

<template>
  <section class="mobile-workbench">
    <div class="phone-frame">
      <div class="phone-status">
        <span>ParkVision</span>
        <span><i class="fa-solid fa-wifi"></i> 5G <i class="fa-solid fa-battery-full"></i></span>
      </div>
      <div class="phone-screen">
        
        <div class="c-app-header">
            <div class="c-header-top">
                <span class="c-plate">浙A·12345</span>
                <i class="fa-regular fa-bell" style="font-size: 18px;"></i>
            </div>
            <h2>我的车辆</h2>
        </div>

        <div class="c-status-card">
            <div class="c-status-indicator">
                <div class="c-pulse-ring"></div>
                <div class="c-inner-circle">{{ orderStatus }}</div>
            </div>
            <div class="c-location"><i class="fa-solid fa-location-dot"></i> B2层 深库区 - A12位</div>
            <div class="c-info-grid">
                <div class="c-info-item"><span>停放时长</span><strong>02:36</strong></div>
                <div class="c-info-item"><span>当前电量</span><strong style="color: #10b981;">82% <i class="fa-solid fa-bolt"></i></strong></div>
            </div>
        </div>

        <div class="c-actions">
            <button class="c-btn c-btn-primary" @click="doAction('retrieve')">
                <i class="fa-solid fa-truck-ramp-box" style="font-size: 20px;"></i> 呼叫出库
            </button>
            <button class="c-btn c-btn-secondary" @click="doAction('touch')">
                <i class="fa-solid fa-box-open" style="font-size: 20px; color: #f59e0b;"></i> 临时取物
                <span class="c-badge">免扣费</span>
            </button>
        </div>

        <div class="c-vip-card" @click="doAction('retrieve')">
            <div class="c-vip-icon"><i class="fa-solid fa-bolt-lightning"></i></div>
            <div class="c-vip-text">
                <h4>VIP 加急取车</h4>
                <p>优先调度 AGV，预计省时 8 分钟</p>
            </div>
            <div class="c-vip-price">+¥5.00</div>
        </div>
        
        <!-- Timer Overlay -->
        <div v-if="showOverlay" style="position: absolute; top:0; left:0; width:100%; height:100%; background:rgba(15,23,42,0.85); backdrop-filter:blur(5px); z-index:50; display:flex; flex-direction:column; justify-content:center; align-items:center;">
            <div style="background: rgba(30, 41, 59, 0.95); width: 85%; border-radius: 20px; padding: 2.5rem 1.5rem; text-align: center; color: white; border: 1px solid rgba(255,255,255,0.1);">
                <h3 style="margin:0 0 10px; font-size:18px;">临时取物已到达</h3>
                <p style="color:#94a3b8; font-size:13px; margin:0;">请在倒计时结束前完成取物</p>
                <div style="font-size: 48px; font-weight:700; font-family:'Orbitron', sans-serif; color: #10b981; margin: 30px 0;">
                    {{ formatTime(timer) }}
                </div>
                <button class="primary-button full" @click="finishTouch">完成并入库</button>
            </div>
        </div>

      </div>
    </div>

    <div class="surface owner-detail" style="border:none; box-shadow:none; background:transparent;">
      <div class="section-head">
        <div>
          <h2 style="font-size:24px; margin-bottom:10px;">车主端核心高保真原型</h2>
          <p style="font-size:15px; max-width:500px;">展示融合了倒计时微动效的 Touch-and-Go 与加急取车功能，无缝契合 Vue 状态管理逻辑。</p>
        </div>
      </div>
      <div class="module-row" style="grid-template-columns: 1fr 1fr;">
        <div><b>实时车位与无感支付</b><span>打通底层数据，展示空闲数、充电位，自动生成订单，离场时扣费放行。</span></div>
        <div><b>Touch-and-Go 交互</b><span>临时取物倒计时，UI 状态实时切换，计费不中断，结束后自动回库。</span></div>
        <div><b style="color: #f59e0b;">VIP 柔性加急</b><span>高亮卡片诱导，点击即触发最高优先级 AGV 调度序列。</span></div>
        <div><b>AI 对话助手入口</b><span>预留自然语言转 API 的智能客服接口。</span></div>
      </div>
    </div>
  </section>
</template>
