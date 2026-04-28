# TravelPlanner 接下来落地实施方案

> 当前状态：Phase 1-8 已完成，MVP 主链路已跑通。  
> 当前目标：从“能跑 Demo”推进到“可演示、可解释、可扩展的 MVP”。

---

## 1. 当前项目判断

根据 `travelPlanner架构图.png` 和项目检查结果，当前主链路已经基本落地：

```text
需求解析
→ 缺失信息检查
→ 查询任务拆解
→ 工具执行
→ 证据归一化
→ 候选方案生成
→ 方案评分
→ 行程生成
→ 提醒生成
→ 一图流文案生成
→ 落库
→ 历史计划查询
```

当前状态可以定义为：

```text
可跑的规则 + mock/部分真实工具 MVP
```

接下来不要大改架构，不要继续堆大功能，而是优先解决：

```text
1. 配置安全
2. 多轮追问闭环
3. 真实工具补齐
4. 证据驱动方案生成
5. 评分模型落地
6. 一图流结构前端化
```

---

## 2. 总体实施顺序

推荐拆成 3 个批次：

```text
Batch 1：配置安全 + 多轮追问闭环
Batch 2：真实工具补齐，优先地图/路线和景点信息
Batch 3：证据驱动生成 + 评分模型落地 + 一图流结构优化
```

推荐顺序：

```text
先 Batch 1
再 Batch 2
最后 Batch 3
```

原因：

```text
1. 先清除明文 key，避免安全风险
2. 先补多轮闭环，让 Agent 真正能追问和继续规划
3. 再接真实工具，避免真实数据接入后主链路吃不进去
4. 最后做证据驱动和评分，让方案质量真正提升
```

---

# Batch 1：配置安全 + 多轮追问闭环

## 3. Batch 1 目标

本批次目标：

```text
1. 清除所有明文敏感配置
2. 改成环境变量读取
3. 新增 application-example.yml
4. 修正日志包名配置
5. 让 sessionId 真正支持多轮追问
6. 补齐相关测试和文档
```

本批次不做：

```text
1. 不接新真实工具
2. 不做前端
3. 不做 RAG
4. 不做 MCP
5. 不做真实票务下单
6. 不做真实酒店预订
7. 不重写 TravelAgentOrchestrator 主流程
```

---

## 4. P0：配置与运行安全

重点检查：

```text
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-*.yml
README.md
docs/**/*.md
测试文件
```

搜索敏感内容：

```text
api-key
apikey
secret
password
dashscope
DASHSCOPE
sk-
qwen
mysql
```

建议统一环境变量：

```text
DASHSCOPE_API_KEY
SEARCH_API_KEY
WEATHER_API_KEY
AMAP_API_KEY
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

配置示例：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}
      chat:
        options:
          model: ${DASHSCOPE_CHAT_MODEL:qwen-plus}
          temperature: ${DASHSCOPE_TEMPERATURE:0.4}

  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/travel_planner}
    username: ${SPRING_DATASOURCE_USERNAME:root}
    password: ${SPRING_DATASOURCE_PASSWORD:}

travel-agent:
  search:
    api-key: ${SEARCH_API_KEY:}
  weather:
    api-key: ${WEATHER_API_KEY:}
  amap:
    api-key: ${AMAP_API_KEY:}
```

新增：

```text
src/main/resources/application-example.yml
```

示例内容只保留占位符，不允许出现真实 key：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
          temperature: 0.4

  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}

travel-agent:
  search:
    api-key: ${SEARCH_API_KEY}
  weather:
    api-key: ${WEATHER_API_KEY}
  amap:
    api-key: ${AMAP_API_KEY}
```

日志包名修正：

```yaml
logging:
  level:
    com.yeqian.travelagent: DEBUG
```

P0 验收：

```text
[ ] 项目中不再出现真实明文 key
[ ] application-example.yml 只有占位符
[ ] application-dev.yml 不含真实 key
[ ] 无真实 key 时测试不失败
[ ] 无真实 key 时工具可 mock 降级
[ ] mvn test 通过
[ ] mvn clean package 通过
```

---

## 5. P1：多轮追问闭环

目标链路：

```text
用户第一次输入
→ TravelIntentParser 解析出部分信息
→ MissingInfoChecker 判断缺失
→ 返回 needClarification=true + sessionId + clarificationQuestions
→ 保存当前 partial intent

