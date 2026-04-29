# Travel Agent Evidence 契约

## 1. Evidence 定位

Evidence 是 TravelPlanner 中连接“工具结果”和“规划生成”的中间层。

工具层可以来自真实 API、mock 降级或失败兜底；主链路后续不应直接依赖工具原始文本，而应优先读取 `TravelEvidence.keyFacts` 中的结构化字段。

当前 Batch 2 只要求 ROUTE 和 ATTRACTION 具备稳定结构，Batch 3 再让候选方案、评分和行程生成真正消费这些 evidence。

## 2. ROUTE keyFacts

ROUTE evidence 表示路线、距离、耗时和路线风险。

| 字段 | 含义 | 消费建议 |
|---|---|---|
| `origin` | 出发地 | 用于判断路线起点是否符合用户出发城市 |
| `destination` | 目的地 | 用于判断城市顺序和跨城连接 |
| `durationMinutes` | 路线耗时，单位分钟；未知时为 `0` | 评分时影响交通便利度和疲劳度 |
| `distanceKm` | 路线距离，单位公里；未知时为 `0.0` | 可辅助判断跨城强度 |
| `transferSuggestion` | 交通或换乘建议 | 行程中可作为交通建议引用 |
| `routeRisk` | 路线风险，常见值 `LOW`、`MEDIUM`、`HIGH` | 评分和风险提示可直接消费 |
| `source` | 数据来源，如 `AMAP`、`MockRouteTool` | 用于展示和溯源 |
| `sourceStatus` | 来源状态 | 见第 4 节 |
| `fallback` | 是否为降级结果 | 见第 5 节 |
| `needSecondConfirm` | 是否需要二次确认 | 见第 6 节 |
| `confidence` | 置信度，范围建议 `0.0-1.0` | 评分时应作为权重折减依据 |
| `failureReason` | 失败或降级原因 | 用于调试和风险说明 |

## 3. ATTRACTION keyFacts

ATTRACTION evidence 表示景点开放、预约、门票和节假日风险。

| 字段 | 含义 | 消费建议 |
|---|---|---|
| `attractionName` | 景点名称或景点集合名称 | 行程中可作为景点安排来源 |
| `city` | 景点所在城市 | 用于匹配每日城市 |
| `openTime` | 开放时间；不确定时为 `需二次确认` | 不确定时不要写成确定开放 |
| `reservationRequired` | 是否建议预约 | 提醒生成可据此提示提前预约 |
| `ticketInfo` | 门票信息；不确定时为 `需二次确认` | 不要编造实时票价、余票 |
| `holidayRisk` | 节假日风险，常见值 `LOW`、`MEDIUM`、`HIGH` | 风险提示和评分可直接消费 |
| `sourceUrl` | 来源链接，可能为空 | 有官方链接时可用于溯源 |
| `source` | 数据来源，如 `OFFICIAL`、`MAP_PLATFORM`、`OTA_PLATFORM`、`MockAttractionInfoTool` | 用于判断可信度 |
| `sourceStatus` | 来源状态 | 见第 4 节 |
| `fallback` | 是否为降级结果 | 见第 5 节 |
| `needSecondConfirm` | 是否需要二次确认 | 见第 6 节 |
| `confidence` | 置信度，范围建议 `0.0-1.0` | 低置信度信息不能强驱动结论 |
| `failureReason` | 失败或降级原因 | 用于调试和风险说明 |

## 3.1 WEATHER keyFacts

WEATHER evidence 表示目的地天气、穿衣建议和天气风险。

| 字段 | 含义 | 消费建议 |
|---|---|---|
| `weatherSummary` | 天气摘要或查询失败说明 | 行程和一图流展示天气提醒 |
| `temperatureRange` | 温度范围，未知时为 `需二次确认` | 辅助穿衣提醒，不确定时不要写死温度 |
| `dressingAdvice` | 穿衣和雨具建议 | 可进入提醒卡片 |
| `weatherRisk` | 天气风险：`LOW`、`MEDIUM`、`HIGH` | 评分和风险标签可消费 |
| `sourceStatus` / `fallback` / `needSecondConfirm` / `confidence` / `failureReason` | 通用来源状态字段 | 同 ROUTE / ATTRACTION 语义 |

## 3.2 HOTEL keyFacts

HOTEL evidence 表示住宿区域、预算和交通便利度。

| 字段 | 含义 | 消费建议 |
|---|---|---|
| `areaSuggestion` | 推荐住宿区域或区域确认提醒 | 生成住宿建议，不代表真实库存 |
| `budgetSuggestion` | 价格或预算参考，未知时为 `需二次确认` | 只做预算提示，不承诺实时价格 |
| `transportConvenience` | 住宿区域交通便利度建议 | 影响行程便利性和提醒 |
| `priceReliability` | 价格可信度，如 `REFERENCE`、`LOW` | 低可信度时文案必须保守 |
| `sourceStatus` / `fallback` / `needSecondConfirm` / `confidence` / `failureReason` | 通用来源状态字段 | 同 ROUTE / ATTRACTION 语义 |

