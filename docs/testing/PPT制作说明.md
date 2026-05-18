# ParkVision 系统测试汇报 PPT 制作说明

参考文件：`docs/文档/果团团系统测试.pptx`  
建议页数：28 页左右  
建议风格：沿用参考 PPT 的“目录页 + 分章节页 + 表格/截图/流程图”结构，但内容换成 ParkVision 的真实系统测试材料。

## 素材总览

所有图片素材已经放在 `docs/testing/assets` 下：

| 素材 | 路径 | 用途 |
|---|---|---|
| 测试结果总览 | `docs/testing/assets/figures/test-results-overview.svg` | 总结页、结论页 |
| 测试流程 | `docs/testing/assets/figures/test-process.svg` | 测试方法页 |
| 数据规模 | `docs/testing/assets/figures/dataset-overview.svg` | 测试数据页 |
| 性能图 | `docs/testing/assets/figures/performance-benchmark.svg` | 性能测试页 |
| 订单白盒流程 | `docs/testing/assets/figures/order-service-flow.svg` | 白盒测试页 |
| 急停白盒流程 | `docs/testing/assets/figures/emergency-control-flow.svg` | 白盒测试页 |
| 首页截图 | `docs/testing/assets/screenshots/dashboard.png` | 系统总览、功能测试 |
| 数字孪生截图 | `docs/testing/assets/screenshots/twin.png` | 数字孪生、AGV |
| 急停截图 | `docs/testing/assets/screenshots/twin-emergency.png` | 安全联动 |
| AI 页面截图 | `docs/testing/assets/screenshots/ai-vision.png` | AI 感知 |
| 调度页面截图 | `docs/testing/assets/screenshots/dispatch.png` | 调度中心 |
| 管理报表截图 | `docs/testing/assets/screenshots/admin-report.png` | 管理台报表 |
| 订单表截图 | `docs/testing/assets/screenshots/admin-orders.png` | 管理台订单 |
| 计费截图 | `docs/testing/assets/screenshots/pricing.png` | 动态计费 |
| 系统页截图 | `docs/testing/assets/screenshots/system.png` | 设备与节点 |
| 车主端截图 | `docs/testing/assets/screenshots/owner.png` | C 端流程 |
| 室内导航截图 | `docs/testing/assets/screenshots/indoor-map.png` | 室内接驳 |

## 第 1 页：封面

标题：ParkVision 智能停车系统测试汇报  
副标题：系统测试、功能验证、黑盒/白盒测试与非功能测试  
展示信息：小组名称、汇报人、日期、课程名称。  
建议图片：可用 `docs/testing/assets/screenshots/dashboard.png` 做右侧或底部大图，裁剪出 KPI 和交通预测区域。

## 第 2 页：目录

目录建议按参考 PPT 的结构：

1. 需求测试
2. 功能性测试
3. 静态测试
4. 黑盒测试
5. 白盒测试
6. 非功能性测试
7. 可用性测试
8. 测试结论

建议不放大图，保持干净。

## 第 3 页：系统测试目标

核心内容：

| 测试目标 | 说明 |
|---|---|
| 验证真实数据 | 前端页面优先读取后端 API 和数据库 |
| 验证业务闭环 | AI、订单、调度、AGV、计费、导航、管理台联动 |
| 验证异常处理 | 非法订单、缺参请求、急停恢复 |
| 验证性能 | 关键接口并发请求成功率和响应时间 |

建议图片：`docs/testing/assets/figures/test-process.svg`

## 第 4 页：测试环境

做成表格：

| 项目 | 配置 |
|---|---|
| 前端 | Vue 3 + Vite，端口 5173 |
| 后端 | Spring Boot 3.4.1，端口 8080 |
| 数据库 | H2 文件数据库 |
| 设备 | 后端模拟 ONVIF、Modbus/TCP、OCPP 风格数据 |
| 截图 | Playwright headless Chromium |

页脚可写：仓库拉取后无需手工建库，首次启动自动生成本地数据库。

## 第 5 页：测试数据规模

放 `docs/testing/assets/figures/dataset-overview.svg`。

补充 4 个口头点：

1. 72 个车位。
2. 27 条订单。
3. 16 条调度任务。
4. 摄像头、闸机、充电桩、AGV 都由后端统一提供状态。

## 第 6 页：需求测试 - 功能性需求

做成两列表格：

| 需求 | 验证结果 |
|---|---|
| 运营首页展示 KPI、车位、预测 | 通过 |
| 车辆入场创建订单并占用车位 | 通过 |
| 取车、临时取物、支付关闭订单 | 通过 |
| 预调度和 VIP 插队 | 通过 |
| AI 识别和入侵急停 | 通过 |
| 动态计费和室内导航 | 通过 |
| 管理台订单、告警、报表 | 通过 |