用户第二次输入并携带 sessionId
→ 读取历史 partial intent
→ 解析本次补充信息
→ 合并 intent
→ 再次检查缺失信息
→ 如果完整，进入完整规划链路
```

推荐新增模型：

```java
public class TravelSessionContext {
    private String sessionId;
    private TravelIntent partialIntent;
    private List<ClarificationQuestion> lastQuestions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
}
```

```java
public class ClarificationQuestion {
    private String field;
    private String question;
    private String example;
    private Boolean required;
}
```

响应结构增强：

```json
{
  "planId": null,
  "sessionId": "session_xxx",
  "needClarification": true,
  "clarificationQuestions": [
    "你从哪个城市出发？"
  ],
  "structuredClarificationQuestions": [
    {
      "field": "departureCity",
      "question": "你从哪个城市出发？",
      "example": "例如：西安",
      "required": true
    }
  ],
  "intent": {}
}
```

Intent 合并规则：

```text
1. 新输入优先覆盖旧值
2. 旧值存在、新输入为空，则保留旧值
3. list 类型字段做去重合并
4. required 字段补齐后重新检查
```

P1 测试用例：

```text
[ ] 第一次缺少出发地，返回 needClarification=true
[ ] 第一次返回 sessionId
[ ] 第二次带 sessionId 补充出发地，可以合并上下文
[ ] 合并后信息完整，继续生成完整旅行计划
[ ] sessionId 不存在时有明确处理策略
[ ] 缺少人数默认 1
[ ] 缺少预算不阻断流程
[ ] 多轮补充 destinationPreferences 能合并去重
```

文档交付：

```text
docs/yeqian/travel-agent-local-run.md
docs/yeqian/travel-agent-multiturn-clarification.md
```

Batch 1 验收命令：

```bash
mvn test
mvn clean package
```

---

## 6. Batch 1 Codex 执行 Prompt

```text
开始执行 Batch 1：配置安全 + 多轮追问闭环。

背景：
根据 travelPlanner 架构图和当前项目检查，MVP 主链路已经基本落地：
需求解析 -> 缺失信息检查 -> 任务拆解 -> 工具执行 -> 证据归一化 -> 候选方案 -> 评分 -> 行程 -> 提醒 -> 一图流 -> 落库。

当前不要新增大功能，不要改主架构。本批次只处理 P0 和 P1。

一、P0：配置与运行安全

请完成：

1. 检查 application-dev.yml、application.yml、application-*.yml 中是否存在明文：
   - DashScope API Key
   - 搜索 API Key
   - 天气 API Key
   - 数据库用户名/密码
   - 其他第三方密钥

2. 将所有敏感配置改成环境变量读取。

例如：
   - DASHSCOPE_API_KEY
   - SEARCH_API_KEY
   - WEATHER_API_KEY
   - AMAP_API_KEY
   - SPRING_DATASOURCE_URL
   - SPRING_DATASOURCE_USERNAME
   - SPRING_DATASOURCE_PASSWORD

3. 新增 application-example.yml，用于提交到仓库，里面只保留示例占位符，不允许出现真实 key。

4. 如果 application-dev.yml 需要保留，请确保不包含真实 key，并在 README 中说明本地如何配置环境变量。

5. 修正 logging 包名配置：
   当前业务包是 com.yeqian.travelagent，不是 travelplanner。
   请检查并修正日志级别配置。

6. 确保测试和本地启动不依赖真实 key。
   没有 key 时应走 mock 或返回明确错误，不能启动失败。

二、P1：多轮追问闭环

请完成：

1. 让 sessionId 真正参与上下文闭环。
2. 用户第一次请求缺少必要信息时，生成 sessionId。
3. 保存当前已解析 TravelIntent。
4. 返回 needClarification=true 和 clarificationQuestions。
5. 用户第二次带着同一个 sessionId 补充信息时，系统合并上一次 intent 和本次输入。
6. 如果信息完整，则继续进入后续完整规划链路。

澄清问题结构优化：
clarificationQuestions 不要只是字符串数组，建议增加结构：
- field
- question
- example
- required

保持兼容：
如果当前前端或测试仍依赖字符串问题列表，可以保留旧字段，同时新增 structuredClarificationQuestions。

