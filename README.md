# Travel Planner Agent MVP

旅行规划 Agent MVP，基于 Spring Boot + Spring AI 兼容 ChatClient + 规则兜底 + 工具调用，完成从用户自然语言输入到旅行计划生成、提醒、一图流文案和历史查询的最小闭环。

## 当前能力

- `POST /api/travel/plans`：生成旅行计划。
- `GET /api/travel/plans/{planId}`：按计划编号查询历史计划。
- `POST /api/ai/chat`：基础 AI 聊天调试接口。
- 支持旅行需求解析、缺失信息追问、`sessionId` 多轮补充、任务拆解、工具执行、证据归一化、候选方案、评分、行程、提醒、一图流文案和落库。
- 天气和搜索工具未配置真实 key 或调用失败时，会降级到 mock，不影响主链路测试。

## 环境变量

本项目禁止在 `application-dev.yml` 中写入真实密钥、数据库密码或第三方凭据。开发环境请通过环境变量注入：

```powershell
$env:JAVA_HOME="D:\soft\jdk17.0.18"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/travel_planner_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:SPRING_DATASOURCE_USERNAME="your_username"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
$env:DEEPSEEK_API_KEY="your_deepseek_api_key"
$env:DEEPSEEK_BASE_URL="https://api.deepseek.com"
$env:DEEPSEEK_CHAT_MODEL="deepseek-v4-flash"
$env:SEARCH_API_KEY="your_tavily_key"
$env:WEATHER_API_KEY="your_openweathermap_key"
$env:AMAP_API_KEY="your_amap_key"
```

如果使用 IntelliJ IDEA 启动，需要在 Run Configuration 的 `Environment variables` 中加入 `DEEPSEEK_API_KEY`，PowerShell 当前窗口里的临时环境变量不会自动传给已经打开的 IDE。

模型 key 未配置或调用失败时，旅行需求解析会走规则兜底；`/api/ai/chat` 会返回明确失败信息。

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

## OpenAPI / Swagger

启动项目后访问：

- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

接口文档只扫描 `/api/**` 对外接口，优先描述 Controller、Request DTO、Response DTO 和统一错误响应，不把数据库 Entity 作为对外 API 文档模型。

## 快速测试

完整输入：

```powershell
curl -X POST "http://localhost:8080/api/travel/plans" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"五一从西安出发去杭州和上海周边玩4天，两个人，预算5000，不想太累\"}"
```

多轮追问第一轮：

```powershell
curl -X POST "http://localhost:8080/api/travel/plans" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"五一想出去玩4天，两个人，预算3000，不想太累\"}"
```

返回 `needClarification=true` 和 `sessionId` 后，第二轮携带同一个 `sessionId`：

```powershell
curl -X POST "http://localhost:8080/api/travel/plans" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"我从西安出发，想去杭州上海周边\",\"sessionId\":\"上一步返回的sessionId\"}"
```

更多说明见：

- `docs/yeqian/project-engineering-rules.md`
- `docs/yeqian/backend-observability-rules.md`
- `docs/yeqian/java-code-style-rules.md`
- `docs/yeqian/openapi-swagger-rules.md`
- `docs/yeqian/travel-agent-stage-progress.md`
- `docs/yeqian/travel-agent-local-run.md`
- `docs/yeqian/travel-agent-multiturn-clarification.md`
- `docs/yeqian/api/旅游规划Agent接口文档.md`
- `docs/yeqian/test/旅游规划AgentMVP验收记录.md`