## 第 7 页：需求测试 - 非功能性需求

做成表格或 6 个小卡片：

| 非功能需求 | 验证方式 |
|---|---|
| 可启动性 | `start.ps1` 启动前后端 |
| 可持久化 | H2 文件数据库 |
| 性能 | 并发接口测试 |
| 容错性 | 黑盒异常用例 |
| 可用性 | 真实页面截图 |
| 可观测性 | 节点、设备、事件、告警 |

## 第 8 页：功能测试总览

放 `docs/testing/assets/figures/test-results-overview.svg`。

重点文字：

功能测试 `12/12` 通过，黑盒异常测试 `5/5` 通过，性能测试关键场景成功率 `100%`。

## 第 9 页：功能测试 - 运营首页

放图：`docs/testing/assets/screenshots/dashboard.png`

讲解点：

1. KPI、车流量、AGV 在线数、告警数来自后端。
2. 交通预测用于预调度决策。
3. 车位概览和事件流与其他页面共享同一份状态。

## 第 10 页：功能测试 - AI 感知

放图：`docs/testing/assets/screenshots/ai-vision.png`

讲解点：

1. “采集画面”调用 `/api/edge/vision/infer`。
2. 返回车牌、置信度、摄像头 ID、动作建议。
3. 入侵检测会联动设备急停。

## 第 11 页：功能测试 - 调度中心

放图：`docs/testing/assets/screenshots/dispatch.png`

讲解点：

1. 调度队列由 `/api/dispatch/queue` 提供。
2. 支持预调度、VIP 插队、临停取物、先到先取。
3. 页面同时展示闸机和充电桩释放条件。

## 第 12 页：功能测试 - 数字孪生

放图：`docs/testing/assets/screenshots/twin.png`

讲解点：

1. 车位状态来自后端车位接口。
2. AGV 坐标、电量、速度、模式来自调度遥测。
3. 数字孪生与运营首页、调度中心共享状态。

## 第 13 页：功能测试 - 急停联动

放图：`docs/testing/assets/screenshots/twin-emergency.png`

讲解点：

1. AI 入侵识别后，系统进入“紧急停车”。
2. 闸机急停状态拉起，节点进入预警。
3. 人工解除急停后设备恢复自动模式。

## 第 14 页：功能测试 - 车主端

放图：`docs/testing/assets/screenshots/owner.png`

讲解点：

1. 当前订单来自后端订单列表。
2. “取车”按钮调用取车接口。
3. “临停取物”按钮调用临时取物接口。
4. “VIP 优先取车”按钮调用 VIP 插队接口。
5. “标记已支付并关闭订单”按钮调用支付关闭订单接口。

## 第 15 页：功能测试 - 动态计费

放图：`docs/testing/assets/screenshots/pricing.png`

讲解点：

1. 计费结果由 `/api/pricing/preview` 返回。
2. 费用组成包括基础停车、峰值调节、EV 充电、VIP 取车。
3. 规则源与管理台计费规则一致。

## 第 16 页：功能测试 - 室内导航

放图：`docs/testing/assets/screenshots/indoor-map.png`

讲解点：

1. 导航快照由 `/api/navigation/indoor` 返回。
2. 展示订单、车牌、车位、目标闸机、剩余距离。
3. 车主 ETA 和 AGV ETA 与实时状态有关。

## 第 17 页：功能测试 - 管理台

放两张图，左右排版：

左图：`docs/testing/assets/screenshots/admin-report.png`  
右图：`docs/testing/assets/screenshots/admin-orders.png`

讲解点：

1. 报表由 `/api/admin/report` 生成。
2. 订单表来自 `/api/admin/orders`。
3. 告警、计费规则、名单数据均由后端接口提供。

## 第 18 页：功能测试 - 系统网关与设备

放图：`docs/testing/assets/screenshots/system.png`

讲解点：

1. 展示摄像头、闸机、充电桩状态。
2. 摄像头字段接近 ONVIF 设备。
3. 闸机字段接近 Modbus/TCP。
4. 充电桩字段接近 OCPP。

## 第 19 页：静态测试

分前端和后端两栏：

前端：

| 检查项 | 结果 |
|---|---|
| 9 个路由完整 | 通过 |
| Store 优先调用后端 API | 通过 |
| 构建和测试脚本存在 | 通过 |

后端：

| 检查项 | 结果 |
|---|---|
| 控制器/服务层/数据访问层分层 | 通过 |
| JDBC 数据访问层持久化 | 通过 |
| 全局异常处理 | 已修复并通过 |
| 服务层测试 | 通过 |

## 第 20 页：黑盒测试 - 等价类

做成表格：

