# ParkVision Backend

The backend is a Spring Boot 3 application built around a simple layered structure:

```text
controller -> service -> repository
```

The default runtime path is now a JDBC repository backed by a persistent H2 file database. A fallback repository implementation is still available so the demo can run without external infrastructure, and the datasource can later be pointed at MySQL by overriding environment variables.

## Tech stack

- Java 17 target
- Spring Boot 3
- Spring Web
- Spring JDBC
- Spring Validation
- Spring Actuator
- H2 file database
- Maven Wrapper

## Main endpoints

- `GET /api/dashboard/summary`
- `GET /api/forecast/traffic`
- `GET /api/slots`
- `GET /api/orders`
- `POST /api/orders/entry`
- `POST /api/orders/{orderNo}/retrieve`
- `POST /api/orders/{orderNo}/touch-and-go`
- `POST /api/orders/{orderNo}/pay`
- `GET /api/dispatch/queue`
- `GET /api/dispatch/agvs`
- `POST /api/dispatch/pre-dispatch`
- `POST /api/dispatch/vip`
- `GET /api/admin/orders`
- `GET /api/admin/alerts`
- `GET /api/admin/pricing-rules`
- `GET /api/admin/access-list`
- `POST /api/admin/report`
- `GET /api/system/nodes`
- `POST /api/edge/vision/infer`
- `GET /api/devices/overview`
- `POST /api/devices/emergency`
- `GET /api/pricing/preview`
- `GET /api/navigation/indoor`

## Run locally

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Default address:

```text
http://localhost:8080
```

Health check:

```text
GET http://localhost:8080/actuator/health
```

Default database:

```text
jdbc:h2:file:./data/parkvision
```
