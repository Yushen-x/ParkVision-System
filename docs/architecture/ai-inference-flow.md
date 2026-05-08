# AI 视觉推理与后端业务联动流程图

PPT 推荐插入：

```text
docs/architecture/ai-inference-flow.svg
```

这张图对应 AI 功能实现章节中的“模型选取与推理流程”或“AI 与系统对接”页面。

讲解词：

```text
摄像头画面先在边缘节点完成图像预处理，再由 YOLOv8 定位车辆、车牌和人员区域；
车牌区域进入 HyperLPR/OCR 识别车牌号，交接区进入 ROI 安全检测；
模型输出被统一封装为 JSON，再通过 VisionController 进入 Java 后端；
后端根据 action 字段触发订单创建、安全急停或预调度等业务动作。
```