| 对象 | 有效等价类 | 无效等价类 |
|---|---|---|
| 订单号 | 存在订单 | `ORDER-404` |
| 订单动作 | 取车/临停取物/支付 | 不存在订单动作 |
| VIP 插队 | 活动订单 | 不存在订单 |
| 计费预览 | 存在订单/默认订单 | 不存在订单 |
| 室内导航 | 存在订单/默认订单 | 不存在订单 |
| 急停接口 | `active=true/false` | 缺失 `active` |

## 第 21 页：黑盒测试 - 无效用例结果

做成测试结果表：

| 编号 | 场景 | 预期 | 结果 |
|---|---|---|---|
| B-01 | 非法订单号取车 | 400 + `ORDER_NOT_FOUND` | 通过 |
| B-02 | 非法订单号 VIP | 400 + `ORDER_NOT_FOUND` | 通过 |
| B-03 | 非法订单号计费 | 400 + `ORDER_NOT_FOUND` | 通过 |
| B-04 | 非法订单号导航 | 400 + `ORDER_NOT_FOUND` | 通过 |
| B-05 | 急停缺参数 | 400 + `VALIDATION_FAILED` | 通过 |

## 第 22 页：白盒测试 - 订单状态控制流

放图：`docs/testing/assets/figures/order-service-flow.svg`

讲解点：

1. 取车路径：订单取车、车位缓存、调度队列新增。
2. 临停取物路径：车位缓存，临取任务入队。
3. 支付完成路径：订单关闭、金额计算、车位释放。
4. 不存在订单路径：抛出 `ORDER_NOT_FOUND`。

## 第 23 页：白盒测试 - 急停控制流

放图：`docs/testing/assets/figures/emergency-control-flow.svg`

讲解点：

1. 开启急停：摄像头 ROI、闸机、系统节点、告警联动。
2. 解除急停：设备恢复自动模式。
3. 缺参路径：返回 `VALIDATION_FAILED`。

## 第 24 页：非功能测试 - 性能测试

放图：`docs/testing/assets/figures/performance-benchmark.svg`

下方列关键数据：

| 场景 | 平均响应 | P95 | 成功率 |
|---|---:|---:|---:|
| 运营首页汇总 | 25.85 ms | 69.90 ms | 100% |
| 调度队列 | 11.03 ms | 13.80 ms | 100% |
| 设备总览 | 12.08 ms | 14.52 ms | 100% |
| 计费预览 | 12.97 ms | 15.87 ms | 100% |
| 管理报表 | 10.01 ms | 20.10 ms | 100% |
| 视觉推理 | 8.70 ms | 16.67 ms | 100% |

## 第 25 页：非功能测试 - 可靠性与安全

建议做成两栏：

可靠性：

1. 前后端可一键启动。
2. H2 文件数据库持久化。
3. 急停可触发并恢复。
4. 前端页面定时轮询刷新状态。

安全：

1. 非法订单返回结构化错误。
2. 缺参请求返回校验错误。
3. 业务异常不直接暴露堆栈。
4. 急停状态会锁定闸机和节点。

可以补一句边界：当前未接登录鉴权，生产化时需补角色权限和审计。

## 第 26 页：可用性测试

做成三块：

导航性：

1. 左侧导航覆盖 9 个页面。
2. 页面标题清楚。
3. 顶部按钮保留核心演示动作。

可视性：

1. KPI、状态颜色、表格、设备事件清晰。
2. 急停状态在数字孪生页面明显可见。

交互友好：

1. 车主端按钮流程接近真实用户操作。
2. 管理台支持报表生成。
3. AI 页面可直接触发推理。

## 第 27 页：测试发现与修复

做成表格：

| 编号 | 问题 | 发现方式 | 修复 |
|---|---|---|---|
| BUG-01 | 急停缺参返回 500 | 黑盒测试 | 增加缺失参数异常处理 |
| BUG-02 | 设备事件 ID 并发碰撞 | 性能测试 | ID 增加随机后缀 |

重点表达：系统测试不仅验证通过，也发现并修复了真实问题。

## 第 28 页：总结

放图：`docs/testing/assets/figures/test-results-overview.svg`

总结文案：

ParkVision 当前已经形成完整系统测试闭环：前端页面读取后端真实业务数据，订单、车位、调度、设备、计费、导航和管理台之间可以联动；数据库默认持久化，设备侧通过后端模拟真实协议字段参与演示；功能测试、黑盒异常测试、性能测试均通过。

结束语：

后续可继续补充登录鉴权、接口限流、端到端自动化测试和真实 MySQL 部署，但当前版本已经满足系统测试汇报要求。

## 制作注意事项

1. 图片尽量使用原图，不要压缩到模糊。
2. 截图页建议采用 16:9 宽屏版式。
3. 表格页不要放太多字，每页保留 3 到 5 个讲解点。
4. 章节页可参考原 PPT 的 Part 分隔风格。
5. 所有路径均为仓库相对路径，队友打开项目后可直接找到素材。
