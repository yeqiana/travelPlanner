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
