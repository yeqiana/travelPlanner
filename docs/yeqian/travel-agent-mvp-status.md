# TravelPlanner MVP 阶段性状态报告

> 状态：P4 只读前端 MVP 初版已完成，当前处于验收收口阶段  
> 当前定位：规则型 Travel Agent MVP  
> 测试状态：后端 52 tests passed；前端 lint/build 已通过  
> 更新时间：2026-04-30  
> 适用范围：后端 MVP 状态说明、后续 Codex 接手、阶段性验收

## 一句话结论

TravelPlanner 当前已完成从自然语言输入到多轮追问、工具调用、结构化证据、可解释评分、行程生成、提醒生成、一图流结构化输出和只读前端展示的 MVP 闭环；当前仍是规则型 Agent，不包含 RAG、MCP 和真实交易链路。

## 1. MVP 当前定位

当前 TravelPlanner 是一个规则型 Travel Agent MVP。

它已经具备从用户自然语言输入到旅行计划响应的最小闭环：

```text
需求解析
→ 缺失信息检查
→ 多轮追问上下文合并
→ 查询任务拆解
→ 工具执行
→ 证据归一化
→ 候选方案生成
→ 6 维评分
→ 行程生成
→ 提醒生成
→ 一图流结构化输出
→ API 响应
```

当前系统目标是“可演示、可解释、可测试、可扩展”，不是完整生产级旅行交易系统。

当前仍未接入：

- RAG 攻略知识库。
- MCP 工具平台。
- 真实票务下单。
- 真实酒店预订。
- 写入型前端能力。

评分和规划仍主要基于规则、mock 降级和有限真实工具结果，不应夸大为智能推荐系统或交易闭环系统。

## 2. 三批次完成情况

### Batch 1：配置安全 + 多轮追问闭环

已完成。

主要能力：

- 敏感配置改为环境变量读取。
- 无真实 key 时测试不依赖外部服务。
- 支持 `sessionId` 多轮上下文合并。
- 第一次请求信息缺失时返回 `needClarification=true`。
- 返回兼容旧接口的 `clarificationQuestions`。
- 新增结构化澄清问题 `structuredClarificationQuestions`。
- 缺少人数时默认 1。
- 缺少预算不阻断主流程。

当前限制：

- `TravelSessionStore` 当前仍是内存态。
- 应用重启、横向多实例或跨进程场景下 session 不持久。

### Batch 2：地图/路线工具 + 景点工具 + ROUTE/ATTRACTION evidence

已完成。

主要能力：

- 新增或增强 `MapRouteTool`。
- 新增或增强 `MapApiClient`。
- 新增或增强 `AttractionInfoTool`。
- 新增或增强 `SearchApiClient`。
- `MapRouteTool` 优先调用地图 API，失败时降级到 mock。
- `AttractionInfoTool` 优先通过搜索结果解析景点开放、预约、门票和节假日风险，失败时降级到 mock。
- `EvidenceNormalizer` 支持 ROUTE evidence 结构化字段。
- `EvidenceNormalizer` 支持 ATTRACTION evidence 结构化字段。
- evidence 中包含 `sourceStatus`、`fallback`、`needSecondConfirm`、`confidence`、`failureReason`。
- 工具失败不阻断主流程。

ROUTE evidence 关键字段：

- `origin`
- `destination`
- `durationMinutes`
- `distanceKm`
- `transferSuggestion`
- `routeRisk`
- `sourceStatus`
- `fallback`
- `needSecondConfirm`
- `confidence`
- `failureReason`

ATTRACTION evidence 关键字段：

- `attractionName`
- `city`
- `openTime`
- `reservationRequired`
- `ticketInfo`
- `holidayRisk`
- `sourceUrl`
- `sourceStatus`
- `fallback`
- `needSecondConfirm`
- `confidence`
- `failureReason`

当前限制：

- 路线 `origin` / `destination` 仍依赖轻量正则解析。
- 景点开放和门票信息仍以保守解析为主，不承诺实时准确。
- 非官方来源会降低置信度并要求二次确认。

