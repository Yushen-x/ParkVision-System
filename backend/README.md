# ParkVision Backend

ParkVision 后端采用 Spring Boot 3 + Java 17，实现标准 MVC 分层结构。当前阶段使用 fallback 数据源保证答辩演示稳定，后续可平滑替换为 MySQL、Redis、RabbitMQ 等真实基础设施。

## 技术栈

- Java 17
- Spring Boot 3
- Spring Web
- Spring Validation
- Spring Actuator

## 标准分层结构

```text
com.parkvision.cps
  controller/     REST API 接口层
  service/        业务逻辑层
  repository/     数据访问抽象与 fallback 实现
  domain/         领域对象与状态枚举
  dto/            前后端传输对象
  common/         统一响应、业务异常、全局异常处理
  config/         CORS 等应用配置
```

## 核心设计

后端 Service 层只依赖 `ParkVisionRepository` 接口，不直接依赖具体数据来源。

```text
Controller -> Service -> ParkVisionRepository -> FallbackParkVisionRepository
```

当前 `application.yml` 配置：

```yaml
parkvision:
  persistence:
    mode: fallback
```

这表示系统在演示阶段使用 `FallbackParkVisionRepository` 提供车位、订单、AGV、告警和计费规则等数据。后续接入数据库时，只需要新增数据库版 Repository 实现，Controller 和 Service 层无需改动。

## 运行

进入 `backend` 目录：

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

## PPT 截图建议

- `controller/OrderController.java`：展示 RESTful API 接口。
- `service/OrderService.java`：展示订单状态流转和泊位分配逻辑。
- `service/DispatchService.java`：展示 Pre-Dispatch 与 VIP 调度策略。
- `repository/ParkVisionRepository.java`：展示数据访问抽象。
- `repository/FallbackParkVisionRepository.java`：展示 fallback 数据源。
- `common/GlobalExceptionHandler.java`：展示统一异常处理。
