# ParkVision 系统测试汇报 PPT 制作说明

参考文件：`docs/文档/果团团系统测试.pptx`

这版说明已经重新对照参考 PPT。参考 PPT 一共 34 页，结构是“封面 + 目录 + 7 个章节 + 感谢页”，不是之前 28 页的截图讲解版。尤其是功能性测试部分，参考 PPT 的第 7-10 页是连续 4 页“测试用例表”，所以 ParkVision 也必须按表格呈现，不能只放前端截图。

## 参考 PPT 结构

| 页码 | 参考 PPT 内容 | ParkVision 对应内容 |
|---:|---|---|
| 1 | 封面 | ParkVision 智能停车系统测试汇报 |
| 2 | 目录 | 需求测试、功能性测试、静态测试、黑盒测试、白盒测试、非功能测试、可用性测试 |
| 3 | Part1 需求测试 | 需求测试章节页 |
| 4 | 功能性需求表 | ParkVision 功能性需求表 |
| 5 | 非功能性需求表 | ParkVision 非功能性需求表 |
| 6 | Part2 功能性测试 | 功能性测试章节页 |
| 7-10 | 测试用例表 | 4 页 ParkVision 功能测试用例表 |
| 11 | Part3 静态测试 | 静态测试章节页 |
| 12-13 | 前后端静态测试 | 前端、后端静态检查与脚本检查 |
| 14 | Part4 黑盒测试 | 黑盒测试章节页 |
| 15-17 | 等价类、有效/无效测试套件 | ParkVision 黑盒测试三张表 |
| 18 | Part5 白盒测试 | 白盒测试章节页 |
| 19-25 | 自动化、代码、控制流图、白盒用例 | OrderService、DeviceService、调度链路白盒测试 |
| 26 | Part6 非功能测试 | 非功能测试章节页 |
| 27-30 | 性能、安全测试 | 性能压测、接口异常、安全边界 |
| 31 | Part7 可用性测试 | 可用性测试章节页 |
| 32-33 | 导航、可视性、错误处理 | 前端页面截图和异常处理截图 |
| 34 | 感谢页 | 汇报结束页，附一句测试结论 |

## 素材位置

优先使用 PNG，PPT 兼容性最好；SVG 是可放大备份。

| 素材 | 推荐路径 | 用途 |
|---|---|---|
| 功能性需求表 | `docs/testing/assets/ppt/functional-requirements.png` | 第 4 页 |
| 非功能性需求表 | `docs/testing/assets/ppt/nonfunctional-requirements.png` | 第 5 页 |
| 功能测试用例表 1 | `docs/testing/assets/ppt/functional-test-cases-01.png` | 第 7 页 |
| 功能测试用例表 2 | `docs/testing/assets/ppt/functional-test-cases-02.png` | 第 8 页 |
| 功能测试用例表 3 | `docs/testing/assets/ppt/functional-test-cases-03.png` | 第 9 页 |
| 功能测试用例表 4 | `docs/testing/assets/ppt/functional-test-cases-04.png` | 第 10 页 |
| 静态测试检查表 | `docs/testing/assets/ppt/static-test-checklist.png` | 第 12-13 页 |
| 等价类列表 | `docs/testing/assets/ppt/blackbox-equivalence.png` | 第 15 页 |
| 有效等价类测试套件 | `docs/testing/assets/ppt/blackbox-valid-suite.png` | 第 16 页 |
| 无效等价类测试套件 | `docs/testing/assets/ppt/blackbox-invalid-suite.png` | 第 17 页 |
| 白盒条件覆盖表 | `docs/testing/assets/ppt/whitebox-coverage.png` | 第 25 页 |
| 性能测试参数表 | `docs/testing/assets/ppt/performance-method.png` | 第 27 页 |
| 安全与异常处理表 | `docs/testing/assets/ppt/security-summary.png` | 第 29-30 页 |
| 测试结果总览 | `docs/testing/assets/figures/test-results-overview.svg` | 总结、结论 |
| 测试数据规模 | `docs/testing/assets/figures/dataset-overview.svg` | 数据说明 |
| 性能压测图 | `docs/testing/assets/figures/performance-benchmark.svg` | 第 28 页 |
| 订单白盒流程 | `docs/testing/assets/figures/order-service-flow.svg` | 第 21 页 |
| 急停白盒流程 | `docs/testing/assets/figures/emergency-control-flow.svg` | 第 23 页 |
| 运营首页截图 | `docs/testing/assets/screenshots/dashboard.png` | 封面、可用性 |
| AI 感知截图 | `docs/testing/assets/screenshots/ai-vision.png` | 功能证据、可用性 |
| 调度中心截图 | `docs/testing/assets/screenshots/dispatch.png` | 功能证据、可用性 |
| 数字孪生截图 | `docs/testing/assets/screenshots/twin.png` | 可视化证据 |
| 急停截图 | `docs/testing/assets/screenshots/twin-emergency.png` | 错误处理、安全联动 |
| 管理报表截图 | `docs/testing/assets/screenshots/admin-report.png` | 管理台、报表 |
| 管理订单截图 | `docs/testing/assets/screenshots/admin-orders.png` | 管理台订单 |
| 车主端截图 | `docs/testing/assets/screenshots/owner.png` | C 端流程 |
| 室内导航截图 | `docs/testing/assets/screenshots/indoor-map.png` | 可用性、导航 |
| 系统配置截图 | `docs/testing/assets/screenshots/system.png` | 设备可观测性 |