### Batch 3：证据消费 + 6 维评分 + 行程/提醒/一图流结构化

已完成。

主要能力：

- `CandidatePlanGenerator` 保留模板兜底，并开始消费 ROUTE / ATTRACTION evidence。
- `TravelCandidatePlan` 新增 `evidenceRefs`。
- `TravelScorer` 保留原 6 个分数字段。
- 新增 `TravelScoreDetail`。
- `TravelScore` 新增 `scoreDetails`。
- `scoreDetails` 固定包含 6 个维度：
  - `ROUTE_CONVENIENCE`
  - `COST`
  - `FATIGUE`
  - `ATTRACTION_VALUE`
  - `HOLIDAY_RISK`
  - `WEATHER_TICKET_RISK`
- 每个评分维度包含：
  - `score`
  - `weight`
  - `reason`
  - `evidenceRefs`
- `ItineraryPlanner` 开始消费 ROUTE / ATTRACTION evidence。
- `ReminderGenerator` 新增 `generate(intent, plan, evidences)` 重载，同时保留旧方法。
- `ImageBrief` 保留 `title`、`subtitle`、`sections`，并新增前端可渲染结构：
  - `routeLine`
  - `dayCards`
  - `budgetCards`
  - `riskTags`
  - `reminderCards`
  - `footerNote`

当前限制：

- 评分仍是启发式规则。
- evidenceRefs 当前使用 `title/sourceName`，还没有复杂 evidenceId 体系。
- WEATHER / HOTEL / TRANSPORT evidence 的结构化程度弱于 ROUTE / ATTRACTION。

### P4：只读前端 MVP 初版

当前状态：

P4：只读前端 MVP 初版已完成，已接入旅行计划展示、追问展示、imageBrief / recommendedPlan fallback，当前处于验收收口阶段。

已完成内容：

- 前端固定使用 Node.js `v20.20.2`，不因 Codex 浏览器插件、IDE 插件或其他工具要求更高版本而调整项目运行基线。
- 只读前端接入后端 `localhost:8080` 的旅行计划接口。
- 支持展示推荐方案、路线概览、日程卡片、风险标签和提醒卡片。
- 支持缺失信息时展示追问状态，并保留 `sessionId` 用于下一轮补充。
- 支持优先消费 `imageBrief`，当 `imageBrief` 为空或字段不完整时 fallback 到 `recommendedPlan`。
- 当前前端不提供真实登录、真实下单、写入型交易或复杂状态管理能力。

验收方式：

- 前端环境检查在 `frontend` 目录执行：
  - `node -v`
  - `npm run lint`
  - `npm run build`
- 手工联调时后端运行在 `localhost:8080`，前端运行在 `localhost:3000`。
- 使用完整旅行需求、缺少出发地、补充出发地、`imageBrief` 为空或字段不完整四类用例验收。
- 移动端视觉重点检查 routeLine、dayCards、riskTags、reminderCards、按钮、卡片滚动区域和横向滚动。

当前限制：

- 当前仅为只读展示型前端 MVP，不做真实登录、收藏、订单、支付、票务、酒店预订等写入能力。
- 当前不新增 RAG，不新增 MCP，不接入新的工具平台。
- 当前不替换前端技术栈，不新增 UI 库，不重构整个前端。
- 当前手工联调需要本地同时启动后端和前端服务。
- 当前 session 仍依赖后端内存态能力，应用重启后无法保证追问上下文恢复。

## 3. 当前主链路

当前主链路由 `TravelAgentOrchestrator` 串联：

```text
POST /api/travel/plans
→ TravelPlanController
→ TravelAgentOrchestrator
→ TravelIntentParser
→ MissingInfoChecker
→ TravelSessionStore
→ TravelTaskPlanner
→ ToolExecutor
→ MapRouteTool / AttractionInfoTool / Mock 工具
→ EvidenceNormalizer
→ CandidatePlanGenerator
→ TravelScorer
→ ItineraryPlanner
→ ReminderGenerator
→ ImageBriefGenerator
→ TravelPlanResponse
```

