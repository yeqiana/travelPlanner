# OpenAPI Swagger 文档规则

## 结论

Swagger 文档用于描述对外 API 契约，服务于前后端联调、接口自测和 AI 辅助理解接口结构。它不负责解释数据库表结构，也不应让业务代码被文档注解大规模污染。

## 集成规则

- Spring Boot 项目优先使用 `springdoc-openapi`。
- Spring Boot 3.x 使用 `springdoc-openapi-starter-webmvc-ui`。
- Swagger UI 默认访问地址为 `/swagger-ui.html`。
- OpenAPI JSON 默认访问地址为 `/v3/api-docs`。
- 文档扫描范围应限制在对外 Controller 包和 `/api/**` 路径。

## 注解范围

优先补充 Swagger 注解的位置：

- Controller：使用 `@Tag`、`@Operation`、`@ApiResponse` 描述接口分组、用途和响应。
- Request DTO：使用 `@Schema` 描述请求字段含义、示例和必填约束。
- Response DTO / VO：使用 `@Schema` 描述响应字段含义。
- 统一响应对象：说明业务状态码、消息和数据结构。
- 错误响应：在 Controller 或全局异常约定中说明常见错误状态。

不建议补充 Swagger 注解的位置：

- 数据库 Entity。
- Mapper。
- Repository。
- 纯内部领域对象。
- 只服务内部计算过程的临时模型。

## 字段描述规则

- 字段说明应描述对外接口含义，不描述数据库列实现细节。
- 示例值应使用接近真实业务的示例，不能使用真实密钥、手机号、身份证号等敏感信息。
- 可为空字段需要在说明中明确表达可为空场景。
- 枚举字段需要说明可选值含义；如果枚举会跨接口复用，应单独沉淀枚举规范文档。

## 变更规则

新增或修改接口时，需要同步评估：

- 是否需要更新 Controller 的 `@Operation` 和 `@ApiResponse`。
- 是否需要更新 Request DTO / Response DTO 的 `@Schema`。
- 是否需要更新 `docs/yeqian/api/` 下的接口文档。
- 是否需要补充 curl 示例或验收记录。

## 验证规则

接口文档改动后，至少执行一次：

```powershell
$env:JAVA_HOME="D:\soft\jdk17.0.18"
mvn test
```

如果本地启动服务，还应访问：

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`