## 制作风格

沿用参考 PPT 的视觉逻辑：章节页用大号 `PartX` 标题，正文页以表格和截图为主，页面标题左上角对齐，表格占据页面主体。ParkVision 不要做成纯产品介绍 PPT，它是系统测试汇报，所以每一页都要能回答“测了什么、怎么测、结果是什么”。

功能测试页的优先级最高。第 7-10 页必须放测试用例表，字段必须包含：功能模块、用例、用例编号、用例说明、前置条件、输入/动作、预期结果、测试结果、失败原因。前端截图可以作为后面可用性测试证据，不能替代功能测试表。

## 第 1 页：封面

标题：ParkVision 智能停车系统测试汇报

副标题：专业方向综合项目汇报 - 系统测试

内容：小组名称、汇报人、日期、课程名称。

图片：右侧或底部放 `docs/testing/assets/screenshots/dashboard.png`，裁出 KPI、车位概览和交通预测区域。

## 第 2 页：目录

目录按参考 PPT 保持 7 项：

1. 需求测试
2. 功能性测试
3. 静态测试
4. 黑盒测试
5. 白盒测试
6. 非功能测试
7. 可用性测试

不要加“项目介绍”章节，避免偏离系统测试主题。

## 第 3 页：Part1 需求测试

做章节页，标题写：

Part1 需求测试

英文小字可写：Requirement Test

页面只保留章节标题和少量装饰，不放大段正文。

## 第 4 页：功能性需求

主图：`docs/testing/assets/ppt/functional-requirements.png`

讲解点：

1. 功能性需求按需求编号、目标用户、子系统和质量属性进行检查。
2. ParkVision 覆盖运营首页、订单、调度、AI、管理台、导航和设备监控。
3. 每条需求都明确可测试、可追踪。

## 第 5 页：非功能性需求

主图：`docs/testing/assets/ppt/nonfunctional-requirements.png`

讲解点：

1. 非功能需求覆盖可启动性、持久化、性能、可靠性、可观测性、可用性和安全边界。
2. 当前系统使用 H2 文件数据库，拉取仓库后可直接启动并自动初始化数据。
3. 外部设备没有真实硬件，但后端按 ONVIF、Modbus/TCP、OCPP 风格模拟现场设备状态。

## 第 6 页：Part2 功能性测试

做章节页，标题写：

Part2 功能性测试

英文小字可写：Functional Test

