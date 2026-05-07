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

## 快速启动

双击根目录的 `start.bat`，或在 PowerShell 中运行：

```powershell
.\start.ps1
```

启动后访问：

```text
http://localhost:5173
```

如果本机没有 Maven，启动脚本会跳过后端，只启动前端 fallback 演示模式。

停止服务：

```powershell
.\stop.ps1
```

## 当前完成度

- 前端：Vue3 工程，包含总览驾驶舱、车主端 H5、运营后台、数字孪生、AI 感知、调度中心、动态定价、系统配置等页面。
- 后端：Spring Boot 标准分层结构，包含 `controller`、`service`、`repository`、`domain`、`dto`、`common`、`config`。
- 数据：当前使用 fallback 数据源，后续可替换为 MySQL、Redis、RabbitMQ。
- AI：当前完成 AI 感知展示、结构化 JSON、视觉接口、预测调度与业务联动演示。

## 答辩演示主线

```text
模拟入场 -> AI 识别车牌 -> 生成订单 -> 更新车位与大屏
-> 高峰预测 -> 预调度 -> 车主取车/支付 -> 安全急停演示
```
