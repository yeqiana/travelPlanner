# 旅游规划 Agent MVP 最小闭环落地实施方案

> 技术路线：Spring Boot + Spring AI Alibaba + 通义千问 DashScope + Tool Calling + 实时检索工具 + 规则评分 + 行程生成  
> 目标：先做一个能跑通完整链路的最小可用版本，不做复杂 RAG、不做 MCP、不直接买票订酒店。

---

## 1. 项目目标

构建一个面向节假日短途/多城市出行的旅游规划 Agent。

用户输入一句自然语言需求，例如：

```text
五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累。
```

系统能够完成：

1. 解析用户旅行需求；
2. 判断是否缺少必要信息；
3. 拆解查询任务；
4. 调用实时工具查询天气、景点、交通、酒店等信息；
5. 归一化查询结果；
6. 生成候选路线；
7. 根据预算、疲劳度、顺路程度等评分；
8. 输出最终旅行计划；
9. 生成购票/预约/出发提醒；
10. 生成一图流文案。

---

## 2. MVP 边界

### 2.1 MVP 要做

第一版只做最小闭环：

```text
用户需求
→ 需求解析
→ 缺失信息判断
→ 查询任务拆解
→ 实时工具调用
→ 信息结构化
→ 候选方案生成
→ 方案评分
→ 行程生成
→ 提醒生成
→ 一图流文案生成
→ 返回用户
```

### 2.2 MVP 不做

第一版明确不做：

```text
不直接购买车票
不直接预订酒店
不承诺实时余票绝对准确
不做复杂 RAG
不做 MCP 平台化
不做多 Agent 协作
不做真实图片生成
不做用户社交分享
不做复杂权限系统
```

### 2.3 后续再扩展

后续可以逐步加入：

```text
真实票务 API
酒店 API
地图深度路线规划
用户偏好记忆
本地攻略知识库
RAG 路线模板库
MCP 工具平台化
图片生成模型
日历系统集成
```

---

## 3. 技术栈选型

| 层级 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.x |
| AI 框架 | Spring AI Alibaba |
| 大模型 | 通义千问 / DashScope |
| 工具调用 | Spring AI Tool Calling |
| 接口风格 | REST + 可选 SSE |
| 数据库 | MySQL 8.x |
| 缓存 | Redis，MVP 可选 |
| ORM | MyBatis-Plus |
| API 文档 | Knife4j / springdoc-openapi |
| 日志 | Logback + TraceId |
| 前端 | Vue3 + TypeScript + Element Plus |
| 部署 | Docker / 本地先跑通 |

---

## 4. 最小闭环架构

```text
用户
  ↓
TravelPlanController
  ↓
TravelPlanningApplicationService
  ↓
TravelAgentOrchestrator
  ↓
TravelIntentParser
  ↓
MissingInfoChecker
  ↓
TravelTaskPlanner
  ↓
ToolExecutor
  ├── WebSearchTool
  ├── WeatherTool
  ├── MapRouteTool
  ├── TransportSearchTool
  ├── HotelSearchTool
  └── AttractionInfoTool
  ↓
EvidenceNormalizer
  ↓
CandidatePlanGenerator
  ↓
TravelScorer
  ↓
ItineraryPlanner
  ↓
ReminderGenerator
  ↓
ImageBriefGenerator
  ↓
TravelPlanResponse
```

---

## 5. 核心模块说明

### 5.1 TravelIntentParser：需求解析器

职责：

把用户自然语言转成结构化旅行需求。

输入：

```text
五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累。
```

输出：

```json
{
  "departureCity": "西安",
  "dateText": "五一",
  "days": 4,
  "peopleCount": 2,
  "budget": 3000,
  "destinationPreferences": ["杭州", "上海周边"],
  "travelStyles": ["不想太累", "节假日", "多城市"],
  "transportPreference": null,
  "hotelBudgetPerNight": null
}
```

实现方式：

- 使用 Spring AI ChatClient 调用大模型；
- Prompt 约束模型只返回 JSON；
- 后端用 DTO 接收；
- 解析失败时返回友好错误或进入兜底追问。

---

### 5.2 MissingInfoChecker：缺失信息检查器

职责：

判断是否需要追问用户。

核心规则：

| 字段 | 策略 |
|---|---|
| 出发地 | 必须 |
| 出行时间 | 必须 |
| 出行天数 | 必须 |
| 人数 | 可默认 1 人 |
| 预算 | 可默认经济型 |
| 目的地 | 可由系统推荐 |
| 偏好 | 可默认不太累、少踩坑 |

