# ParkVision Frontend

Vue3 + Vite 前端工程，负责车主端、运营后台、数字孪生大屏、AI 感知和调度中枢展示。

## 技术栈

- Vue 3
- Vue Router
- Vite
- Fetch API
- Canvas 图表

## 运行

```powershell
npm install
npm run dev
```

默认地址：

```text
http://localhost:5173
```

如果后端 `http://localhost:8080` 已启动，Vite 会把 `/api` 请求代理到后端；如果后端不可用，前端 API 层会自动使用 mock 数据。

## 目录说明

```text
src/
  api/          后端接口适配与 mock fallback
  assets/       全局样式
  components/   通用组件
  data/         演示数据
  router/       页面路由
  stores/       演示状态与业务动作
  views/        业务页面
```
