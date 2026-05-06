# ParkVision Backend

Spring Boot 后端工程，当前是“系统实现阶段”的轻量演示版：业务数据使用内存仓库，重点展示后端架构、接口分层和前后端联动方式。

## 技术栈

- Java 17+
- Spring Boot 3
- Spring Web
- Spring Validation
- Spring Actuator

## 运行

本机需要 Maven。进入 `backend` 后运行：

```powershell
mvn spring-boot:run
```

服务地址：

```text
http://localhost:8080
```

健康检查：

```text
GET http://localhost:8080/actuator/health
```

## 分层说明

```text
com.parkvision.cps
  common          通用响应结构
  config          跨域等配置
  dashboard       总览与车流预测
  parking         车位与库区状态
  order           订单状态机
  admin           后台管理、告警、计费规则
  dispatch        AGV 与调度队列
  vision          边缘 AI 识别接口
  infrastructure  内存数据仓库，后续替换为 MySQL/Redis/MQ
```

## 后续升级方向

- `InMemoryDataStore` 替换为 MySQL Repository。
- 车位状态、AGV 坐标放入 Redis 缓存。
- 调度任务写入 RabbitMQ，支持异步执行和死信队列。
- AI 识别改为调用 Python FastAPI 服务。
- 增加 JWT 登录鉴权、角色权限与操作审计。