## 3.3 TRANSPORT keyFacts

TRANSPORT evidence 表示城际交通方式、耗时、费用和票务风险。

| 字段 | 含义 | 消费建议 |
|---|---|---|
| `transportMode` | 交通方式建议，如高铁、飞机、自驾 | 候选方案和行程交通说明可消费 |
| `durationText` | 耗时文本，未知时为 `需二次确认` | 不确定时不要写成确定耗时 |
| `costRange` | 费用区间或价格文本 | 只做预算参考 |
| `ticketRisk` | 票务风险：`LOW`、`MEDIUM`、`HIGH` | 生成抢票、候补和提前预订提醒 |
| `sourceStatus` / `fallback` / `needSecondConfirm` / `confidence` / `failureReason` | 通用来源状态字段 | 同 ROUTE / ATTRACTION 语义 |

## 4. sourceStatus

`sourceStatus` 描述 evidence 的来源状态。

- `SUCCESS`：真实工具调用成功，结果来自外部 API 或可信搜索解析。
- `FALLBACK`：真实工具不可用、超时、失败或信息不确定，已降级到 mock 或保守结果。
- `FAILED`：工具完全失败，没有得到可用真实结果或可用 mock 结果，但仍生成低置信度 evidence 以保证主流程不中断。

后续代码不要只看 `ToolResult.success`，应优先看 `keyFacts.sourceStatus`。

## 5. fallback

`fallback=true` 表示该 evidence 不是完整真实数据，而是降级结果。

降级结果可以用于维持主流程，但不能当作强事实使用。比如路线 mock 可以提示“需要预留时间”，但不能被当成真实路线耗时。

## 6. needSecondConfirm

`needSecondConfirm=true` 表示该信息需要用户或后续工具再次确认。

典型场景：

- mock 降级结果。
- 非官方景点来源。
- 门票、开放时间、预约规则无法确认。
- 地图路线接口失败或超时。

行程文案中遇到该字段时，应使用“建议确认”“以官方平台为准”等保守表达。

## 7. confidence

`confidence` 表示 evidence 的可信度。

建议消费方式：

- `0.8` 以上：可作为较强依据。
- `0.5-0.8`：可参考，但需要保守表达。
- `0.5` 以下：只能作为风险提示或兜底参考。

评分和候选方案生成时，低置信度 evidence 不应强行改变推荐结论。

## 8. failureReason

`failureReason` 记录失败、降级或解析异常原因。

它主要服务于：

- 开发调试。
- 风险提示生成。
- 后续观测工具质量。

面向用户展示时不要原样暴露内部异常栈，只提炼为“路线信息需二次确认”“景点预约信息需确认”等自然语言。

## 9. FALLBACK 不等于真实 SUCCESS

`FALLBACK` 的目标是保证主流程继续，而不是证明信息真实。

例如：

- AMap 超时后返回 mock 路线，只能说明“路线需要确认”，不能说明真实耗时。
- 搜索 API 无 key 后返回 mock 景点信息，只能说明“节假日可能需要预约”，不能说明具体开放时间和票价。

因此 Batch 3 消费 evidence 时必须区分：

- `SUCCESS`：可以参与推荐和评分。
- `FALLBACK`：主要参与风险提示和保守兜底。
- `FAILED`：只能用于说明不确定性，不能生成确定结论。

## 10. Batch 3 消费建议

### CandidatePlanGenerator

- 优先使用 `sourceStatus=SUCCESS` 且 `confidence` 较高的 ROUTE evidence 判断城市顺序。
- 遇到 `routeRisk=HIGH` 或 `durationMinutes` 过大时，减少高强度跨城方案。
- FALLBACK 路线只能作为“需确认路线可行性”的提示，不应强行生成高确定性路线。

### TravelScorer

- 交通便利度参考 ROUTE 的 `durationMinutes`、`distanceKm`、`routeRisk`。
- 景点风险参考 ATTRACTION 的 `reservationRequired`、`holidayRisk`。
- `confidence` 应影响评分权重：低置信度证据对总分影响要降低。
- `sourceStatus=FAILED` 的 evidence 不应给正向加分，只能增加风险或降低确定性。

### ItineraryPlanner

- 每日行程安排景点时，优先使用 `sourceStatus=SUCCESS` 的 ATTRACTION evidence。
- `openTime` 或 `ticketInfo` 为 `需二次确认` 时，文案必须保守。
- `reservationRequired=true` 时，应在 notes 或 reminders 中提示提前预约。
- ROUTE 为 FALLBACK/FAILED 时，交通安排不要写死具体耗时，只写“需提前确认路线和班次”。