## 第 7 页：功能测试用例表（一）运营与订单

主图：`docs/testing/assets/ppt/functional-test-cases-01.png`

这页必须是表格，不要换成运营首页截图。表格覆盖运营首页数据展示、车辆入场、预调度和标准取车。

讲解点：

1. 运营首页不再是模拟死数据，KPI、车位、预测和事件来自后端接口。
2. 入场建单会真实改变订单数量和车位状态。
3. 调度动作会写入调度队列，队列变化可在调度中心看到。

## 第 8 页：功能测试用例表（二）AI 感知与安全联动

主图：`docs/testing/assets/ppt/functional-test-cases-02.png`

讲解点：

1. AI 正常识别返回车牌、置信度、摄像头 ID 和动作建议。
2. 入侵场景会触发摄像头 ROI、闸机急停、节点 warning 和告警新增。
3. 急停解除后，系统回到自动模式。

## 第 9 页：功能测试用例表（三）车主端、计费与导航

主图：`docs/testing/assets/ppt/functional-test-cases-03.png`

讲解点：

1. 车主端覆盖当前订单、取车、临时取物和支付关闭。
2. 动态计费返回组成项、计费规则解释和总金额。
3. 室内导航返回接驳点、距离、ETA 和安全提示。

## 第 10 页：功能测试用例表（四）管理台与系统设备

主图：`docs/testing/assets/ppt/functional-test-cases-04.png`

讲解点：

1. 管理台订单表来自 `/api/admin/orders`。
2. 管理台报表来自 `/api/admin/report`，可以展示最近 7 天收入对比。
3. 系统配置页展示摄像头、闸机、充电桩和系统节点状态。

## 第 11 页：Part3 静态测试

做章节页，标题写：

Part3 静态测试

英文小字可写：Static Test

## 第 12 页：前端静态测试

推荐排版：左侧放检查点，右侧放截图。

主图：`docs/testing/assets/ppt/static-test-checklist.png`

截图可选：`docs/testing/assets/screenshots/dashboard.png` 或 `docs/testing/assets/screenshots/system.png`

讲解点：

1. 前端路由覆盖 9 个演示页面。
2. Store 优先调用后端 API，接口失败时才兜底。
3. 页面标题、表格、按钮和状态都已经中文化。

## 第 13 页：后端静态测试

推荐排版：左侧放后端结构说明，右侧放代码截图或命令结果。

代码截图建议取：

`backend/src/main/java/com/parkvision/cps/service/OrderService.java`

`backend/src/main/java/com/parkvision/cps/service/DeviceService.java`

讲解点：

1. 后端按 Controller、Service、Repository 分层。
2. Repository 负责 H2 文件数据库读写。
3. 全局异常处理会把业务错误转成结构化响应。
4. 后端测试命令：`.\mvnw.cmd test`，当前通过 5 个测试。

## 第 14 页：Part4 黑盒测试

做章节页，标题写：

Part4 黑盒测试

英文小字可写：Blackbox Test

## 第 15 页：等价类列表

主图：`docs/testing/assets/ppt/blackbox-equivalence.png`

讲解点：

1. 按订单号、订单动作、AI 参数、急停参数、计费、导航和报表查询划分等价类。
2. 有效等价类验证正常业务链路。
3. 无效等价类验证异常输入不会让系统崩溃。

## 第 16 页：有效等价类测试套件

主图：`docs/testing/assets/ppt/blackbox-valid-suite.png`

讲解点：

1. 有效用例覆盖创建订单、VIP 取车、临时取物、AI 正常识别和管理报表。
2. 每个用例都有预期结果和实际输出。
3. 实际输出来自真实运行的后端接口。

## 第 17 页：无效等价类测试套件

主图：`docs/testing/assets/ppt/blackbox-invalid-suite.png`

讲解点：

1. 非法订单号统一返回 `ORDER_NOT_FOUND`。
2. 急停缺少 `active` 参数返回 `VALIDATION_FAILED`。
3. 这部分修复过一个真实问题：缺参急停原来可能返回 500，现在是 400。