追问原则：

```text
能默认就默认，不要频繁追问。
只有缺少出发地、时间、天数这类核心条件时才追问。
```

示例返回：

```json
{
  "needClarification": true,
  "questions": [
    "你是从哪个城市出发？",
    "计划出行几天？"
  ]
}
```

---

### 5.3 TravelTaskPlanner：查询任务拆解器

职责：

根据旅行需求拆解出工具可执行的查询任务。

示例输出：

```json
[
  {
    "taskType": "WEATHER",
    "query": "杭州 五一 天气"
  },
  {
    "taskType": "TRANSPORT",
    "query": "西安 到 杭州 高铁 机票"
  },
  {
    "taskType": "HOTEL",
    "query": "杭州 五一 酒店 300元 附近区域"
  },
  {
    "taskType": "ATTRACTION",
    "query": "杭州 灵隐寺 五一 预约 开放时间"
  },
  {
    "taskType": "ROUTE",
    "query": "杭州 上海 南浔 4天3晚 路线可行性"
  }
]
```

第一版任务类型：

```text
WEATHER
TRANSPORT
HOTEL
ATTRACTION
ROUTE
GENERAL_WEB
```

---

### 5.4 ToolExecutor：工具执行器

职责：

根据任务类型调用对应工具。

MVP 工具优先级：

| 工具 | MVP 策略 |
|---|---|
| WebSearchTool | 必须接 |
| WeatherTool | 必须接 |
| AttractionInfoTool | 必须接 |
| MapRouteTool | 可先接高德 API 或用搜索结果估算 |
| TransportSearchTool | 第一版可用搜索 API 获取建议，不承诺实时余票 |
| HotelSearchTool | 第一版可用搜索 API 获取区域和价格区间 |

工具调用结果统一返回 ToolResult。

```json
{
  "taskType": "WEATHER",
  "source": "WeatherTool",
  "success": true,
  "rawContent": "...",
  "fetchedAt": "2026-04-27T15:30:00+08:00"
}
```

---

### 5.5 EvidenceNormalizer：证据归一化器

职责：

把不同工具返回的信息统一成 TravelEvidence。

统一结构：

```json
{
  "evidenceType": "WEATHER",
  "city": "杭州",
  "title": "杭州五一期间天气概况",
  "summary": "预计有小雨，建议安排轻户外和室内备选。",
  "keyFacts": {
    "risk": "小雨",
    "suggestion": "准备雨具，减少纯户外长时间安排"
  },
  "confidence": 0.8,
  "sourceName": "天气工具",
  "sourceUrl": null,
  "fetchedAt": "2026-04-27T15:30:00+08:00"
}
```

作用：

- 降低大模型胡编；
- 方便评分；
- 方便保存；
- 方便后续展示来源。

---

### 5.6 CandidatePlanGenerator：候选方案生成器

职责：

基于用户需求和证据生成 2～3 个候选路线。

示例：

```json
[
  {
    "name": "杭州 + 南浔 + 上海",
    "route": ["西安", "杭州", "南浔", "上海", "西安"],
    "reason": "路线顺路，强度适中，适合不想太累的人。"
  },
  {
    "name": "杭州 + 苏州 + 上海",
    "route": ["西安", "杭州", "苏州", "上海", "西安"],
    "reason": "交通方便，但五一热门城市人流较大。"
  },
  {
    "name": "杭州 + 上海深度游",
    "route": ["西安", "杭州", "上海", "西安"],
    "reason": "跨城少，疲劳度低，但多城市体验弱。"
  }
]
```

---

### 5.7 TravelScorer：方案评分器

职责：

对候选方案进行多维度评分。

评分维度：

| 维度 | 权重 |
|---|---:|
| 交通顺路程度 | 25% |
| 疲劳程度 | 25% |
| 预算匹配 | 20% |
| 景点价值 | 15% |
| 节假日风险 | 10% |
| 天气 / 票务风险 | 5% |

评分结果：

```json
{
  "planName": "杭州 + 南浔 + 上海",
  "totalScore": 86,
  "routeConvenienceScore": 90,
  "fatigueScore": 88,
  "budgetScore": 82,
  "attractionValueScore": 85,
  "holidayRiskScore": 78,
  "weatherTicketRiskScore": 80,
  "recommendationReason": "交通较顺，跨城强度适中，预算可控。"
}
```