说明：

- `TravelIntentParser` 负责解析自然语言旅行意图。
- `MissingInfoChecker` 判断是否需要追问。
- `TravelSessionStore` 保存和合并多轮追问上下文。
- `TravelTaskPlanner` 拆解查询任务。
- `ToolExecutor` 按任务类型分发工具，并保证单个工具失败不阻断主流程。
- `EvidenceNormalizer` 把工具结果归一化为 evidence。
- `CandidatePlanGenerator` 生成候选方案。
- `TravelScorer` 对候选方案做 6 维评分。
- `ItineraryPlanner` 生成最终行程。
- `ReminderGenerator` 生成票务、预约、路线确认和出发准备提醒。
- `ImageBriefGenerator` 生成一图流结构化内容。
- `TravelPlanResponse` 统一返回接口结果。

## 4. 当前接口响应能力

`POST /api/travel/plans` 当前响应保留并支持以下字段：

- `sessionId`
- `needClarification`
- `clarificationQuestions`
- `structuredClarificationQuestions`
- `intent`
- `tasks`
- `toolResults`
- `evidences`
- `candidatePlans`
- `scoredPlans`
- `recommendedPlan`
- `score`
- `reminders`
- `imageBrief`
- `risks`
- `createdAt`

其中：

- `candidatePlans[].evidenceRefs` 用于说明候选方案参考了哪些证据。
- `scoredPlans[].score.scoreDetails` 用于展示 6 个评分维度明细。
- `score.score.scoreDetails` 可用于读取当前最高分方案的评分明细。
- `imageBrief` 中保留旧字段 `title`、`subtitle`、`sections`。
- `imageBrief` 中新增：
  - `routeLine`
  - `dayCards`
  - `budgetCards`
  - `riskTags`
  - `reminderCards`
  - `footerNote`

接口兼容性说明：

- 旧字段没有删除。
- 新字段以增量方式加入。
- 旧的 `ReminderGenerator.generate(intent, plan)` 仍可用。
- `TravelCandidatePlan`、`TravelScore`、`ImageBrief` 均保留兼容旧调用的构造器。

## 5. 当前失败降级能力

当前失败降级策略已覆盖 MVP 主流程。

已具备：

- 无 `AMAP_API_KEY` 时，路线工具可降级到 mock。
- 地图 API 地理编码失败或路线查询失败时，可降级到 mock。
- 搜索 API 缺 key 或失败时，景点工具可降级到 mock。
- 单个工具失败不会阻断主流程。
- ROUTE evidence 降级时会标记：
  - `sourceStatus=FALLBACK`
  - `fallback=true`
  - `needSecondConfirm=true`
  - `confidence` 降低
  - `failureReason` 记录原因
- ATTRACTION evidence 降级时会标记：
  - `sourceStatus=FALLBACK`
  - `fallback=true`
  - `needSecondConfirm=true`
  - `openTime=需二次确认`
  - `ticketInfo=需二次确认`
  - `confidence` 降低
  - `failureReason` 记录原因

重要边界：

- FALLBACK / FAILED evidence 不能当作真实确定事实。
- FALLBACK / FAILED evidence 只用于保守扣分、风险提示和二次确认提醒。
- 当前不会基于降级或失败 evidence 生成确定性票价、余票、实时开放状态结论。

## 6. 当前测试结果

当前测试结果：

```text
mvn test
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```text
mvn clean package
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

说明：

- 当前共有 52 个测试通过。
- `mvn clean package` 可成功生成 Spring Boot jar。
- Maven 当前存在 SLF4J 多 binding 警告。
- 该 SLF4J 警告不影响测试和打包通过。

已覆盖的核心场景：

