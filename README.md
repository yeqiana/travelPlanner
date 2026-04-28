# Travel Planner Agent MVP

旅游规划 Agent MVP，基于 Spring Boot + Spring AI 兼容 ChatClient + 规则兜底 + 工具调用，完成从用户自然语言输入到旅行计划生成、提醒、一图流文案和历史查询的最小闭环。

## 当前能力

- `POST /api/travel/plans`：生成旅行计划。
- `GET /api/travel/plans/{planId}`：按计划编号查询历史计划。
- `POST /api/ai/chat`：基础 AI 聊天调试接口。
- 旅行需求解析、缺失信息追问、任务拆解、工具执行、证据归一化、候选方案、评分、行程、提醒、一图流文案、落库。
- 真实天气 API：OpenWeatherMap，未配置或失败时降级到 mock。
- 真实搜索 API：Tavily，未配置或失败时降级到 mock。

## 环境变量

```powershell
$env:JAVA_HOME="D:\soft\jdk17.0.18"
$env:WEATHER_API_KEY="你的 OpenWeatherMap Key"
$env:SEARCH_API_KEY="你的 Tavily Key"
```

`application-dev.yml` 当前使用 Spring AI OpenAI 兼容配置。模型 Key 未配置或调用失败时，旅行需求解析会走规则兜底；`/api/ai/chat` 会返回明确失败信息。

## 数据库初始化

执行：

```sql
source docs/phase7_minimal_persistence.sql;
```

或直接复制 `docs/phase7_minimal_persistence.sql` 中的建表 SQL 到 MySQL 执行。

## 启动

```powershell
$env:JAVA_HOME="D:\soft\jdk17.0.18"
mvn spring-boot:run
```

默认端口：`8080`。

## 快速测试

```powershell
curl -X POST "http://localhost:8080/api/travel/plans" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"五一从西安出发去杭州和上海周边玩4天，两个人，预算5000，不想太累\",\"sessionId\":\"demo\"}"
```

更多接口、验收说明和本地运行说明见：

- `docs/yeqian/api/旅游规划Agent接口文档.md`
- `docs/yeqian/test/旅游规划AgentMVP验收记录.md`
- `docs/yeqian/dev/旅游规划Agent本地运行指南.md`
