# 配置示例

本文档提供项目配置文件的参考写法。当前项目默认启用 `dev` 环境：公共默认配置放在 `application.yml`，开发环境自己的配置放在 `application-dev.yml`。实际部署时请按当前环境复制到对应配置文件中，并避免把真实密码、密钥提交到代码仓库。

## application.yml 公共配置示例

```yaml
# 当前项目默认启用开发环境配置。
# 公共配置放在本文件中；开发环境自己的差异配置放在 application-dev.yml。
spring:
  profiles:
    active: dev

  application:
    # 应用名称。用于日志、监控、服务注册等场景识别当前服务。
    name: travel-planner

  jackson:
    # 接口返回时间的时区，避免前后端时间显示不一致。
    time-zone: Asia/Shanghai
    # 日期时间序列化格式。
    date-format: yyyy-MM-dd HH:mm:ss

logging:
  level:
    # Spring 框架日志级别。公共默认值保持收敛，环境配置可按需覆盖。
    org.springframework: WARN

travel-planner:
  # 行程规划公共业务配置。环境差异项可在 application-dev.yml / application-prod.yml 中覆盖。
  trip:
    # 默认行程天数。用户未指定天数时使用该值。
    default-days: 3
    # 单次规划允许的最大天数，防止请求过大影响响应时间。
    max-days: 30
  ai:
    # 是否启用 AI 行程生成能力。未接入模型服务时可关闭。
    enabled: false
    # 模型服务地址。生产环境建议通过环境变量覆盖。
    base-url: http://localhost:11434
    # 默认模型名称，请按实际接入模型调整。
    model: qwen2.5

travel-agent:
  planning:
    # 多轮追问 session 过期小时数。过期后不再恢复上下文，默认 24 小时。
    session-ttl-hours: ${TRAVEL_SESSION_TTL_HOURS:24}
```

## application-dev.yml 示例

```yaml
# 开发环境配置。
# 这里只放本地开发环境自己的差异项；公共默认值请放在 application.yml。
server:
  # 本地开发 HTTP 服务端口。
  port: 8080

spring:
  datasource:
    # 本地开发数据库连接地址。请按本机数据库名称、端口和参数调整。
    url: jdbc:mysql://localhost:3306/travel_planner_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    # 本地开发数据库用户名。
    username: ${SPRING_DATASOURCE_USERNAME}
    # 本地开发数据库密码。仅作为示例，真实密码不要提交到仓库。
    password: ${SPRING_DATASOURCE_PASSWORD}
    # JDBC 驱动类名。使用其他数据库时需要对应替换。
    driver-class-name: com.mysql.cj.jdbc.Driver

logging:
  level:
    # 开发环境打开项目包 DEBUG，便于排查问题。
    com.yeqian.travelagent: DEBUG
```

## application-prod.yml 示例

```yaml
# 生产环境配置建议由环境变量或配置中心注入敏感信息。
server:
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

logging:
  level:
    com.yeqian.travelagent: INFO

travel-planner:
  ai:
    enabled: ${AI_ENABLED:false}
    base-url: ${AI_BASE_URL:http://localhost:11434}
    model: ${AI_MODEL:qwen2.5}

travel-agent:
  planning:
    session-ttl-hours: ${TRAVEL_SESSION_TTL_HOURS:24}
```

## 环境变量示例

```bash
SERVER_PORT=8080
DB_URL=jdbc:mysql://127.0.0.1:3306/travel_planner?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=travel_user
DB_PASSWORD=change-me
AI_ENABLED=false
AI_BASE_URL=http://localhost:11434
AI_MODEL=qwen2.5
TRAVEL_SESSION_TTL_HOURS=24
```

## 使用建议

- 公共默认值放在 `application.yml`，开发环境差异项放在 `application-dev.yml`。
- 当前默认使用 `dev`：`spring.profiles.active: dev`。
- 生产环境使用环境变量覆盖敏感配置。
- 不要提交真实数据库密码、API Key、Token 等敏感信息。
- 新增 Travel Agent 链路配置时，建议统一放在 `travel-agent` 前缀下，并同步更新 `TravelAgentProperties`。
- 修改配置后至少启动一次应用，确认配置能被正常加载。
