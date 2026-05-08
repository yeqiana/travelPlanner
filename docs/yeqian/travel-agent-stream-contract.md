# Travel Agent SSE 流式接口契约

## 结论

`POST /api/travel/plans/stream` 提供基础 SSE 阶段反馈。非流式 `POST /api/travel/plans` 保持兼容，仍是稳定主接口。

## 请求

请求体与非流式接口一致：

```json
{
  "message": "五一从西安出发去杭州玩4天，两个人，预算5000",
  "sessionId": "demo-session"
}
```

## 事件

- `stage`：阶段性文本，如“正在理解旅行需求”。
- `completed`：完整统一响应，数据结构为 `Result<TravelPlanResponse>`。
- `error`：异常文本。

## 前端策略

- 流式调用失败时展示普通错误提示。
- 非流式接口继续可用，不因 SSE 失败而影响原主流程。
- 最终渲染仍以 `completed.data` 中的结构化行程为准。
