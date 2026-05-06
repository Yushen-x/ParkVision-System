# ParkVision-System

ParkVision 智慧停车综合管理平台系统实现项目。

## 项目结构

```text
ParkVision-System/
  frontend/              Vue3 + Vite 前端工程
  backend/               Spring Boot 后端工程
  docs/                  架构、接口和答辩材料
  deploy/                Docker Compose 部署草案
  scripts/               本地辅助脚本
  parkvision-frontend/   旧版纯静态演示原型，保留作备用
```

## 推荐运行方式

一键启动：

```powershell
.\start.ps1
```

启动后请保持当前终端打开，前端开发服务器会在前台运行。

或双击：

```text
start.bat
```

`start.bat` 会打开一个新的前端服务窗口，并自动打开浏览器。如果本机没有 Maven，会自动跳过后端，只启动前端 mock 演示模式。

后台启动并写入日志：

```powershell
.\start.ps1 -Detached
```

停止服务：

```powershell
.\stop.ps1
```

手动启动后端：

```powershell
cd backend
mvn spring-boot:run
```

手动启动前端：

```powershell
cd frontend
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

## 当前完成度

- 前端：已升级为 Vue3 工程，包含总览驾驶舱、车主端 H5、运营后台、数字孪生、AI 感知、调度中枢、答辩脚本。
- 后端：已搭建 Spring Boot 分层架构，提供仪表盘、订单、车位、告警、调度、AI 感知演示接口。
- 文档：已完成系统实现路线、架构说明、接口清单、演示脚本和分工建议。
- 数据：当前为内存 mock 数据，后续可替换为 MySQL/Redis/RabbitMQ。

## 高分演示主线

建议答辩按这一条主线跑：

```text
模拟入场 -> AI 识别车牌 -> 生成订单 -> 更新车位与大屏 -> 高峰预测 -> 预调度 -> 车主取车/支付 -> 安全急停演示
```

这条线能同时覆盖 AI、业务流、调度算法、数字孪生和前端工程实现。
