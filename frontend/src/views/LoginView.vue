<script setup>
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { login } from "../stores/parkingStore";

const router = useRouter();
const pageRoute = useRoute();

const username = ref("");
const password = ref("");
const role = ref("admin");
const busy = ref(false);
const error = ref("");

async function submit() {
  if (!username.value.trim() || !password.value.trim()) {
    error.value = "请输入账号和密码";
    return;
  }
  error.value = "";
  busy.value = true;
  await new Promise((resolve) => setTimeout(resolve, 450));
  const user = login({ username: username.value.trim(), role: role.value });
  busy.value = false;
  const redirect = typeof pageRoute.query.redirect === "string" ? pageRoute.query.redirect : "";
  router.replace(redirect || (user.role === "owner" ? "/owner" : "/"));
}
</script>

<template>
  <div class="login-shell">
    <div class="login-aside">
      <div class="login-brand">
        <div class="brand-mark">PV</div>
        <div>
          <strong>ParkVision</strong>
          <span>智能停车履约演示系统</span>
        </div>
      </div>
      <div class="login-pitch">
        <h1>立体车库 · 数字孪生 · AI 履约一体化</h1>
        <p>从车牌识别、回转塔库搬运、动态计费到车主取车，完整业务闭环在一个系统里演示。</p>
        <ul>
          <li><i class="fa-solid fa-circle-check"></i> 三环回转塔库数字孪生实时联动</li>
          <li><i class="fa-solid fa-circle-check"></i> 车牌识别 / 车主助手可接入大模型</li>
          <li><i class="fa-solid fa-circle-check"></i> 预约锁位 → 到场 → 计费的闭环演示</li>
        </ul>
      </div>
      <span class="login-version">PV-CPS v1.2.0</span>
    </div>

    <div class="login-main">
      <form class="login-card" @submit.prevent="submit">
        <h2>欢迎登录</h2>
        <p class="login-sub">请选择登录身份并输入账号密码。</p>

        <div class="role-toggle">
          <button type="button" :class="{ active: role === 'admin' }" @click="role = 'admin'">
            <i class="fa-solid fa-user-shield"></i> 管理端
          </button>
          <button type="button" :class="{ active: role === 'owner' }" @click="role = 'owner'">
            <i class="fa-solid fa-user"></i> 车主端
          </button>
        </div>

        <label class="login-field">
          <span>账号</span>
          <div class="login-input"><i class="fa-solid fa-user"></i><input v-model="username" type="text" autocomplete="username" :placeholder="role === 'admin' ? '管理员账号' : '手机号 / 车牌'" /></div>
        </label>

        <label class="login-field">
          <span>密码</span>
          <div class="login-input"><i class="fa-solid fa-lock"></i><input v-model="password" type="password" autocomplete="current-password" placeholder="输入密码" /></div>
        </label>

        <p v-if="error" class="login-error">{{ error }}</p>

        <button class="primary-button login-submit" type="submit" :disabled="busy">
          <i class="fa-solid" :class="busy ? 'fa-spinner fa-spin' : 'fa-arrow-right-to-bracket'"></i>
          {{ busy ? "登录中..." : "登录" }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-shell {
  position: fixed;
  inset: 0;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  background: #eef2ff;
}

.login-aside {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 56px;
  color: #fff;
  background: radial-gradient(circle at 20% 20%, rgba(99, 102, 241, 0.55), transparent 42%),
    linear-gradient(135deg, #4f46e5, #312e81);
  overflow: hidden;
}

.login-aside::after {
  content: "";
  position: absolute;
  right: -120px;
  bottom: -120px;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  z-index: 1;
}

.login-brand .brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-family: "Outfit", sans-serif;
  font-weight: 800;
  font-size: 20px;
  color: #4f46e5;
  background: #fff;
}

.login-brand strong {
  display: block;
  font-size: 20px;
  font-family: "Outfit", sans-serif;
}

.login-brand span {
  font-size: 13px;
  opacity: 0.78;
}

.login-pitch {
  z-index: 1;
}

.login-pitch h1 {
  font-size: 30px;
  line-height: 1.35;
  margin: 0 0 14px;
}

.login-pitch p {
  font-size: 15px;
  line-height: 1.7;
  opacity: 0.85;
  margin: 0 0 22px;
  max-width: 420px;
}

.login-pitch ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 12px;
}

.login-pitch li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  opacity: 0.92;
}

.login-pitch li i {
  color: #a5f3cf;
}

.login-version {
  z-index: 1;
  font-size: 12px;
  opacity: 0.6;
}

.login-main {
  display: grid;
  place-items: center;
  padding: 32px;
}

.login-card {
  width: 100%;
  max-width: 380px;
  padding: 36px 32px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid var(--border-color);
  box-shadow: 0 30px 60px -40px rgba(15, 23, 42, 0.5);
}

.login-card h2 {
  margin: 0;
  font-size: 24px;
  color: var(--text-main);
}

.login-sub {
  margin: 8px 0 22px;
  color: var(--text-muted);
  font-size: 13px;
}

.role-toggle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 20px;
  padding: 4px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.04);
}

.role-toggle button {
  min-height: 38px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.role-toggle button.active {
  background: #fff;
  color: var(--brand);
  box-shadow: 0 6px 16px -10px rgba(79, 70, 229, 0.6);
}

.login-field {
  display: block;
  margin-bottom: 16px;
}

.login-field > span {
  display: block;
  margin-bottom: 7px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.login-input {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  padding: 0 14px;
  border-radius: 11px;
  border: 1px solid var(--border-color);
  background: #fff;
  transition: border-color 0.2s ease;
}

.login-input:focus-within {
  border-color: var(--brand);
}

.login-input i {
  color: var(--text-muted);
}

.login-input input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--text-main);
}

.login-error {
  margin: 0 0 14px;
  color: var(--danger-red);
  font-size: 13px;
}

.login-submit {
  width: 100%;
  min-height: 46px;
  font-size: 15px;
  margin-top: 4px;
}

.login-quick {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.login-quick span {
  color: var(--text-muted);
  font-size: 12px;
}

.login-quick button {
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 920px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-aside {
    display: none;
  }
}
</style>