- 缺失信息检查。
- 结构化追问。
- sessionId 多轮上下文合并。
- 查询任务拆解。
- 工具分发。
- 地图工具缺 key 降级。
- 地图 geocode 失败降级。
- 路线查询超时降级。
- 景点搜索缺 key 降级。
- 非官方景点来源降低置信度。
- ROUTE evidence 结构化归一化。
- ATTRACTION evidence 结构化归一化。
- FALLBACK / FAILED evidence 归一化。
- 候选方案 evidenceRefs。
- 6 维 scoreDetails。
- 高路线耗时降低交通和疲劳分。
- 五一 + 高 holidayRisk 降低节假日风险分。
- 预约提醒。
- 二次确认路线/交通提醒。
- 票务/门票确认提醒。
- imageBrief 新结构字段。

尚未覆盖或覆盖不足的风险场景：

- 多实例 session 一致性。
- session 过期清理。
- 复杂自然语言路线解析。
- HOTEL / TRANSPORT / WEATHER 完整结构化证据消费。
- 真实外部 API 长时间异常下的观测和告警。
- 完整接口 JSON 快照测试。
- 大量请求下的性能和并发行为。

## 7. 当前已知限制

1. 当前仍是规则型 Agent MVP，不是完整智能推荐系统。
2. session 当前仍是内存态，应用重启后丢失。
3. 当前没有多实例 session 共享能力。
4. 路线 `origin` / `destination` 仍有轻量解析限制。
5. 真实票务下单未接入。
6. 真实酒店预订未接入。
7. RAG 未接入。
8. MCP 未接入。
9. 只读前端 MVP 初版已完成，但仍处于 P4 验收收口阶段。
10. 评分仍是启发式规则。
11. evidenceRefs 暂用 `title/sourceName`，未建立复杂 evidenceId 体系。
12. WEATHER / HOTEL / TRANSPORT evidence 结构化消费仍较弱。
13. Maven 有 SLF4J 多 binding 警告，但不影响测试和打包。

## 8. P4 手工联调验收清单

联调地址：

- 后端：`localhost:8080`
- 前端：`localhost:3000`

### 验收用例 1：完整旅行需求

输入：

```text
五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累
```

检查：

- 页面能正常发起请求。
- 能展示推荐方案。
- 能展示 `routeLine`。
- 能展示 `dayCards`。
- 能展示 `riskTags`。
- 能展示 `reminderCards`。
- 页面没有空白卡片。
- 页面没有 `undefined` / `null` / `[object Object]`。

### 验收用例 2：缺少出发地

输入：

```text
五一想出去玩4天，两个人，预算3000，不想太累
```

检查：

- 页面展示追问状态。
- 能展示 `clarificationQuestions` 或 `structuredClarificationQuestions`。
- 不渲染空白行程卡片。
- 不误展示推荐方案。
- `sessionId` 能保留用于下一轮补充。

### 验收用例 3：补充出发地

输入：

```text
我从西安出发，想去杭州上海周边
```

检查：

- 前端携带上一轮 `sessionId`。
- 后端能合并上下文。
- 信息完整后进入正常计划展示。
- 页面从追问状态切换到计划展示状态。

### 验收用例 4：imageBrief 为空或字段不完整

检查：

- 页面 fallback 到 `recommendedPlan`。
- 不出现空白主区域。
- 不出现运行时异常。
- 不出现 `undefined` / `null` 文案。

## 9. P4 移动端视觉检查清单

检查项：

- 小屏下 `routeLine` 是否正常换行。
- `dayCards` 是否保持可读。
- `riskTags` 是否换行正常。
- `reminderCards` 是否不重叠。
- 按钮是否不溢出。
- 卡片和滚动区域是否不互相遮挡。
- 页面是否有明显横向滚动。

当前结论：

- 移动端视觉检查项已纳入 P4 收口验收范围。
- 已做代码级走查并完成 P4 范围内的小修复：`routeLine` 改为小屏换行，`dayCards`、`riskTags`、`reminderCards` 增加长文本断词，降低移动端溢出风险。
- 已通过 `npm run lint` 和 `npm run build` 验证样式修复不破坏前端构建。
- 浏览器插件截图走查未执行：当前插件运行时要求 Node.js `>= v22.22.0`，项目基线固定为 `v20.20.2`，本阶段不为插件升级 Node。

