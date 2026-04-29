# 旅游规划 Agent 接口文档

## 通用响应

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

失败时 `code=-1`，`message` 为错误说明。

## 1. 生成旅行计划

`POST /api/travel/plans`

请求示例：

```json
{
  "message": "五一从西安出发去杭州和上海周边玩4天，两个人，预算5000，不想太累",
  "sessionId": "demo-session"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "planId": "0b0fd5a2-3a25-43b6-9bfb-2cf3c6b79d11",
    "needClarification": false,
    "clarificationQuestions": [],
    "intent": {
      "departureCity": "西安",
      "dateText": "五一",
      "days": 4,
      "peopleCount": 2,
      "budget": 5000,
      "destinationPreferences": ["杭州", "上海周边"],
      "travelStyles": ["不想太累", "节假日"]
    },
    "tasks": [],
    "evidences": [],
    "candidatePlans": [],
    "recommendedPlan": {
      "title": "杭州 + 上海低疲劳精选 4天旅行计划",
      "dailyPlans": []
    },
    "score": {
      "score": {
        "totalScore": 86
      }
    },
    "reminders": [],
    "imageBrief": {
      "title": "杭州 + 上海低疲劳精选 4天旅行计划"
    },
    "risks": ["车票/酒店价格需二次确认。"],
    "createdAt": "2026-04-27T21:53:36+08:00"
  }
}
```

可用性：可用。  
是否需要修复：当前不需要。数据库不可用时会由全局异常返回失败，需要先初始化数据库。

## 2. 流式生成旅行计划

`POST /api/travel/plans/stream`

当前状态：未实现。  
可用性：不可用。  
是否需要修复：Phase 9 不新增大功能，暂不补 SSE。

## 3. 查询历史旅行计划

`GET /api/travel/plans/{planId}`

请求示例：

```bash
curl "http://localhost:8080/api/travel/plans/0b0fd5a2-3a25-43b6-9bfb-2cf3c6b79d11"
```

响应示例：与生成旅行计划接口的 `data` 结构一致。

可用性：可用，依赖 `travel_plan.response_json` 中保存的完整响应。  
是否需要修复：当前不需要。查询不存在的 `planId` 返回失败提示。

## 4. AI 聊天调试接口

`POST /api/ai/chat`

请求示例：

```json
{
  "message": "你好，请用一句话介绍杭州"
}
```

响应示例：

```json
{
  "success": true,
  "content": "杭州是以西湖、人文历史和江南生活气息闻名的城市。",
  "errorMessage": null
}
```

可用性：保留可用，但依赖模型配置。  
是否需要修复：模型 Key 未配置或模型服务不可用时返回明确失败信息。

## curl 验收命令

```powershell
curl -X POST "http://localhost:8080/api/travel/plans" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"五一从西安出发去杭州和上海周边玩4天，两个人，预算5000，不想太累\",\"sessionId\":\"demo\"}"

curl -X POST "http://localhost:8080/api/travel/plans" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"五一去杭州玩4天\"}"

curl "http://localhost:8080/api/travel/plans/{planId}"

curl -X POST "http://localhost:8080/api/ai/chat" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"你好\"}"
```

## 当前 mock 与真实 API

已接真实 API：

- 天气：`WeatherTool -> WeatherApiClient -> OpenWeatherMap`，失败降级到 `MockWeatherTool`。
- 搜索：`WebSearchTool -> SearchApiClient -> Tavily`，失败降级到 `MockWebSearchTool`。

仍是 mock 或规则实现：

- 交通、酒店、景点、路线工具仍为 mock。
- 候选方案、评分、提醒、一图流为规则生成。
- 行程生成当前为规则实现，不直接依赖大模型。

## 已知限制

- 不承诺真实余票、酒店价格和景点预约名额准确。
- 未实现 SSE 流式接口。
- 未接真实票务下单、酒店预订、地图深度路线规划。
- 数据库保存失败时主接口会失败，不会返回未落库的计划。

## 5. 响应契约补充样例

完整计划响应重点字段示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "planId": "0b0fd5a2-3a25-43b6-9bfb-2cf3c6b79d11",
    "sessionId": "session_demo",
    "needClarification": false,
    "clarificationQuestions": [],
    "structuredClarificationQuestions": [],
    "evidences": [
      {
        "evidenceType": "WEATHER",
        "city": "杭州",
        "title": "杭州 天气证据",
        "summary": "杭州天气晴转多云，穿衣建议：按当季轻便衣物准备，出发前复查天气。",
        "keyFacts": {
          "sourceStatus": "SUCCESS",
          "fallback": false,
          "needSecondConfirm": false,
          "confidence": 0.75,
          "weatherSummary": "杭州天气晴转多云",
          "temperatureRange": "需二次确认",
          "dressingAdvice": "按当季轻便衣物准备，出发前复查天气",
          "weatherRisk": "LOW"
        }
      },
      {
        "evidenceType": "TRANSPORT",
        "city": "杭州",
        "keyFacts": {
          "transportMode": "高铁优先",
          "durationText": "7小时",
          "costRange": "600-900元",
          "ticketRisk": "HIGH",
          "needSecondConfirm": false
        }
      }
    ],
    "imageBrief": {
      "title": "杭州 + 上海周边4天旅行计划",
      "subtitle": "低疲劳节假日路线",
      "sections": [
        {
          "title": "路线",
          "content": "西安 -> 杭州 -> 上海周边 -> 西安"
        }
      ]
    },
    "risks": ["节假日人流和票务风险需提前确认"]
  }
}
```

追问响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "planId": null,
    "sessionId": "session_6f4f0b2e",
    "needClarification": true,
    "clarificationQuestions": ["你是从哪个城市出发？"],
    "structuredClarificationQuestions": [
      {
        "field": "departureCity",
        "question": "你是从哪个城市出发？",
        "example": "例如：西安",
        "required": true
      }
    ],
    "intent": {
      "departureCity": null,
      "dateText": "五一",
      "days": 4,
      "peopleCount": 2,
      "destinationPreferences": ["杭州"]
    }
  }
}
```