补充测试：
- 第一次缺少出发地，返回 needClarification=true
- 返回 sessionId
- 第二次带 sessionId 补充“我从西安出发”，能够合并上下文
- 合并后信息完整时继续生成旅行计划
- sessionId 不存在时返回明确错误或按新会话处理，选择一种并在文档说明
- 缺少人数时默认 1
- 缺少预算时不阻断流程

限制：
不要新增 RAG。
不要新增 MCP。
不要开发前端。
不要接真实票务下单。
不要接真实酒店预订。
不要大规模重构主链路。
不要重写 TravelAgentOrchestrator 的整体流程。
不要删除现有测试。

文档更新：
1. docs/yeqian/travel-agent-local-run.md
2. docs/yeqian/travel-agent-multiturn-clarification.md

完成后请输出：
1. 修改文件列表
2. 配置安全处理结果
3. 多轮追问实现说明
4. 新增/修改测试列表
5. curl 测试示例
6. mvn test 结果
7. 当前仍存在的限制
```

---

# Batch 2：真实工具补齐

## 7. Batch 2 目标

优先补齐最影响行程质量的真实工具：

```text
1. 地图 / 路线查询
2. 景点开放与预约信息
```

本批次建议只做：

```text
MapRouteTool / MapApiClient
AttractionInfoTool / SearchApiClient 增强
EvidenceNormalizer 支持路线和景点结构化字段
```

不做：

```text
真实票务下单
真实酒店预订
用户系统
RAG
MCP
```

---

## 8. MapRouteTool 设计

输入：

```json
{
  "originCity": "杭州",
  "destinationCity": "南浔",
  "departureDate": "2026-05-01",
  "transportMode": "PUBLIC_TRANSIT"
}
```

输出：

```json
{
  "origin": "杭州",
  "destination": "南浔",
  "estimatedDurationMinutes": 120,
  "distanceKm": 90,
  "transferSuggestion": "建议高铁/大巴结合，节假日预留额外时间",
  "riskLevel": "MEDIUM",
  "source": "AMAP",
  "fetchedAt": "2026-04-28T14:30:00+08:00"
}
```

降级策略：

```text
有 AMAP_API_KEY：
  调真实地图 API

无 AMAP_API_KEY：
  走 MockMapRouteTool

API 超时：
  返回 failed ToolResult
  主流程继续
  风险提示中标记“路线耗时需二次确认”
```

---

## 9. AttractionInfoTool 增强

查询目标：

```text
景点开放时间
是否需要预约
门票信息
节假日风险
官方来源 URL
```

输出结构：

```json
{
  "attractionName": "灵隐寺",
  "city": "杭州",
  "openTime": "需二次确认",
  "reservationRequired": true,
  "ticketInfo": "需二次确认",
  "holidayRisk": "HIGH",
  "officialSourceUrl": "https://...",
  "confidence": 0.75,
  "fetchedAt": "2026-04-28T14:30:00+08:00"
}
```

来源优先级：

```text
1. 景区官网
2. 官方公众号 / 官方平台
3. 文旅局
4. 地图平台
5. OTA 平台
6. 普通攻略网站
```

没有官方来源时：

```text
必须标记“需二次确认”
```

---

## 10. EvidenceNormalizer 更新

ROUTE evidence：

```json
{
  "evidenceType": "ROUTE",
  "title": "杭州到南浔路线耗时",
  "summary": "预计耗时约 2 小时，节假日建议预留额外时间。",
  "keyFacts": {
    "origin": "杭州",
    "destination": "南浔",
    "durationMinutes": 120,
    "distanceKm": 90,
    "transferSuggestion": "建议提前查询高铁/大巴班次",
    "routeRisk": "MEDIUM"
  }
}
```

ATTRACTION evidence：

```json
{
  "evidenceType": "ATTRACTION",
  "title": "灵隐寺开放与预约信息",
  "summary": "节假日可能需要提前预约，具体以官方平台为准。",
  "keyFacts": {
    "attractionName": "灵隐寺",
    "city": "杭州",
    "openTime": "需二次确认",
    "reservationRequired": true,
    "ticketInfo": "需二次确认",
    "holidayRisk": "HIGH",
    "sourceUrl": "https://..."
  }
}
```

Batch 2 测试：

```text
[ ] 无 AMAP_API_KEY 时 MapRouteTool 走 mock
[ ] 有 AMAP_API_KEY 但 API 失败时主流程不中断
[ ] 路线工具结果可以归一化为 ROUTE evidence
[ ] 景点工具结果可以归一化为 ATTRACTION evidence
[ ] POST /api/travel/plans 返回的 evidences 中包含路线结构化字段
[ ] POST /api/travel/plans 返回的 evidences 中包含景点结构化字段
```

---

## 11. Batch 2 Codex 执行 Prompt

```text
开始执行 Batch 2：真实工具补齐，优先地图/路线查询和景点信息查询。