## 10. P4 收口验证记录

验证时间：2026-04-30。

前端环境检查：

- `node -v`：`v20.20.2`。
- `npm run lint`：通过，执行 `tsc --noEmit`。
- `npm run build`：通过，Vite 构建成功；存在 chunk 超过 500 kB 的提示，不影响 P4 构建通过。

本地联调检查：

- 后端 `localhost:8080` 已监听。
- 前端 `localhost:3000` 已监听。
- 完整旅行需求接口验证通过：`needClarification=false`，返回 `recommendedPlan`，且 `imageBrief` 中包含 `routeLine`、`dayCards`、`riskTags`、`reminderCards`。
- 缺少出发地接口验证通过：`needClarification=true`，返回 `clarificationQuestions` 和 `structuredClarificationQuestions`，不返回推荐方案。
- 补充出发地接口验证通过：前端应携带上一轮 `sessionId`；接口侧验证同一 `sessionId` 可合并上下文，并在信息完整后返回推荐方案。

未验证项：

- 未执行浏览器插件截图验收；原因是插件运行时要求 Node.js `>= v22.22.0`，而项目前端基线固定为 `v20.20.2`，本阶段不为插件升级 Node。
- `imageBrief` 为空或字段不完整的页面级 fallback 未通过浏览器截图验证；当前以前端适配逻辑、lint 和 build 作为收口验证依据。

## 11. 下一阶段建议

建议按以下优先级推进。

### P0：持久化 session 与计划结果

目标：

- 将内存态 `TravelSessionStore` 迁移到数据库或缓存。
- 增加 session 过期时间。
- 支持应用重启后的多轮追问恢复。

原因：

- 这是从 MVP 走向可用服务的基础能力。

### P1：固化 API 契约和响应样例

目标：

- 补齐 OpenAPI 示例。
- 为 `TravelPlanResponse` 增加典型 JSON 示例。
- 增加接口响应快照测试。

原因：

- 当前响应结构已经扩展，后续前端或其他调用方需要稳定契约。

### P2：增强路线和目的地解析

目标：

- 提升多城市、多段路线、模糊目的地表达的解析能力。
- 避免仅依赖轻量正则。

原因：

- 路线解析质量直接影响 ROUTE evidence、候选方案和评分。

### P3：补齐 WEATHER / HOTEL / TRANSPORT evidence 结构化

目标：

- 为天气、住宿、交通票务建立更稳定 keyFacts。
- 让评分和行程生成更充分消费这些证据。

原因：

- 当前 Batch 3 已打通 evidence 消费模式，下一步应扩展证据类型，而不是重写主链路。

### P4：只读前端 MVP 验收收口

目标：

- 完成只读前端 MVP 的环境检查、手工联调和移动端视觉走查。
- 只修复 P4 验收范围内的问题，不新增大功能。

原因：

- 只读前端 MVP 初版已经接入核心展示能力，当前重点是确认可运行、可展示、无明显空白和异常文案。

### P5：评估 RAG / MCP

目标：

- 后续 P5 只做 RAG / MCP 评估文档，不做实现。

原因：

- 当前阶段核心是稳定 P4 MVP 闭环，不应过早扩大架构或新增工具平台。 
## 后续 Codex 接手注意事项

1. 不要重写 TravelAgentOrchestrator 主链路。
2. 不要删除旧响应字段。
3. 新字段必须以增量方式加入。
4. FALLBACK / FAILED evidence 不能当作真实成功。
5. 测试不能依赖真实 AMAP_API_KEY / SEARCH_API_KEY。
6. session 当前是内存态，改持久化时必须保留现有接口兼容。
7. 前端展示应优先消费 imageBrief、scoreDetails、evidences，而不是重新解析文案。
8. RAG / MCP / 真实下单均属于后续评估项，不是当前 MVP 必需项。
