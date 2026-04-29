# TravelPlanner MVP Demo 回归用例集

## 1. 文档目的

本文档用于 TravelPlanner MVP 后续演示、手工验收、接口回归和 Codex 接手开发时快速确认主链路是否仍然可用。

当前系统仍是规则型 Agent MVP，不是完整生产级旅行交易系统。回归重点是：

- 接口主链路是否跑通。
- 多轮追问是否可用。
- 工具失败是否可降级。
- evidence 是否进入候选方案、评分、行程、提醒和一图流。
- 响应字段是否保持兼容。

默认接口：

```http
POST /api/travel/plans
Content-Type: application/json
```

请求示例：

```json
{
  "message": "五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累"
}
```

如果服务统一响应外层包含 `data`，以下字段验收均指 `data` 内部字段。

## 2. 用例 1：完整输入场景

### 用例名称

完整输入直接生成旅行计划。

### 输入

```text
五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累
```

### 前置条件

- 服务正常启动。
- 可以配置真实 API key，也可以不配置真实 API key。
- 不配置真实 API key 时应走 mock / fallback 降级。

### 预期响应字段

- `needClarification`
- `candidatePlans`
- `scoredPlans`
- `score`
- `score.score.scoreDetails` 或 `scoredPlans[].score.scoreDetails`
- `evidences`
- `recommendedPlan`
- `reminders`
- `imageBrief`
- `imageBrief.routeLine`
- `imageBrief.dayCards`
- `imageBrief.riskTags`
- `imageBrief.reminderCards`
- `imageBrief.footerNote`

### 关键验收点

- `needClarification=false`。
- `candidatePlans` 不为空。
- `scoredPlans` 不为空。
- `scoreDetails` 包含 6 个维度：
  - `ROUTE_CONVENIENCE`
  - `COST`
  - `FATIGUE`
  - `ATTRACTION_VALUE`
  - `HOLIDAY_RISK`
  - `WEATHER_TICKET_RISK`
- `evidences` 至少包含 `ROUTE` 或 `ATTRACTION`。
- `imageBrief` 包含：
  - `routeLine`
  - `dayCards`
  - `riskTags`
  - `reminderCards`
  - `footerNote`
- `recommendedPlan` 包含路线、每日行程、风险和待办。

### 不允许出现的错误行为

- 完整输入仍返回 `needClarification=true`。
- `candidatePlans` 为空。
- `scoreDetails` 缺少 6 个维度。
- `imageBrief` 只有纯文本 sections，没有新增结构化字段。
- 真实工具失败导致接口 500。

## 3. 用例 2：缺少出发地场景

### 用例名称

缺少出发地时触发澄清问题。

### 输入

```text
五一想出去玩4天，两个人，预算3000，不想太累
```

### 前置条件

- 服务正常启动。
- 不依赖真实外部 API。

### 预期响应字段

- `needClarification`
- `sessionId`
- `clarificationQuestions`
- `structuredClarificationQuestions`
- `intent`

### 关键验收点

- `needClarification=true`。
- 返回非空 `sessionId`。
- `clarificationQuestions` 包含出发地相关问题。
- `structuredClarificationQuestions` 包含 `field=departureCity`。
- `intent` 中应保留已解析到的信息，如日期、天数、人数、预算、旅行风格。

### 不允许出现的错误行为

- 缺少出发地仍直接进入完整规划链路。
- 未返回 `sessionId`。
- 只返回纯文本问题，缺少 `structuredClarificationQuestions`。
- 已解析上下文丢失。

## 4. 用例 3：多轮补充场景

### 用例名称

第一次缺少出发地，第二次使用同一 sessionId 补充后进入完整规划。

### 第一轮输入

```text
五一想出去玩4天，两个人，预算3000，不想太累
```

第一轮请求：

```json
{
  "message": "五一想出去玩4天，两个人，预算3000，不想太累"
}
```

### 第二轮输入

```text
我从西安出发，想去杭州上海周边
```

第二轮请求：

```json
{
  "message": "我从西安出发，想去杭州上海周边",
  "sessionId": "第一轮返回的 sessionId"
}
```

### 前置条件

- 第一轮响应中拿到 `sessionId`。
- 第二轮请求必须携带同一个 `sessionId`。
- 当前 session 存储仍是内存态，服务不能在两轮之间重启。

### 预期响应字段

- `sessionId`
- `needClarification`
- `intent`
- `tasks`
- `evidences`
- `candidatePlans`
- `scoredPlans`
- `recommendedPlan`
- `reminders`
- `imageBrief`

### 关键验收点

- 第二轮响应继续使用同一个 `sessionId`。
- 第二轮 `needClarification=false`。
- `intent.departureCity` 合并为 `西安`。
- `intent.destinationPreferences` 包含杭州 / 上海相关偏好。
- 保留第一轮中的日期、天数、人数、预算和“不想太累”偏好。
- 第二轮进入完整规划链路，返回候选方案、评分、行程、提醒和一图流。

### 不允许出现的错误行为

- 第二轮生成新的 session，导致上下文丢失。
- 第二轮只解析本轮文本，丢失第一轮预算、天数、人数。
- 第二轮仍要求补充出发地。
- 服务重启后仍假设内存 session 可用。