第一版评分可以用规则，不需要模型全权判断。

---

### 5.8 ItineraryPlanner：行程规划器

职责：

根据最高分方案生成每日行程。

输出结构：

```json
{
  "title": "杭州 + 南浔 + 上海 4天3晚轻量路线",
  "summary": "适合预算有限、不想太累、想一次玩多个地方的用户。",
  "route": ["西安", "杭州", "南浔", "上海", "西安"],
  "dailyPlans": [
    {
      "day": 1,
      "city": "杭州",
      "morning": "抵达杭州，前往酒店寄存行李",
      "afternoon": "西湖轻松游",
      "evening": "湖滨步行街，早点休息",
      "fatigueLevel": "低",
      "notes": ["第一天不安排高强度景点"]
    }
  ],
  "transportSuggestions": [],
  "hotelSuggestions": [],
  "budgetEstimate": {},
  "risks": []
}
```

---

### 5.9 ReminderGenerator：提醒生成器

职责：

生成购票、预约、出发准备提醒。

示例：

```json
[
  {
    "title": "查看西安到杭州车票/机票",
    "type": "TICKET",
    "remindRule": "出发前15天 09:00",
    "description": "优先关注卧铺、早到高铁和低价机票。"
  },
  {
    "title": "预约热门景点",
    "type": "ATTRACTION",
    "remindRule": "出发前7天 10:00",
    "description": "节假日热门景区建议提前预约。"
  },
  {
    "title": "出发前检查证件和充电设备",
    "type": "PREPARE",
    "remindRule": "出发前1天 20:00",
    "description": "检查身份证、充电器、雨具、常用药。"
  }
]
```

---

### 5.10 ImageBriefGenerator：一图流文案生成器

职责：

生成适合前端渲染成图片的结构化内容。

第一版不直接生成图片，只生成图片内容结构。

```json
{
  "title": "五一西安出发｜杭州+南浔+上海4天3晚",
  "subtitle": "预算有限、不想太累、多城市轻量路线",
  "sections": [
    {
      "title": "路线",
      "content": "西安 → 杭州 → 南浔 → 上海 → 西安"
    },
    {
      "title": "适合人群",
      "content": "两人出行、预算3000左右、想多玩几个地方但不想太累"
    },
    {
      "title": "每日安排",
      "content": "Day1 杭州轻松游；Day2 灵隐寺+南浔；Day3 上海 citywalk；Day4 返程"
    }
  ]
}
```

---

## 6. 项目目录结构

```text
travel-planner-agent
├── pom.xml
├── src/main/java/com/yeqian/travelagent
│   ├── TravelAgentApplication.java
│   │
│   ├── interfaces
│   │   └── controller
│   │       └── TravelPlanController.java
│   │
│   ├── application
│   │   ├── service
│   │   │   └── TravelPlanningApplicationService.java
│   │   └── dto
│   │       ├── TravelPlanRequest.java
│   │       ├── TravelPlanResponse.java
│   │       └── TravelStreamResponse.java
│   │
│   ├── agent
│   │   ├── orchestrator
│   │   │   └── TravelAgentOrchestrator.java
│   │   ├── parser
│   │   │   └── TravelIntentParser.java
│   │   ├── checker
│   │   │   └── MissingInfoChecker.java
│   │   ├── planner
│   │   │   ├── TravelTaskPlanner.java
│   │   │   ├── CandidatePlanGenerator.java
│   │   │   └── ItineraryPlanner.java
│   │   ├── scorer
│   │   │   └── TravelScorer.java
│   │   ├── normalizer
│   │   │   └── EvidenceNormalizer.java
│   │   └── generator
│   │       ├── ReminderGenerator.java
│   │       └── ImageBriefGenerator.java
│   │
│   ├── tool
│   │   ├── ToolExecutor.java
│   │   ├── WebSearchTool.java
│   │   ├── WeatherTool.java
│   │   ├── MapRouteTool.java
│   │   ├── TransportSearchTool.java
│   │   ├── HotelSearchTool.java
│   │   └── AttractionInfoTool.java
│   │
│   ├── domain
│   │   ├── model
│   │   │   ├── TravelIntent.java
│   │   │   ├── TravelTask.java
│   │   │   ├── TravelEvidence.java
│   │   │   ├── TravelCandidatePlan.java
│   │   │   ├── TravelScore.java
│   │   │   ├── TravelPlan.java
│   │   │   ├── TravelReminder.java
│   │   │   └── ImageBrief.java
│   │   └── enums
│   │       ├── TravelTaskType.java
│   │       ├── EvidenceType.java
│   │       └── FatigueLevel.java
│   │
│   ├── infrastructure
│   │   ├── ai
│   │   │   ├── SpringAiConfig.java
│   │   │   ├── PromptTemplateFactory.java
│   │   │   └── JsonOutputParser.java
│   │   ├── client
│   │   │   ├── SearchApiClient.java
│   │   │   ├── WeatherApiClient.java
│   │   │   └── MapApiClient.java
│   │   ├── persistence
│   │   │   ├── entity
│   │   │   ├── mapper
│   │   │   └── repository
│   │   └── config
│   │       └── TravelAgentProperties.java
│   │
│   └── common
│       ├── result
│       ├── exception
│       └── trace
│
└── src/main/resources
    ├── application.yml
    └── prompts
        ├── travel-intent-parser.st
        ├── travel-task-planner.st
        ├── itinerary-planner.st
        ├── reminder-generator.st
        └── image-brief-generator.st
```

