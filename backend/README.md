# ParkVision Backend

ParkVision 后端采用 Spring Boot 分层架构，当前实现阶段以“真实后端结构 + fallback 数据源”方式支撑前端演示。

## 技术栈

- Java 17
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

## 分层结构

```text
com.parkvision.cps
  common
    ApiResponse              统一响应结构
    BusinessException        业务异常
    GlobalExceptionHandler   全局异常处理
  dashboard                  总览与车流预测
  parking                    车位状态与库容 DTO
  order                      入场、取车、支付状态机
  dispatch                   AGV 队列与预调度策略
  vision                     边缘 AI 推理接口
  admin                      运营后台、告警、计费规则
  infrastructure.repository
    ParkVisionRepository          数据访问接口
    FallbackParkVisionRepository  fallback 演示数据源
```

## 数据源策略

当前 `application.yml` 使用：

```yaml
parkvision:
  persistence:
    mode: fallback
```

也就是说，Service 层只依赖 `ParkVisionRepository` 接口。如果后续接入 MySQL、Redis 或 MQ，只需要新增对应 Repository 实现；演示阶段则由 `FallbackParkVisionRepository` 提供可回退的业务数据，保证前端页面不会因为后端环境不足而失效。

## 截图建议

- `infrastructure/repository/ParkVisionRepository.java`：展示真实数据访问抽象。
- `infrastructure/repository/FallbackParkVisionRepository.java`：展示 fallback 数据源与种子数据。
- `common/GlobalExceptionHandler.java`：展示统一异常处理。
- `order/OrderService.java`：展示入场、泊位分配、订单状态流转。
- `dispatch/DispatchService.java`：展示 Pre-Dispatch 与 VIP 队列策略。