背景：
Batch 1 已完成配置安全和多轮追问闭环。
当前主链路已跑通，但交通、酒店、景点、路线仍主要依赖 mock 或搜索建议。
本批次目标是增强真实工具能力，但保持失败降级，不破坏主流程。

一、目标

优先补齐：
1. MapRouteTool / MapApiClient
2. AttractionInfoTool / SearchApiClient 增强

暂不实现：
1. 真实票务下单
2. 真实酒店预订
3. 用户登录系统
4. RAG
5. MCP

二、MapRouteTool 要求

请实现或增强地图/路线查询能力：

1. 支持输入：
   - originCity
   - destinationCity
   - departureDate 可选
   - transportMode 可选

2. 输出：
   - origin
   - destination
   - estimatedDurationMinutes
   - distanceKm 可选
   - transferSuggestion
   - riskLevel
   - source
   - fetchedAt

3. 如果配置了 AMAP_API_KEY，则调用真实地图 API。
4. 如果没有配置 AMAP_API_KEY，则降级为 mock。
5. 如果 API 超时或失败，返回失败 ToolResult，但不能中断主流程。

三、AttractionInfoTool 要求

请增强景点信息查询：

1. 优先通过 WebSearchTool 查询：
   - 景点开放时间
   - 是否需要预约
   - 门票信息
   - 节假日风险
   - 官方来源优先

2. 输出结构化信息：
   - attractionName
   - city
   - openTime
   - reservationRequired
   - ticketInfo
   - holidayRisk
   - officialSourceUrl
   - confidence
   - fetchedAt

3. 没有查到官方信息时，标记“需二次确认”。

四、EvidenceNormalizer 更新

请让 EvidenceNormalizer 支持新的结构化字段：

1. 路线证据：
   - origin
   - destination
   - durationMinutes
   - distanceKm
   - transferSuggestion
   - routeRisk

2. 景点证据：
   - openTime
   - reservationRequired
   - ticketInfo
   - holidayRisk
   - sourceUrl

五、测试要求

新增或补充测试：

1. 无 AMAP_API_KEY 时 MapRouteTool 走 mock
2. 有配置但 API 失败时主流程不中断
3. 路线工具结果可以归一化为 ROUTE evidence
4. 景点工具结果可以归一化为 ATTRACTION evidence
5. POST /api/travel/plans 返回的 evidences 中包含路线或景点结构化字段

六、限制

不要重构整个工具层。
不要删除已有 mock 工具。
不要让真实 API 失败影响 mvn test。
测试中应使用 mock client。

七、完成后输出：