---

## 7. API 设计

### 7.1 生成旅行计划

```http
POST /api/travel/plans
```

请求：

```json
{
  "message": "五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累",
  "sessionId": "optional-session-id"
}
```

响应：

```json
{
  "planId": "plan_20260427_001",
  "needClarification": false,
  "clarificationQuestions": [],
  "intent": {},
  "tasks": [],
  "evidences": [],
  "candidatePlans": [],
  "recommendedPlan": {},
  "score": {},
  "reminders": [],
  "imageBrief": {}
}
```

---

### 7.2 流式生成旅行计划

```http
POST /api/travel/plans/stream
```

SSE 返回阶段性事件：

```text
event: progress
data: 正在解析旅行需求...

event: progress
data: 正在查询天气和景点预约信息...

event: progress
data: 正在生成候选方案...

event: result
data: {最终结果 JSON}
```

---

### 7.3 查询历史计划

```http
GET /api/travel/plans/{planId}
```

---

### 7.4 生成一图流文案

```http
POST /api/travel/plans/{planId}/image-brief
```

---

## 8. 数据库表设计

### 8.1 travel_plan

```sql
CREATE TABLE travel_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    user_message TEXT NOT NULL,
    departure_city VARCHAR(64),
    destination_text VARCHAR(255),
    date_text VARCHAR(64),
    start_date DATE,
    end_date DATE,
    days INT,
    people_count INT,
    budget DECIMAL(10,2),
    status VARCHAR(32) NOT NULL,
    final_plan_json JSON,
    score_json JSON,
    image_brief_json JSON,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_plan_id (plan_id)
);
```

---

### 8.2 travel_task

```sql
CREATE TABLE travel_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id VARCHAR(64) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    query_text VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    result_json JSON,
    error_message TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_plan_id (plan_id),
    INDEX idx_task_type (task_type)
);
```

---

### 8.3 travel_evidence

```sql
CREATE TABLE travel_evidence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id VARCHAR(64) NOT NULL,
    evidence_type VARCHAR(32) NOT NULL,
    title VARCHAR(255),
    summary TEXT,
    source_name VARCHAR(128),
    source_url VARCHAR(512),
    raw_content MEDIUMTEXT,
    normalized_json JSON,
    confidence DECIMAL(5,2),
    fetched_at DATETIME,
    created_at DATETIME NOT NULL,
    INDEX idx_plan_id (plan_id),
    INDEX idx_evidence_type (evidence_type)
);
```

---

### 8.4 travel_reminder

```sql
CREATE TABLE travel_reminder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    reminder_type VARCHAR(32) NOT NULL,
    remind_rule VARCHAR(128),
    remind_time DATETIME,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_plan_id (plan_id)
);
```

---

## 9. 核心编排伪代码