## 第 18 页：Part5 白盒测试

做章节页，标题写：

Part5 白盒测试

英文小字可写：Whitebox Test

## 第 19 页：持续集成和自动化测试

参考 PPT 第 19 页是自动化测试说明。ParkVision 这一页写“本地自动化验证链路”。

建议做成四个卡片：

| 类型 | 命令 | 结果 |
|---|---|---|
| 后端单元测试 | `.\mvnw.cmd test` | 通过 |
| 前端单元测试 | `npm test -- --run` | 通过 |
| 前端构建 | `npm run build` | 通过 |
| 系统测试数据采集 | `node scripts/testing/collect-system-test-data.mjs` | 通过 |

## 第 20 页：e.g. 订单服务（changeStatus）- 代码

参考 PPT 第 20、22 页会放代码截图。ParkVision 这里放订单状态变更代码。

截图文件：

`backend/src/main/java/com/parkvision/cps/service/OrderService.java`

截图范围建议包含 `changeStatus` 方法。

讲解点：

1. 订单不存在时抛出业务异常。
2. 取车、临时取物、支付完成对应不同分支。
3. 支付完成会释放车位并计算金额。

## 第 21 页：订单服务 - 控制流图与测试

左侧主图：`docs/testing/assets/figures/order-service-flow.svg`

右侧或底部放：`docs/testing/assets/ppt/whitebox-coverage.png` 的前半部分截图。

讲解点：

1. 覆盖取车分支。
2. 覆盖临时取物分支。
3. 覆盖支付关闭分支。
4. 覆盖订单不存在异常分支。

## 第 22 页：e.g. 设备服务（setEmergency）- 代码

截图文件：

`backend/src/main/java/com/parkvision/cps/service/DeviceService.java`

截图范围建议包含 `setEmergency` 方法和调用 `updateSystemNodes` 的部分。

讲解点：

1. `active=true` 进入急停。
2. `active=false` 解除急停。
3. 急停会联动摄像头、闸机、节点和告警。

## 第 23 页：设备服务 - 控制流图与测试

主图：`docs/testing/assets/figures/emergency-control-flow.svg`

讲解点：

1. AI 入侵和人工急停走同一套设备联动逻辑。
2. 摄像头 ROI、闸机急停位、系统节点状态需要同步变化。
3. 缺参异常由全局异常处理转换为 400 响应。

## 第 24 页：调度与计费链路白盒说明

参考 PPT 第 24 页是订单服务控制流图。ParkVision 可用这一页补充业务链路。

建议排版：左侧画流程，右侧放 4 个分支。

流程：

入场建单 → 分配车位 → 生成调度任务 → 取车/临取/VIP → 计费预览 → 支付关闭 → 释放车位

分支：

1. 普通取车
2. VIP 取车
3. 临时取物
4. 支付关闭

可配图：`docs/testing/assets/screenshots/dispatch.png` 和 `docs/testing/assets/screenshots/pricing.png`

## 第 25 页：白盒测试用例与条件覆盖

主图：`docs/testing/assets/ppt/whitebox-coverage.png`

讲解点：

1. 用 True/False 标记分支覆盖情况，形式对齐参考 PPT 第 25 页。
2. 覆盖订单存在、取车、临取、支付、车位释放、急停开启、急停解除、缺参异常。
3. 白盒测试不是只看页面，而是验证服务层分支是否走到。

## 第 26 页：Part6 非功能测试

做章节页，标题写：

Part6 非功能测试

英文小字可写：Non-functional Test

## 第 27 页：性能测试方法

主图：`docs/testing/assets/ppt/performance-method.png`

讲解点：

1. 参考 PPT 这里解释线程数、Ramp-Up 和循环次数。
2. ParkVision 用 Node 脚本压测核心 API，概念上对应 JMeter 的线程组。
3. 测试对象覆盖运营汇总、调度队列、设备总览、计费预览、管理报表和视觉推理。

