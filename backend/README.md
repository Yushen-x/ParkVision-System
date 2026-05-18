# ParkVision Backend

The backend is a Spring Boot 3 application built around a simple layered structure:

```text
controller -> service -> repository
```

It ships with a fallback repository implementation so the demo can run without external infrastructure, but the service layer is already structured so the repository can be replaced later by MySQL, Redis, or other integrations.

## Tech stack

- Java 17 target
- Spring Boot 3
- Spring Web
- Spring Validation
- Spring Actuator
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