```java
public class TravelAgentOrchestrator {

    public TravelPlanResponse plan(TravelPlanRequest request) {

        // 1. 需求解析
        TravelIntent intent = travelIntentParser.parse(request.getMessage());

        // 2. 缺失信息判断
        MissingInfoCheckResult checkResult = missingInfoChecker.check(intent);
        if (checkResult.needClarification()) {
            return TravelPlanResponse.needClarification(checkResult.getQuestions());
        }

        // 3. 查询任务拆解
        List<TravelTask> tasks = travelTaskPlanner.plan(intent);

        // 4. 工具执行
        List<ToolResult> toolResults = toolExecutor.execute(tasks);

        // 5. 证据归一化
        List<TravelEvidence> evidences = evidenceNormalizer.normalize(toolResults);

        // 6. 候选方案生成
        List<TravelCandidatePlan> candidates = candidatePlanGenerator.generate(intent, evidences);

        // 7. 方案评分
        List<ScoredTravelPlan> scoredPlans = travelScorer.score(candidates, intent, evidences);

        // 8. 行程生成
        TravelPlan finalPlan = itineraryPlanner.generate(intent, scoredPlans, evidences);

        // 9. 提醒生成
        List<TravelReminder> reminders = reminderGenerator.generate(intent, finalPlan);

        // 10. 一图流文案生成
        ImageBrief imageBrief = imageBriefGenerator.generate(finalPlan);

        return TravelPlanResponse.success(intent, tasks, evidences, scoredPlans, finalPlan, reminders, imageBrief);
    }
}
```

---

## 10. Spring AI Alibaba 配置示例

### 10.1 application.yml

```yaml
spring:
  application:
    name: travel-planner-agent

  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
          temperature: 0.4

travel-agent:
  search:
    provider: web-search
    api-key: ${SEARCH_API_KEY}
  weather:
    provider: qweather
    api-key: ${WEATHER_API_KEY}
  map:
    provider: amap
    api-key: ${AMAP_API_KEY}
  planning:
    max-candidate-plan-count: 3
    max-daily-attraction-count: 3
    max-daily-cross-city-count: 1
```

---

### 10.2 Tool 示例

```java
@Component
public class WeatherTool {

    private final WeatherApiClient weatherApiClient;

    public WeatherTool(WeatherApiClient weatherApiClient) {
        this.weatherApiClient = weatherApiClient;
    }

    @Tool(description = "查询指定城市在指定日期范围内的天气信息")
    public WeatherResult queryWeather(WeatherQuery query) {
        return weatherApiClient.query(query.city(), query.startDate(), query.endDate());
    }
}
```

```java
@Component
public class AttractionInfoTool {

    private final SearchApiClient searchApiClient;

    public AttractionInfoTool(SearchApiClient searchApiClient) {
        this.searchApiClient = searchApiClient;
    }

    @Tool(description = "查询景点开放时间、预约要求、门票信息和节假日风险")
    public AttractionInfoResult queryAttractionInfo(AttractionQuery query) {
        return searchApiClient.searchAttraction(query.keyword(), query.city());
    }
}
```

---

## 11. Prompt 文件设计

### 11.1 travel-intent-parser.st

```text
你是一个旅行需求解析器。
请把用户输入解析成严格 JSON，不要输出任何解释。

用户输入：
{message}

输出 JSON 字段：
{
  "departureCity": "",
  "dateText": "",
  "days": null,
  "peopleCount": null,
  "budget": null,
  "destinationPreferences": [],
  "travelStyles": [],
  "transportPreference": "",
  "hotelBudgetPerNight": null,
  "avoidPlaces": []
}

规则：
1. 没提到的字段填 null 或空数组。
2. 不要编造用户没有说的信息。
3. 预算如果用户说的是总预算，填 budget。
4. 如果用户表达“不想太累”，加入 travelStyles。
```

---

### 11.2 travel-task-planner.st

```text
你是旅行查询任务拆解器。
请根据结构化旅行需求，拆解出需要查询的任务。
只输出 JSON 数组。

旅行需求：
{intentJson}

可用任务类型：
WEATHER
TRANSPORT
HOTEL
ATTRACTION
ROUTE
GENERAL_WEB

输出格式：
[
  {
    "taskType": "",
    "query": "",
    "city": "",
    "priority": 1
  }
]

规则：
1. 优先生成天气、交通、酒店、景点、路线任务。
2. 不要生成无法执行的任务。
3. 每个任务 query 要清晰可搜索。
4. 最多生成 8 个任务。
```

---

### 11.3 itinerary-planner.st