## 5. 用例 4：低预算场景

### 用例名称

低预算下成本维度影响方案排序和解释。

### 输入

```text
五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累
```

### 前置条件

- 服务正常启动。
- 可使用真实工具或 mock 降级。

### 预期响应字段

- `scoredPlans`
- `score`
- `score.score.scoreDetails`
- `candidatePlans`
- `recommendedPlan`

### 关键验收点

- `scoreDetails` 中存在 `dimension=COST`。
- `COST.reason` 能体现预算约束或成本估算逻辑。
- 预算较低时，高跨城、高成本方案不应无脑排第一。
- 推荐方案应倾向低跨城、低疲劳、预算更可控的方案。

### 不允许出现的错误行为

- `COST` 维度固定不变。
- 所有方案预算分完全一样且无解释。
- 高成本方案不考虑预算直接排第一。
- 生成确定性实时酒店价格或实时票价。

## 6. 用例 5：高预算场景

### 用例名称

高预算下成本压力低于低预算场景。

### 输入

```text
五一从西安出发，4天，两个人，预算8000，想去杭州上海周边，不想太累
```

### 前置条件

- 服务正常启动。
- 建议与低预算场景使用同一套环境配置，便于对比。

### 预期响应字段

- `scoredPlans`
- `score`
- `score.score.scoreDetails`
- `recommendedPlan`

### 关键验收点

- `scoreDetails` 中存在 `dimension=COST`。
- 与预算 3000 场景相比，成本压力应更低。
- 成本维度分数或推荐理由应体现预算变化。
- 推荐理由可以更偏向体验完整度，但仍需尊重“不想太累”。

### 不允许出现的错误行为

- 预算 3000 和预算 8000 的成本解释完全没有差异。
- 高预算直接忽略疲劳偏好。
- 生成确定性实时票价、余票或酒店价格。

## 7. 用例 6：高疲劳偏好冲突场景

### 用例名称

用户想多玩城市，但同时要求不要太累。

### 输入

```text
五一从西安出发，4天，两个人，预算3000，想多玩几个城市，但不要太累
```

### 前置条件

- 服务正常启动。
- 可使用真实工具或 mock 降级。

### 预期响应字段

- `candidatePlans`
- `scoredPlans`
- `score`
- `score.score.scoreDetails`
- `recommendedPlan`
- `recommendedPlan.risks`

### 关键验收点

- `scoreDetails` 中存在 `dimension=FATIGUE`。
- `FATIGUE.reason` 能解释跨城次数、旅行天数和“不想太累”的冲突。
- 高跨城方案应被扣分或标记风险。
- 推荐方案不应盲目堆城市。
- 风险或提醒中应提示路线和体力安排需要留缓冲。

### 不允许出现的错误行为

- 忽略“不想太累”，直接推荐高强度多城市路线。
- `FATIGUE` 分数固定不变。
- 不解释疲劳风险。

## 8. 用例 7：工具降级场景

### 用例名称

无真实 API key 时工具降级但主流程不中断。

### 输入

```text
五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累
```

### 前置条件

不配置以下环境变量或配置为空：

- `AMAP_API_KEY`
- `SEARCH_API_KEY`

服务应允许在无真实 key 时启动并执行主流程。

### 预期响应字段

- `needClarification`
- `toolResults`
- `evidences`
- `candidatePlans`
- `scoredPlans`
- `recommendedPlan`
- `reminders`
- `imageBrief`

### 关键验收点

- 主流程不中断。
- 响应不应是 500。
- `evidences` 中存在 `sourceStatus=FALLBACK` 的证据。
- FALLBACK evidence 中应包含 `needSecondConfirm=true`。
- ROUTE 降级时应提示路线耗时、班次或交通安排需二次确认。
- ATTRACTION 降级时应提示开放、预约、门票需二次确认。
- `reminders` 中应出现预约、二次确认路线/交通、票务/门票确认类提醒。

### 不允许出现的错误行为

- 无 API key 导致服务启动失败。
- 无 API key 导致接口 500。
- FALLBACK evidence 被当成真实成功结果使用。
- 生成确定性票价。
- 生成确定性余票。
- 生成确定性开放状态。
- 将内部异常堆栈原样暴露给用户。

## 9. 通用回归检查项

每次 Demo 回归建议额外检查：

- `needClarification=false` 的完整规划响应中，`candidatePlans` 不为空。
- `scoredPlans` 不为空。
- `recommendedPlan.dailyPlans` 不为空。
- `recommendedPlan.risks` 不为空。
- `reminders` 不为空。
- `imageBrief.sections` 保留。
- `imageBrief.routeLine` 不为空。
- `imageBrief.dayCards` 不为空。
- `scoreDetails` 包含 6 个维度。
- FALLBACK / FAILED evidence 不产生确定性事实结论。

## 10. 当前不作为回归验收目标的内容

以下能力当前 MVP 未实现，不应作为本阶段失败项：

- RAG 攻略知识库。
- MCP 工具平台。
- 前端页面。
- 用户登录注册。
- 真实票务下单。
- 真实酒店预订。
- 真实支付。
- 复杂推荐模型训练。
- 多实例 session 共享。