1. 修改文件列表
2. 新增工具能力说明
3. 环境变量说明
4. 降级策略说明
5. 测试结果
6. curl 示例
```

---

# Batch 3：证据驱动生成 + 评分模型落地 + 一图流结构优化

## 12. Batch 3 目标

Batch 3 是提升智能感和可解释性的关键。

目标是让：

```text
真实工具结果
→ 结构化证据
→ 候选方案
→ 评分模型
→ 行程生成
→ 风险提示
→ 提醒事项
→ 一图流结构
```

形成真正的数据驱动链路。

---

## 13. Evidence 结构增强

建议让 `TravelEvidence` 支持：

```text
keyFacts
structuredData
evidenceRefs
confidence
sourceUrl
fetchedAt
```

如果当前已有 `normalizedJson`，可以复用。

---

## 14. CandidatePlanGenerator 改造

改造目标：

```text
根据 ROUTE / WEATHER / ATTRACTION / HOTEL / TRANSPORT evidence 生成候选方案
```

要求：

```text
1. 根据路线证据判断城市顺序是否合理
2. 根据用户天数限制候选方案数量和跨城次数
3. 用户偏好“不想太累”时，优先低跨城、低通勤方案
4. 用户预算较低时，优先低成本方案
5. 候选方案记录 usedEvidenceIds 或 evidenceRefs
6. 候选方案说明为什么生成这个方案
```

候选方案结构建议：

```json
{
  "name": "杭州 + 南浔 + 上海轻量路线",
  "route": ["西安", "杭州", "南浔", "上海", "西安"],
  "reason": "路线较顺，跨城次数可控，适合不想太累的节假日出行。",
  "estimatedFatigue": "MEDIUM",
  "estimatedCostLevel": "MEDIUM",
  "usedEvidenceRefs": ["ev_route_001", "ev_attraction_002"]
}
```

---

## 15. TravelScorer 改造

按架构图左侧 6 个评分维度落地：

| 维度 | 权重 |
|---|---:|
| 交通顺路程度 | 25% |
| 总成本 | 20% |
| 疲劳程度 | 25% |
| 景点价值 | 15% |
| 节假日风险 | 10% |
| 天气 / 票务风险 | 5% |

权重集中配置：

```yaml
travel-agent:
  scoring:
    route-convenience-weight: 0.25
    cost-weight: 0.20
    fatigue-weight: 0.25
    attraction-value-weight: 0.15
    holiday-risk-weight: 0.10
    weather-ticket-risk-weight: 0.05
```

每个维度必须返回：

```json
{
  "dimension": "FATIGUE",
  "score": 82,
  "reason": "跨城次数适中，路线耗时可控，符合不想太累的偏好。",
  "evidenceRefs": ["ev_route_001"]
}
```

评分要求：

```text
1. 天气/票务风险不能再是固定分
2. 节假日风险参考 dateText 和 attraction holidayRisk
3. 疲劳程度参考跨城次数、路线耗时、每天景点数量
4. 总成本参考 budget、hotel priceRange、transport priceRange
5. 最终 totalScore 根据权重计算
```

---

## 16. ItineraryPlanner 改造

目标：

```text
从证据里拿具体信息生成行程，而不是纯模板。
```

要求：

```text
1. 每日行程尽量使用 ATTRACTION evidence
2. 交通建议使用 ROUTE / TRANSPORT evidence
3. 酒店建议使用 HOTEL evidence
4. 风险提示引用 evidence 中的 holidayRisk / rainRisk / ticketRisk
5. 没有证据时标记“需二次确认”
6. 不编造具体余票
7. 不编造具体酒店实时价格
```

DailyPlan 建议：

```json
{
  "day": 1,
  "city": "杭州",
  "morning": "抵达杭州，前往酒店区域寄存行李",
  "afternoon": "西湖轻松游",
  "evening": "湖滨步行街，早点休息",
  "fatigueLevel": "LOW",
  "evidenceRefs": ["ev_attraction_001", "ev_route_001"],
  "notes": [
    "节假日建议提前确认景点预约",
    "天气如有小雨，建议准备雨具"
  ]
}
```

---

## 17. ReminderGenerator 改造

提醒要根据证据生成。

如果景点 evidence：

```json
{
  "reservationRequired": true,
  "holidayRisk": "HIGH"
}
```

提醒必须包含：

```text
提前预约景点
```

如果交通 evidence：

```json
{
  "ticketRisk": "HIGH"
}
```

提醒必须包含：

```text
提前查看车票/机票
```

如果天气 evidence：

```json
{
  "rainRisk": "MEDIUM"
}
```

提醒可以包含：

```text
准备雨具
```

---

## 18. ImageBriefGenerator 改造

当前只是文案结构，下一步改成前端可直接渲染的数据结构。

建议输出：

```json
{
  "title": "五一西安出发｜杭州+南浔+上海4天3晚",
  "subtitle": "预算有限、不想太累、多城市轻量路线",
  "routeLine": [
    "西安",
    "杭州",
    "南浔",
    "上海",
    "西安"
  ],
  "dayCards": [
    {
      "day": 1,
      "title": "抵达杭州，轻松开启",
      "items": [
        "西湖轻松游",
        "湖滨步行街",
        "早点休息"
      ]
    }
  ],
  "budgetCards": [
    {
      "name": "交通",
      "value": "需二次确认"
    },
    {
      "name": "住宿",
      "value": "按实际平台为准"
    }
  ],
  "riskTags": [
    "节假日人流较多",
    "热门景点建议提前预约",
    "车票价格需二次确认"
  ],
  "reminderCards": [
    {
      "title": "提前看票",
      "time": "出发前15天"
    }
  ],
  "footerNote": "实时价格和预约信息以官方平台为准"
}
```

---

## 19. Batch 3 测试用例

```text
[ ] 同一目的地，不同预算，评分排序会变化
[ ] 用户说“不想太累”时，高跨城方案疲劳分降低
[ ] 有下雨风险时，天气/票务风险分降低
[ ] 景点需要预约时，风险提示和提醒中出现预约提醒
[ ] ROUTE evidence 中耗时过长时，交通顺路分降低
[ ] 最终 recommendedPlan 包含 scoreDetails
[ ] imageBrief 包含 routeLine、dayCards、riskTags
[ ] 没有证据支持的信息标记“需二次确认”
```

---

## 20. Batch 3 Codex 执行 Prompt

```text
开始执行 Batch 3：证据驱动生成 + 评分模型落地 + 一图流结构优化。