```text
你是一个谨慎的旅行规划师。
你需要根据用户需求、候选方案评分和证据信息，生成可执行的旅行计划。

用户需求：
{intentJson}

候选方案：
{scoredPlansJson}

证据信息：
{evidencesJson}

输出 JSON：
{
  "title": "",
  "summary": "",
  "route": [],
  "dailyPlans": [],
  "transportSuggestions": [],
  "hotelSuggestions": [],
  "budgetEstimate": {},
  "risks": [],
  "todoList": []
}

规则：
1. 不要安排过满。
2. 每天景点不超过 3 个。
3. 节假日要考虑排队和预约。
4. 没有证据支持的信息要写“需二次确认”。
5. 不要编造具体余票和具体酒店价格。
```

---

## 12. 分阶段实施计划

### Phase 0：项目初始化

目标：

搭建基础工程。

交付：

```text
Spring Boot 项目
Spring AI Alibaba 依赖
DashScope 配置
基础 Controller
统一返回 Result
全局异常处理
TraceId 日志
```

验收：

```text
POST /api/ai/chat 可以成功调用通义千问
接口能正常返回模型结果
```

---

### Phase 1：需求解析与追问

目标：

完成用户输入到 TravelIntent 的转换。

交付：

```text
TravelIntentParser
MissingInfoChecker
TravelIntent DTO
TravelPlanRequest
TravelPlanResponse
```

验收：

输入：

```text
五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累
```

输出能识别：

```text
出发地：西安
时间：五一
天数：4
人数：2
预算：3000
偏好：不想太累
目的地偏好：杭州、上海周边
```

---

### Phase 2：任务拆解与工具执行

目标：

完成 TravelIntent 到查询任务，再到工具结果。

交付：

```text
TravelTaskPlanner
ToolExecutor
WebSearchTool
WeatherTool
AttractionInfoTool
TravelTask
ToolResult
```

验收：

```text
能自动生成 WEATHER / TRANSPORT / HOTEL / ATTRACTION / ROUTE 任务
能调用至少 2 个真实工具
能返回工具结果
```

---

### Phase 3：证据归一化

目标：

把工具结果整理成统一证据。

交付：

```text
EvidenceNormalizer
TravelEvidence
travel_evidence 表
```

验收：

```text
每个工具结果都能转成 TravelEvidence
证据包含 type、summary、source、confidence、fetchedAt
```

---

### Phase 4：候选方案与评分

目标：

生成 2～3 个候选方案并评分。

交付：

```text
CandidatePlanGenerator
TravelScorer
TravelCandidatePlan
TravelScore
```

验收：

```text
能生成多个候选路线
每个路线有总分
每个路线有分项分
能够说明推荐理由
```

---

### Phase 5：行程生成

目标：

生成完整旅行计划。

交付：

```text
ItineraryPlanner
TravelPlan
每日行程
交通建议
住宿建议
预算估算
风险提示
```

验收：

```text
能输出完整 4 天行程
每天有上午 / 下午 / 晚上安排
有预算估算
有风险提示
有“需二次确认”标记
```

---

### Phase 6：提醒与一图流文案

目标：

让方案更可执行、更适合展示。

交付：

```text
ReminderGenerator
ImageBriefGenerator
TravelReminder
ImageBrief
```

验收：

```text
能生成购票提醒
能生成景点预约提醒
能生成出发准备提醒
能生成一图流文案结构
```

---

### Phase 7：前端 MVP

目标：

做出可演示页面。

页面：

```text
旅行需求输入页
生成过程页
行程详情页
候选方案对比页
提醒事项页
一图流预览页
```

验收：

```text
用户能输入旅行需求
前端能展示生成过程
前端能展示最终行程
前端能展示候选方案评分
前端能展示提醒事项和一图流文案
```

---

## 13. 最小验收 Demo

### 13.1 输入

```text
五一从西安出发，4天，两个人，预算3000，想去杭州上海周边，不想太累。
```

### 13.2 系统执行

```text
1. 解析需求
2. 判断信息完整
3. 拆解查询任务
4. 查询天气、景点、交通、酒店建议
5. 归一化证据
6. 生成候选路线
7. 评分排序
8. 生成最终行程
9. 生成提醒事项
10. 生成一图流文案
```

### 13.3 输出

```text
推荐路线：杭州 + 南浔 + 上海 4天3晚

推荐原因：
路线顺路、跨城强度适中、适合预算有限且不想太累的用户。

每日行程：
Day1：西安 → 杭州，西湖轻松游
Day2：灵隐寺 / 杭州轻量游，下午前往南浔
Day3：南浔 → 上海，上海 citywalk
Day4：上海轻松逛，返程

预算估算：
交通、住宿、餐饮、门票分项估算。

风险提示：
节假日车票紧张、热门景点需预约、酒店价格可能上涨。

提醒事项：
出发前 15 天看票
出发前 7 天预约景点
出发前 1 天检查证件和行李

一图流文案：
生成适合前端排版的图片内容结构。
```