## 第 28 页：性能测试结果

主图：`docs/testing/assets/figures/performance-benchmark.svg`

下方补充数据：

| 场景 | 平均响应 | P95 | 成功率 |
|---|---:|---:|---:|
| 运营首页汇总 | 25.85 ms | 69.90 ms | 100% |
| 调度队列 | 11.03 ms | 13.80 ms | 100% |
| 设备总览 | 12.08 ms | 14.52 ms | 100% |
| 计费预览 | 12.97 ms | 15.87 ms | 100% |
| 管理报表 | 10.01 ms | 20.10 ms | 100% |
| 视觉推理 | 8.70 ms | 16.67 ms | 100% |

## 第 29 页：安全测试 - 异常输入

主图：`docs/testing/assets/ppt/security-summary.png`

讲解点：

1. 当前项目没有登录鉴权，所以不能照抄参考 PPT 的默认账号登录测试。
2. 本页聚焦输入校验、业务异常和结构化错误。
3. 非法订单、缺参急停都能返回明确错误码。

## 第 30 页：安全测试 - 安全联动边界

推荐左图：`docs/testing/assets/screenshots/twin-emergency.png`

推荐右图：`docs/testing/assets/screenshots/system.png`

讲解点：

1. AI 入侵或人工急停会锁定闸机，并让节点进入 warning。
2. 急停状态可恢复，恢复动作会写入设备事件。
3. 生产化还需要补登录鉴权、角色权限、接口限流和审计日志。

## 第 31 页：Part7 可用性测试

做章节页，标题写：

Part7 可用性测试

英文小字可写：Usability Test

## 第 32 页：导航性、可视性、交互友好

参考 PPT 第 32 页是多截图拼图。ParkVision 也做 2x2 拼图。

左上：`docs/testing/assets/screenshots/dashboard.png`

右上：`docs/testing/assets/screenshots/owner.png`

左下：`docs/testing/assets/screenshots/admin-report.png`

右下：`docs/testing/assets/screenshots/indoor-map.png`

四个小标题：

1. 控制台导航清晰
2. 车主端操作集中
3. 管理报表可视化
4. 室内导航明确

## 第 33 页：错误处理

参考 PPT 第 33 页是错误处理截图。ParkVision 这里放急停和异常接口处理。

推荐左图：`docs/testing/assets/screenshots/ai-vision.png`

推荐中图：`docs/testing/assets/screenshots/twin-emergency.png`

推荐右图：`docs/testing/assets/ppt/blackbox-invalid-suite.png`

讲解点：

1. AI 入侵可以触发安全联动。
2. 急停状态在数字孪生页可见。
3. 无效输入不会导致系统崩溃，而是返回结构化错误。

## 第 34 页：感谢页

标题：感谢您的聆听！

底部放一句结论：

ParkVision 已完成系统测试闭环：前端读取后端真实业务数据，H2 文件数据库可直接启动，核心功能、黑盒异常、白盒分支、性能和可用性测试均有证据支撑。

## 制作注意事项

1. 第 7-10 页必须使用功能测试用例表，不能只放系统截图。
2. 表格素材优先使用 `docs/testing/assets/ppt` 目录下的 PNG，放大到整页宽度。
3. 章节页可直接仿照参考 PPT 的 Part 样式。
4. 如果某页文字太多，删讲解点，不删表格。
5. 安全测试不要照抄参考 PPT 的登录账号，因为 ParkVision 当前没有登录模块；要讲清楚这是接口输入校验和急停安全边界测试。
6. 所有路径都是仓库相对路径，队友拉取项目后可直接找到素材。

## 重新生成 PPT 表格素材

如果后续改了测试数据或想重新导出 PNG，运行：

```powershell
node .\scripts\testing\generate-ppt-reference-assets.mjs
```

脚本会重新生成 `docs/testing/assets/ppt` 下的 13 组 SVG 和 PNG。