背景：
当前旅游规划 Agent MVP 主流程已跑通，也已经有工具结果和 EvidenceNormalizer。
但 CandidatePlanGenerator、TravelScorer、ItineraryPlanner 对 evidences 的利用还不够深入。
本批次目标是让“真实证据”真正影响候选方案、评分、行程和风险提示。

一、目标

实现以下能力：

1. CandidatePlanGenerator 根据 evidences 生成候选方案，而不是只靠城市名模板。
2. TravelScorer 的 6 个评分维度从用户输入和证据中计算。
3. ItineraryPlanner 从证据中提取具体交通、景点、预约、风险信息。
4. ImageBriefGenerator 输出前端可直接渲染的数据结构。

二、Evidence 结构增强

请检查 TravelEvidence 是否支持这些字段。
如不支持，请以最小改动增加 keyFacts 或 structuredData：

1. ROUTE evidence：
   - origin
   - destination
   - durationMinutes
   - distanceKm
   - transferSuggestion
   - routeRisk

2. WEATHER evidence：
   - city
   - weatherSummary
   - rainRisk
   - temperatureRange
   - outdoorSuitability

3. ATTRACTION evidence：
   - attractionName
   - city
   - openTime
   - reservationRequired
   - ticketInfo
   - holidayRisk
   - sourceUrl

4. HOTEL evidence：
   - city
   - area
   - priceRange
   - convenienceScore

5. TRANSPORT evidence：
   - origin
   - destination
   - transportType
   - duration
   - priceRange
   - ticketRisk

三、CandidatePlanGenerator 改造

要求：

1. 根据 ROUTE evidence 判断城市顺序是否合理。
2. 根据用户天数限制候选方案数量和跨城数量。
3. 如果用户偏好“不想太累”，优先生成低跨城、低通勤方案。
4. 如果预算较低，优先生成住宿/交通成本较低方案。
5. 候选方案必须记录 usedEvidenceIds 或 evidenceRefs，说明用了哪些证据。

四、TravelScorer 改造

把架构图左侧 6 个评分维度做成明确权重配置：

1. 交通顺路程度：25%
2. 总成本：20%
3. 疲劳程度：25%
4. 景点价值：15%
5. 节假日风险：10%
6. 天气 / 票务风险：5%

要求：

1. 权重放到配置类或常量类，不要散落硬编码。
2. 每个维度必须返回：
   - score
   - reason
   - evidenceRefs

3. 天气/票务风险不能再是固定分。
4. 节假日风险要参考 dateText 和 attraction holidayRisk。
5. 疲劳程度要参考跨城次数、路线耗时、每天景点数量。
6. 总成本要参考 budget、hotel priceRange、transport priceRange。
7. 最终 totalScore 根据权重计算。

五、ItineraryPlanner 改造

要求：

1. 每日行程中尽量使用证据中的景点、开放时间、预约要求。
2. 交通建议中使用 ROUTE / TRANSPORT evidence。
3. 酒店建议中使用 HOTEL evidence。
4. 风险提示中必须体现：
   - 哪些信息来自证据
   - 哪些需要二次确认
   - 哪些是工具失败导致的不确定

