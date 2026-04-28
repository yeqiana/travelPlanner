# 旅游规划 Agent MVP 验收记录

## 全链路结论

当前 `POST /api/travel/plans` 最小闭环已跑通：

用户输入 -> 需求解析 -> 缺失信息判断 -> 查询任务拆解 -> 工具执行 -> 证据归一化 -> 候选方案生成 -> 方案评分 -> 行程生成 -> 提醒生成 -> 一图流文案生成 -> 落库 -> 返回用户。

## 代码结构检查

- Controller：只做参数接收和统一响应包装，未发现业务逻辑写入 Controller。
- ApplicationService：负责调用 Agent 编排和持久化，职责较薄。
- Orchestrator：集中串联 Agent 主流程，工具调用入口统一在 `ToolExecutor`。
- Tool：真实天气、真实搜索和 mock 工具集中在 `tool` 包。
- Domain：模型和枚举集中在 `domain` 包。
- Infrastructure：外部 API 客户端、配置、持久化 Mapper 和 Entity 集中在 `infrastructure` 包。

偏差说明：

- 当前没有独立 Repository 包，持久化通过 JDBC Mapper 完成，MVP 阶段可接受。
- `POST /api/travel/plans/stream` 未实现。

## 异常与降级

- DashScope 或 OpenAI 兼容模型不可用：`TravelIntentParser` 会降级到规则解析；`POST /api/ai/chat` 会返回明确错误。
- 天气 API Key 未配置：`WeatherApiClient` 抛出明确错误，`WeatherTool` 降级到 `MockWeatherTool`。
- 搜索 API Key 未配置：`SearchApiClient` 抛出明确错误，`WebSearchTool` 降级到 `MockWebSearchTool`。
- 工具调用失败：`ToolExecutor` 捕获单个工具异常，返回失败 `ToolResult`，不阻断其他任务。
- 大模型返回非 JSON：解析异常时进入规则兜底。
- 数据库保存失败：接口返回系统失败，当前不会伪装为成功。
- 缺失出发地、日期、天数：返回 `needClarification=true` 和追问。
- 缺少人数：默认 1。
- 缺少预算：不阻断流程，评分按经济型默认分处理。

## 测试覆盖

已补齐或已有测试：

- `TravelIntentParserTest`
- `MissingInfoCheckerTest`
- `TravelTaskPlannerTest`
- `ToolExecutorTest`
- `EvidenceNormalizerTest`
- `CandidatePlanGeneratorTest`
- `TravelScorerTest`
- `ItineraryPlannerTest`
- `ReminderGeneratorTest`
- `ImageBriefGeneratorTest`
- `TravelPlanControllerIntegrationTest`

覆盖用例：

- 完整输入能生成完整旅行计划。
- 缺少出发地会追问。
- 缺少日期会追问。
- 缺少天数会追问。
- 缺少人数默认 1。
- 缺少预算不阻断流程。
- 工具失败不阻断流程。
- 生成候选方案数量为 1 到 3 个。
- 推荐方案包含评分。
- 最终计划包含 `dailyPlans`。
- 最终计划包含 `reminders`。
- 最终计划包含 `imageBrief`。
- 生成计划后可通过 `planId` 查询历史计划。

## TravelPlanResponse 字段验收

当前响应已包含：

- `planId`
- `needClarification`
- `clarificationQuestions`
- `intent`
- `tasks`
- `evidences`
- `candidatePlans`
- `recommendedPlan`
- `score`
- `reminders`
- `imageBrief`
- `risks`
- `createdAt`

额外保留：

- `toolResults`
- `scoredPlans`

## 运行结果

已执行：

```powershell
$env:JAVA_HOME="D:\soft\jdk17.0.18"
mvn clean test
```

结果：`Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`。

待执行：

```powershell
mvn clean package
```

## 已知限制

- 交通、酒店、景点、路线仍是 mock。
- 天气只接当前天气，不是完整多日天气预报。
- 搜索结果质量依赖 Tavily 返回内容。
- 未实现 SSE 流式接口。
- 未接真实票务下单和酒店预订。
