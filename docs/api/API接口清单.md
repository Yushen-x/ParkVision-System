# ParkVision API 接口清单

后端统一返回：

```json
{
  "success": true,
  "message": "ok",
  "data": {}
}
```

## 总览与预测

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | `/api/dashboard/summary` | 获取首页 KPI |
| GET | `/api/forecast/traffic` | 获取车流历史与预测曲线 |

## 车位与库区

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | `/api/slots` | 获取车位状态 |

## 订单

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| POST | `/api/orders/entry` | 模拟 AI 识别入场并创建订单 |
| POST | `/api/orders/{orderNo}/retrieve` | 发起取车 |
| POST | `/api/orders/{orderNo}/touch-and-go` | 临时取物 |
| POST | `/api/orders/{orderNo}/pay` | 支付离场 |

## 管理后台

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | `/api/admin/orders` | 查询出入场记录 |
| GET | `/api/admin/alerts` | 查询异常告警 |
| GET | `/api/admin/pricing-rules` | 查询计费规则 |

## 调度

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | `/api/dispatch/queue` | 查询调度队列 |
| GET | `/api/dispatch/agvs` | 查询 AGV 状态 |
| POST | `/api/dispatch/pre-dispatch` | 触发提前移库 |
| POST | `/api/dispatch/vip` | 插入 VIP 加急任务 |

## AI 感知

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| POST | `/api/edge/vision/infer` | 模拟边缘视觉识别 |

请求示例：

```json
{
  "cameraId": "gate-A-01",
  "imageUrl": "demo://snapshot.jpg",
  "simulateIntrusion": false
}
```

响应示例：

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "requestId": "edge-123456",
    "cameraId": "gate-A-01",
    "plate": "沪A·P7686",
    "confidence": 0.982,
    "intrusion": false,
    "action": "ALLOW_ENTRY_AND_CREATE_ORDER"
  }
}
```
