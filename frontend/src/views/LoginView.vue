<script setup>
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { login, register } from "../stores/parkingStore";

const router = useRouter();
const pageRoute = useRoute();

const mode = ref("login");
const username = ref("");
const password = ref("");
const busy = ref(false);
const error = ref("");

// Registration-only fields (a new account is always an owner).
const displayName = ref("");
const phone = ref("");
const plateNo = ref("");
const energyType = ref("FUEL");

const accountHints = [
  { role: "管理端", username: "admin", password: "admin123", icon: "fa-user-shield" },
  { role: "车主端", username: "owner", password: "owner123", icon: "fa-user" },
];

function fillAccount(hint) {
  username.value = hint.username;
  password.value = hint.password;
  error.value = "";
}

function switchMode(next) {
  mode.value = next;
  error.value = "";
}

function goAfterAuth(user) {
  const redirect = typeof pageRoute.query.redirect === "string" ? pageRoute.query.redirect : "";
  router.replace(redirect || (user.role === "owner" ? "/owner" : "/"));
}

async function submit() {
  if (mode.value === "register") {
    return submitRegister();
  }
  if (!username.value.trim() || !password.value.trim()) {
    error.value = "请输入账号和密码";
    return;
  }
  error.value = "";
  busy.value = true;
  const result = await login({ username: username.value.trim(), password: password.value });
  busy.value = false;
  if (!result.ok) {
    error.value = result.error;
    return;
  }
  goAfterAuth(result.user);
}

async function submitRegister() {
  if (!username.value.trim() || !password.value || !displayName.value.trim() || !plateNo.value.trim()) {
    error.value = "请填写账号、密码、姓名和车牌";
    return;
  }
  if (password.value.length < 6) {
    error.value = "密码至少 6 位";
    return;
  }
  error.value = "";
  busy.value = true;
  const result = await register({
    username: username.value.trim(),
    password: password.value,
    displayName: displayName.value.trim(),
    phone: phone.value.trim(),
    plateNo: plateNo.value.trim(),
    energyType: energyType.value,
  });
  busy.value = false;
  if (!result.ok) {
    error.value = result.error;
    return;
  }
  goAfterAuth(result.user);
}
</script>

<template>
  <div class="login-shell">
    <div class="login-aside">
      <div class="login-brand">
        <div class="brand-mark">PV</div>
        <div>
          <strong>ParkVision</strong>
          <span>智能停车履约管理系统</span>
        </div>
      </div>
      <div class="login-pitch">
        <h1>立体车库 · 数字孪生 · AI 履约一体化</h1>
        <p>从车牌识别、回转塔库搬运、动态计费到车主取车，构建完整的智能停车业务闭环。</p>
        <ul>
          <li><i class="fa-solid fa-circle-check"></i> 三环回转塔库数字孪生实时联动</li>
          <li><i class="fa-solid fa-circle-check"></i> 车牌识别 / 车主智能助手一体化</li>
          <li><i class="fa-solid fa-circle-check"></i> 预约锁位 → 到场 → 计费的业务闭环</li>
        </ul>
      </div>
      <span class="login-version">PV-CPS v1.2.0</span>
    </div>

    <div class="login-main">
      <form class="login-card" @submit.prevent="submit">
        <div class="login-tabs">
          <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
          <button type="button" :class="{ active: mode === 'register' }" @click="switchMode('register')">车主注册</button>
        </div>
        <p class="login-sub">
          {{ mode === 'login' ? '请输入账号和密码，系统会按账号身份进入对应工作台。' : '注册即创建车主账号，并绑定一辆车牌，注册成功后自动登录。' }}
        </p>

        <label class="login-field">
          <span>账号</span>
          <div class="login-input"><i class="fa-solid fa-user"></i><input v-model="username" type="text" autocomplete="username" placeholder="请输入账号" /></div>
        </label>

        <label class="login-field">
          <span>密码</span>
          <div class="login-input"><i class="fa-solid fa-lock"></i><input v-model="password" type="password" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" :placeholder="mode === 'login' ? '请输入密码' : '设置密码（至少 6 位）'" /></div>
        </label>

        <template v-if="mode === 'register'">
          <label class="login-field">
            <span>姓名</span>
            <div class="login-input"><i class="fa-solid fa-id-card"></i><input v-model="displayName" type="text" placeholder="请输入姓名" /></div>
          </label>
          <label class="login-field">
            <span>手机号（选填）</span>
            <div class="login-input"><i class="fa-solid fa-phone"></i><input v-model="phone" type="tel" placeholder="用于展示脱敏号码" /></div>
          </label>
          <label class="login-field">
            <span>车牌号</span>
            <div class="login-input"><i class="fa-solid fa-car"></i><input v-model="plateNo" type="text" placeholder="如 沪A·12345 / SH-A1234" /></div>
          </label>
          <label class="login-field">
            <span>能源类型</span>
            <div class="login-input">
              <i class="fa-solid fa-bolt"></i>
              <select v-model="energyType" class="login-select">
                <option value="FUEL">燃油</option>
                <option value="EV">新能源</option>
              </select>
            </div>
          </label>
        </template>

        <p v-if="error" class="login-error"><i class="fa-solid fa-circle-exclamation"></i> {{ error }}</p>

        <button class="primary-button login-submit" type="submit" :disabled="busy">
          <i class="fa-solid" :class="busy ? 'fa-spinner fa-spin' : 'fa-arrow-right-to-bracket'"></i>
          {{ busy ? (mode === 'login' ? '登录中...' : '注册中...') : (mode === 'login' ? '登录' : '注册并登录') }}
        </button>

        <div v-if="mode === 'login'" class="login-accounts">
          <span class="login-accounts-title">可用账号</span>
          <button v-for="hint in accountHints" :key="hint.username" type="button" class="login-account" @click="fillAccount(hint)">
            <i class="fa-solid" :class="hint.icon"></i>
            <div>
              <b>{{ hint.role }}</b>
              <small>{{ hint.username }} / {{ hint.password }}</small>
            </div>
            <span class="login-account-fill">填入</span>
          </button>
        </div>
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

.login-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.login-tabs button {
  flex: 1;
  padding: 9px 0;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.login-tabs button.active {
  border-color: var(--brand);
  color: var(--brand);
  background: rgba(79, 70, 229, 0.06);
}

.login-select {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--text-main);
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

.login-error i {
  margin-right: 6px;
}

.login-accounts {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px dashed var(--border-color);
  display: grid;
  gap: 10px;
}

.login-accounts-title {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.login-account {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 11px;
  border: 1px solid var(--border-color);
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.login-account:hover {
  border-color: var(--brand);
  background: rgba(79, 70, 229, 0.04);
}

.login-account > i {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 9px;
  color: var(--brand);
  background: rgba(79, 70, 229, 0.1);
}

.login-account > div {
  flex: 1;
  min-width: 0;
}

.login-account b {
  display: block;
  color: var(--text-main);
  font-size: 13px;
}

.login-account small {
  color: var(--text-muted);
  font-size: 12px;
}

.login-account-fill {
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
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