---

## 14. 测试计划

### 14.1 单元测试

覆盖：

```text
TravelIntentParser
MissingInfoChecker
TravelTaskPlanner
EvidenceNormalizer
TravelScorer
ReminderGenerator
```

重点测试：

```text
缺少出发地是否追问
缺少预算是否默认经济型
不想太累是否降低高强度方案分
每天跨城超过 1 次是否降分
缺少证据时是否标记需二次确认
```

---

### 14.2 集成测试

覆盖接口：

```text
POST /api/travel/plans
POST /api/travel/plans/stream
GET /api/travel/plans/{planId}
```

重点测试：

```text
完整输入是否返回完整计划
缺失输入是否返回追问
工具失败是否降级处理
模型输出异常是否兜底
```

---

### 14.3 人工验收

准备 5 组样例：

```text
西安 → 杭州上海 4天
西安 → 重庆武隆 3天
西安 → 成都青城山 3天
北京 → 南京苏州 4天
广州 → 桂林阳朔 3天
```

每组检查：

```text
路线是否顺
预算是否合理
行程是否过满
风险是否提示
提醒是否可执行
```

---

## 15. 风险与兜底策略

### 15.1 模型胡编

策略：

```text
所有事实性信息必须来自工具结果。
无证据信息必须标记“需二次确认”。
禁止编造具体余票、酒店价格、预约名额。
```

---

### 15.2 工具调用失败

策略：

```text
单个工具失败不终止全流程。
返回部分结果。
在风险提示中说明该项需要用户二次确认。
```

---

### 15.3 搜索结果质量差

策略：

```text
同一任务尽量多源查询。
证据归一化时保留来源和抓取时间。
优先使用官方、地图、天气、OTA、景区平台信息。
```

---

### 15.4 行程安排过满

策略：

```text
每天景点不超过 3 个。
每天跨城不超过 1 次。
连续高强度天数降分。
节假日热门景点预留排队时间。
```

---

## 16. 推荐开发顺序

```text
第 1 步：搭建 Spring Boot + Spring AI Alibaba 工程
第 2 步：调通 DashScope 普通聊天接口
第 3 步：实现 TravelIntentParser
第 4 步：实现 MissingInfoChecker
第 5 步：实现 TravelTaskPlanner
第 6 步：实现 WebSearchTool / WeatherTool
第 7 步：实现 EvidenceNormalizer
第 8 步：实现 CandidatePlanGenerator
第 9 步：实现 TravelScorer
第 10 步：实现 ItineraryPlanner
第 11 步：实现 ReminderGenerator
第 12 步：实现 ImageBriefGenerator
第 13 步：落库 travel_plan / travel_task / travel_evidence / travel_reminder
第 14 步：实现 SSE 流式生成
第 15 步：开发前端 MVP 页面
```

---

## 17. 最终交付物清单

### 后端交付物

```text
TravelPlanController
TravelPlanningApplicationService
TravelAgentOrchestrator
TravelIntentParser
MissingInfoChecker
TravelTaskPlanner
ToolExecutor
WeatherTool
WebSearchTool
AttractionInfoTool
EvidenceNormalizer
CandidatePlanGenerator
TravelScorer
ItineraryPlanner
ReminderGenerator
ImageBriefGenerator
数据库表结构
Prompt 文件
接口文档
测试用例
```

### 前端交付物

```text
旅行需求输入页
生成过程展示
行程详情展示
方案评分展示
提醒事项展示
一图流文案展示
```

### 文档交付物

```text
接口文档
模块设计文档
Prompt 设计文档
测试验收清单
部署说明
```

---

## 18. 一句话总结

这个 MVP 的核心不是 RAG，而是：

```text
实时检索 + 工具调用 + 信息结构化 + 规则评分 + 行程生成
```

最小闭环只要先跑通：

```text
用户输入
→ 需求解析
→ 任务拆解
→ 工具查询
→ 证据归一化
→ 方案评分
→ 行程生成
→ 提醒生成
→ 一图流文案
```

这个链路跑通后，再逐步扩展真实票务、酒店、地图、RAG、MCP、用户偏好和图片生成。
