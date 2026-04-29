# Travel Agent 本地运行说明

## 结论

本地运行必须通过环境变量配置敏感信息，不能把真实 key、数据库密码或第三方凭据写入 `application-dev.yml`。

## JDK

```powershell
$env:JAVA_HOME="D:\soft\jdk17.0.18"
```

## 必要配置

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/travel_planner_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:SPRING_DATASOURCE_USERNAME="your_username"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
```

## 可选配置

```powershell
$env:DASHSCOPE_API_KEY="your_model_api_key"
$env:DASHSCOPE_CHAT_MODEL="doubao-seed-2-0-pro-260215"
$env:OPENAI_COMPATIBLE_BASE_URL="https://ark.cn-beijing.volces.com/api/v3"
$env:SEARCH_API_KEY="your_tavily_key"
$env:WEATHER_API_KEY="your_openweathermap_key"
$env:AMAP_API_KEY="your_amap_key"
```

模型、天气、搜索 key 不配置时，旅行规划主链路仍可运行：模型解析会走规则兜底，天气和搜索工具会走 mock 降级。

## 启动

```powershell
mvn spring-boot:run
```

## 验证

```powershell
mvn test
mvn clean package
```

## Batch 2 工具降级说明

路线工具使用 `AMAP_API_KEY` 调用地图 API，景点工具使用 `SEARCH_API_KEY` 调用搜索 API。未配置 key、接口超时或返回异常时，工具会降级到 mock，主流程继续执行；ROUTE / ATTRACTION evidence 会在 `keyFacts` 中标记 `sourceStatus`、`fallback`、`needSecondConfirm`、`confidence` 和 `failureReason`。
