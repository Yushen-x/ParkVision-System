# ParkVision API 接口清单

后端统一返回：

```json
{
  "success": true,
  "code": "OK",
  "message": "ok",
  "data": {}
}
```

## 总览与预测

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | `/api/dashboard/summary` | 获取首页 KPI |
| GET | `/api/forecast/traffic` | 获取车流历史与预测曲线 |

## 车位与订单

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | `/api/slots` | 获取车位状态 |
| GET | `/api/orders` | 获取原始订单列表 |
| POST | `/api/orders/entry` | 模拟入场并创建订单 |
| POST | `/api/orders/{orderNo}/retrieve` | 发起取车 |
| POST | `/api/orders/{orderNo}/touch-and-go` | 发起临时取物 |
| POST | `/api/orders/{orderNo}/pay` | 支付并关闭订单 |

## 管理后台

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | `/api/admin/orders` | 获取管理台订单行 |
| GET | `/api/admin/alerts` | 获取告警记录 |
| GET | `/api/admin/pricing-rules` | 获取计费规则 |
| GET | `/api/admin/access-list` | 获取黑白名单与用户类型 |
| POST | `/api/admin/report` | 生成运营报表摘要与图表数据 |

## 调度与 AGV

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | `/api/dispatch/queue` | 获取调度队列 |
| GET | `/api/dispatch/agvs` | 获取 AGV 遥测状态 |
| POST | `/api/dispatch/pre-dispatch` | 触发提前移库 |
| POST | `/api/dispatch/vip?orderNo=...` | 插入 VIP 加急任务 |

## AI 感知

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| POST | `/api/edge/vision/infer` | 模拟边缘视觉识别并写入设备状态 |

请求示例：

```json
{
  "cameraId": "CAM-SOUTH-01",
  "imageUrl": "demo://snapshot.jpg",
  "simulateIntrusion": false
}
```

## 设备与安全

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | `/api/devices/overview` | 获取摄像头、闸机、充电桩和设备事件总览 |
| POST | `/api/devices/emergency?active=true` | 触发或解除急停 |
| GET | `/api/system/nodes` | 获取系统节点健康状态 |

## 体验页专用

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | `/api/pricing/preview?orderNo=...` | 获取实时计费预估 |
| GET | `/api/navigation/indoor?orderNo=...` | 获取室内接驳导航快照 |
