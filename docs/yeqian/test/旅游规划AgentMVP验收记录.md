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
# 2026-05-07 前端优化浏览器手工验收

## 结论

本次针对下一阶段优化后的前端做移动端浏览器验收，核心场景通过：新增表单可滚动填写，390px 宽度下未发现页面级横向溢出；缺失信息追问可以展示；完整计划可以生成并展示详细时间段行程和上下文快捷提示。

## 验收环境

- 后端：`http://localhost:8080`，已监听。
- 前端：`http://localhost:3000`，已监听。
- 视口：390 x 844，模拟移动端宽度。
- 工具说明：Codex in-app browser 的 Node REPL 工具运行时解析到项目基线 Node `v20.20.2`，但该工具要求 `>= v22.22.0`，因此本次使用本机 Chrome + Playwright 完成浏览器验收；未调整项目前端 Node 基线。

## 验收结果

### 新增表单布局

- 顶部可见字段：出发城市、出发时间、目的地、人数、总预算、天数。
- 下滑后可见字段：行程节奏、交通偏好、酒店偏好、餐饮偏好、必去景点、避开事项、补充要求和开始生成按钮。
- 390px 宽度下 `scrollWidth=390`、`clientWidth=390`，未发现页面级横向溢出。
- 未发现 `undefined`、`null`、`[object Object]` 异常文案。
- 必填校验生效：未填写目的地时“开始生成”按钮保持禁用。

### 缺失信息追问

输入：

```text
五一想出去玩4天，两个人，预算3000，不想太累
```

结果：

- 页面展示“需要补充信息”。
- 展示追问项：
  - 你是从哪个城市出发？
  - 你想去哪个目的地或偏好哪类目的地？
- 底部出现上下文快捷提示，例如“补充出发城市”等。
- 未发现页面级横向溢出。

### 完整计划展示

表单输入：

```text
出发城市：西安
出发时间：五一
目的地：杭州
人数：2
预算：3000元
交通偏好：高铁优先
酒店偏好：地铁旁
餐饮偏好：当地小吃
必去景点：西湖
避开事项：排队太久
```

结果：

- 页面成功展示旅行计划。
- 日程展示包含更具体的时间段，例如 `08:30-10:00`、`13:30-16:30`、`18:30-20:00`。
- 页面展示路线、每日安排、风险/提醒区域。
- 底部快捷提示已变为上下文建议，例如“调整第2天节奏”“减少跨城交通”“换一些西湖餐厅”。
- 未发现页面级横向溢出。
- 未发现 `undefined`、`null`、`[object Object]` 异常文案。

## 发现的问题和风险

- 控制台捕获到 1 个 `404 Not Found` 静态资源请求，未影响主流程；疑似 favicon 或静态资源缺失，后续可单独确认。
- 表单字段明显变多，虽然可滚动且未溢出，但首屏只能看到核心字段，偏好字段需要下滑填写；当前可接受，后续可考虑折叠“更多偏好”。
- 底部快捷提示为横向滚动，长提示在当前视口会露出一部分截断，这是横向滚动交互的预期表现，但后续可考虑限制提示长度。
- 本次未使用 Codex in-app browser 完成验收，原因是工具侧 Node 运行时限制；项目 Node 基线仍保持 `v20.20.2`。

## 2026-05-07 SSE 前端接入补充验收

### 结论

SSE provider 已接入聊天页，生成中不再只显示纯 loading 动画，会展示后端阶段文本；流式完成后可以正常渲染最终行程。

### 验收结果

- 前端调用 `POST /api/travel/plans/stream`。
- 生成中捕获到阶段文本，例如“正在生成每日详细行程”。
- 最终页面可以展示完整计划。
- 未发现 `undefined`、`null`、`[object Object]` 异常文案。
- 如果 SSE 请求失败，前端 API 层会回退到非流式 `POST /api/travel/plans`。
- 控制台仍有 1 个 `404 Not Found` 静态资源请求，和主流程无关。
