# ParkVision 数据库核心关系图

这张图用于系统实现答辩中的“后端微服务实现 / 数据设计”页面。

推荐插入 PPT 的文件：

```text
docs/architecture/parkvision-er-diagram.svg
```

图中核心表：

- `c_user`：车主用户表
- `c_vehicle`：车辆资产表
- `parking_order`：停车订单表
- `parking_slot`：物理库位表
- `agv_unit`：AGV 设备表
- `dispatch_task`：调度任务表
- `alert_event`：AI 告警事件表
- `pricing_rule`：计费规则表

讲解主线：

```text
车主绑定车辆
  -> AI 识别入场
  -> 创建停车订单
  -> 分配物理库位
  -> 生成 AGV 调度任务
  -> AI 告警与计费规则影响后续取车、支付和安全控制
```
