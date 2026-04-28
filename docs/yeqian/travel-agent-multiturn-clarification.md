# Travel Agent 多轮追问说明

## 结论

Batch 1 已让 `sessionId` 参与旅行规划追问闭环：第一次缺少必要信息时返回 `sessionId`，第二次携带同一 `sessionId` 可合并上一次的部分意图。

## 必要字段

当前会阻断规划并触发追问的字段：

- `departureCity`：出发城市。
- `dateText`：出发时间文本。
- `days`：出行天数。

`peopleCount` 缺失时默认 1 人，`budget` 缺失时不阻断流程。

## 第一轮示例

```powershell
curl -X POST "http://localhost:8080/api/travel/plans" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"五一想出去玩4天，两个人，预算3000，不想太累\"}"
```

关键响应字段：

```json
{
  "sessionId": "session_xxx",
  "needClarification": true,
  "clarificationQuestions": [
    "你是从哪个城市出发？"
  ],
  "structuredClarificationQuestions": [
    {
      "field": "departureCity",
      "question": "你是从哪个城市出发？",
      "example": "例如：西安",
      "required": true
    }
  ]
}
```

## 第二轮示例

```powershell
curl -X POST "http://localhost:8080/api/travel/plans" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"我从西安出发，想去杭州上海周边\",\"sessionId\":\"session_xxx\"}"
```

系统会合并第一轮的 `dateText`、`days`、`peopleCount`、`budget`、`travelStyles`，并使用第二轮补充的 `departureCity`、`destinationPreferences`。合并后信息完整时，继续走原有完整旅行规划链路。

## 会话策略

当前 `sessionId` 上下文保存在应用内存中。应用重启、多实例部署或内存清理后，旧 `sessionId` 会失效；如果请求携带不存在的 `sessionId`，系统按新会话处理。
