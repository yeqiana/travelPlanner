# 旅游规划 Agent 本地运行指南

## 1. 环境要求

- JDK 17
- Maven 3.6+
- MySQL 8.x
- Windows PowerShell

本机验证时使用：

```powershell
$env:JAVA_HOME="D:\soft\jdk17.0.18"
```

## 2. 环境变量

```powershell
$env:WEATHER_API_KEY="你的 OpenWeatherMap Key"
$env:SEARCH_API_KEY="你的 Tavily Key"
```

如果不配置天气或搜索 Key，主流程不会中断，会降级到 mock 工具。

模型配置位于 `src/main/resources/application-dev.yml`，当前使用 OpenAI 兼容 ChatClient 配置。模型不可用时，旅行规划主链路会使用规则兜底解析；`/api/ai/chat` 依赖模型服务。

## 3. 数据库初始化 SQL

执行文件：

```text
docs/phase7_minimal_persistence.sql
```

核心表：

```sql
CREATE TABLE IF NOT EXISTS travel_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id VARCHAR(64) NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_travel_plan_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

完整 SQL 请以 `docs/phase7_minimal_persistence.sql` 为准。

## 4. 启动命令

```powershell
$env:JAVA_HOME="D:\soft\jdk17.0.18"
mvn spring-boot:run
```

默认访问地址：

```text
http://localhost:8080
```

## 5. 测试命令

```powershell
$env:JAVA_HOME="D:\soft\jdk17.0.18"
mvn clean test
mvn clean package
```

## 6. curl 验收

完整输入：

```powershell
curl -X POST "http://localhost:8080/api/travel/plans" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"五一从西安出发去杭州和上海周边玩4天，两个人，预算5000，不想太累\",\"sessionId\":\"demo\"}"
```

缺少出发地：

```powershell
curl -X POST "http://localhost:8080/api/travel/plans" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"五一去杭州玩4天\"}"
```

查询历史：

```powershell
curl "http://localhost:8080/api/travel/plans/{planId}"
```

AI 聊天：

```powershell
curl -X POST "http://localhost:8080/api/ai/chat" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"你好\"}"
```

## 7. 示例输出要点

完整计划响应应包含：

- `planId`
- `needClarification=false`
- `intent`
- `tasks`
- `evidences`
- `candidatePlans`
- `recommendedPlan.dailyPlans`
- `score`
- `reminders`
- `imageBrief`
- `risks`
- `createdAt`

缺少关键字段时响应应包含：

- `needClarification=true`
- `clarificationQuestions`
- 已解析出的部分 `intent`

## 8. 当前 mock 模块

- `MockTransportSearchTool`
- `MockHotelSearchTool`
- `MockAttractionInfoTool`
- `MockRouteTool`
- 真实天气和真实搜索失败后的 fallback mock

## 9. 当前真实 API 模块

- `WeatherTool`：调用 OpenWeatherMap 当前天气接口。
- `WebSearchTool`：调用 Tavily 搜索接口。

## 10. 已知限制

- 暂不支持 `POST /api/travel/plans/stream`。
- 暂不接真实票务下单和酒店预订。
- 暂不新增 RAG、MCP、前端或复杂权限系统。
- 数据库不可用会导致生成接口在落库阶段失败。