5. 不允许编造具体余票、具体酒店实时价格。
6. 没有证据支持时，明确写“需二次确认”。

六、ImageBriefGenerator 改造

输出前端可渲染 JSON：

{
  "title": "",
  "subtitle": "",
  "routeLine": [],
  "dayCards": [],
  "budgetCards": [],
  "riskTags": [],
  "reminderCards": [],
  "footerNote": ""
}

要求：
1. 不直接生成图片。
2. 只生成结构化内容。
3. 适合后续前端渲染成一图流。

七、测试要求

新增或补充：

1. 同一目的地，不同预算，评分排序会变化。
2. 用户说“不想太累”时，高跨城方案疲劳分降低。
3. 有下雨风险时，天气/票务风险分降低。
4. 景点需要预约时，风险提示和提醒中必须出现预约提醒。
5. ROUTE evidence 中耗时过长时，交通顺路分降低。
6. 最终 recommendedPlan 包含 scoreDetails。
7. imageBrief 包含 routeLine、dayCards、riskTags。

八、限制

不要引入 RAG。
不要引入 MCP。
不要接真实下单。
不要重写全部类。
不要破坏现有接口字段兼容性。
保留已有测试并保证 mvn test 通过。

九、完成后输出：

1. 修改文件列表
2. 证据驱动改造说明
3. 评分模型说明
4. 示例输入输出
5. 测试结果
6. 当前仍然 mock 的部分
```

---

# 21. 三批次完成后的目标状态

完成 Batch 1～3 后，项目应达到：

```text
可演示
可解释
可测试
可扩展
```

能力目标：

| 能力 | 目标状态 |
|---|---|
| 主链路 | 已跑通 |
| 配置安全 | 无明文 key |
| 多轮追问 | 支持 sessionId 上下文合并 |
| 工具调用 | 部分真实 + mock 降级 |
| 路线工具 | 支持地图 API 或 mock 降级 |
| 景点工具 | 支持开放/预约/风险结构化 |
| 证据归一化 | 支持 keyFacts / structuredData |
| 候选方案 | 受证据影响 |
| 方案评分 | 6 维度可解释 |
| 行程生成 | 受证据影响 |
| 提醒生成 | 受风险和预约证据影响 |
| 一图流 | 前端可渲染结构化 JSON |
| 历史查询 | 支持 |
| RAG | 暂不做 |
| MCP | 暂不做 |
| 真实订票 | 暂不做 |
| 真实订酒店 | 暂不做 |

---

# 22. 最终开发顺序

```text
第 1 步：执行 Batch 1
第 2 步：人工验收多轮追问闭环
第 3 步：执行 Batch 2
第 4 步：人工验收真实工具降级和证据结构
第 5 步：执行 Batch 3
第 6 步：人工验收评分变化和行程质量
第 7 步：整理 Demo 输入输出
第 8 步：准备前端 MVP 或一图流预览页
```

---

# 23. Demo 验收输入

完整输入：

```text
五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累
```

缺少出发地：

```text
五一想出去玩4天，两个人，预算3000，不想太累
```

补充出发地：

```text
我从西安出发，想去杭州上海周边
```

预算变化测试：

```text
五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累
```

```text
五一从西安出发，4天，两个人，预算8000，想去杭州上海周边，不想太累
```

疲劳度测试：

```text
五一从西安出发，4天，两个人，预算3000，想多玩几个城市，但不要太累
```

---

# 24. 后续暂缓事项

以下事项暂缓：

```text
1. RAG 攻略知识库
2. MCP 工具平台化
3. 前端完整 UI
4. 用户登录注册
5. 真实票务下单
6. 真实酒店预订
7. 图片模型生成
8. 推荐系统
9. 商业化订单链路
```

原因：

```text
当前阶段核心目标是把规划闭环做稳，不是做大而全。
```

---

# 25. 一句话总结

接下来不要再扩大架构，而是按下面顺序推进：

```text
Batch 1：安全 + 多轮闭环
Batch 2：真实工具 + 结构化证据
Batch 3：证据驱动生成 + 可解释评分 + 一图流结构
```

三批完成后，TravelPlanner 就会从：

```text
能跑的旅游规划 Demo
```

升级为：

```text
可演示、可解释、可扩展的旅游规划 Agent MVP
```
