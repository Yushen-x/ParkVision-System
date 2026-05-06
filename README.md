# ParkVision-System

ParkVision 智慧停车综合管理平台系统实现项目。

## 项目结构

```text
ParkVision-System/
  frontend/     Vue3 + Vite 前端工程
  backend/      Spring Boot 后端工程
  docs/         架构说明、接口清单、答辩材料
  deploy/       Docker Compose 部署草案
  scripts/      本地启动与辅助脚本
  start.bat     Windows 一键启动入口
  start.ps1     PowerShell 一键启动入口
  stop.bat      Windows 停止服务入口
  stop.ps1      PowerShell 停止服务入口
```

说明：项目只保留一个正式前端工程 `frontend/`。旧版纯静态原型已经删除，避免和 Vue 前端混淆。

## 快速启动

双击根目录的 `start.bat`，或在 PowerShell 中运行：

```powershell
.\start.ps1
```

启动后访问：

```text
http://localhost:5173
```

如果本机没有 Maven，启动脚本会跳过后端，只启动前端 mock 演示模式。

停止服务：

```powershell
.\stop.ps1
```

## 手动启动

后端：

```powershell
cd backend
mvn spring-boot:run
```

前端：

```powershell
cd frontend
npm install
npm run dev
```

## 当前完成度

- 前端：已升级为 Vue3 工程，包含总览驾驶舱、车主端 H5、运营后台、数字孪生、AI 感知、调度中心、动态定价、系统配置等页面。
- 后端：已搭建 Spring Boot 分层架构，提供仪表盘、订单、车位、告警、调度、AI 感知等演示接口。
- 文档：已完成系统实现路线、架构说明、接口清单和答辩材料建议。
- 数据：当前使用 mock 数据，后续可替换为 MySQL、Redis、RabbitMQ 等真实基础设施。

## 答辩演示主线

```text
模拟入场 -> AI 识别车牌 -> 生成订单 -> 更新车位与大屏
-> 高峰预测 -> 预调度 -> 车主取车/支付 -> 安全急停演示
```

这条主线可以同时覆盖 AI 感知、业务流程、调度算法、数字孪生和前端工程实现。
