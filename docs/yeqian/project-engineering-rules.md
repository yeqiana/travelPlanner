# 项目工程规则

## 结论

本文档沉淀 Travel Planner Agent 项目的长期工程约定。后续新增功能、修复问题、补充文档时，应优先遵守本文档，并在规则变化时同步更新。

## 适用范围

- Java 代码生成与修改。
- Spring Bean 注入方式。
- 配置文件拆分与敏感信息处理。
- OpenAPI / Swagger 接口文档维护。
- 需要跨批次复用的业务规则、接口契约和工程约定。

## Java 代码规则

- Spring Bean 注入统一使用 `@Resource`。
- 禁止新生成或新修改代码使用 `@Autowired` 注入。
- 禁止新生成或新修改代码使用 Lombok `@RequiredArgsConstructor` 做 Bean 构造注入，除非明确要求。
- 每个 Java 类、接口、枚举都必须有中文注释，说明职责和用途。
- 每个方法都必须有中文注释，说明方法作用。
- 方法有参数和返回值时，注释中必须说明参数和返回值含义。
- Controller、Service、Mapper、DTO、VO、Config 等类必须有中文简介。

## 配置规则

- 公共默认配置放在 `src/main/resources/application.yml`。
- 开发环境自己的配置放在 `src/main/resources/application-dev.yml`。
- 当前项目默认启用 `dev` 环境。
- 真实数据库密码、API Key、Token 等敏感信息必须通过环境变量或配置中心注入。
- 示例配置可以写占位值，但不能写真实密钥。

## OpenAPI / Swagger 规则

- API 文档描述的是对外 API 契约，不是数据库表说明书。
- Spring Boot 项目优先使用 `springdoc-openapi`。
- Controller、Request DTO、Response DTO、VO、统一响应和错误响应优先作为 Swagger 文档承载对象。
- 不要为了生成文档而批量给数据库 Entity 添加 Swagger 注解。
- 不要把数据库 Entity 当作对外 API 文档模型。

## 文档沉淀规则

出现以下情况时，必须评估是否新增或更新 `docs` 文档：

- 新增跨模块复用的字段规范、状态枚举、接口契约。
- 新增会影响多个模块的业务规则或评分规则。
- 新增需要在后续批次继续复用的工程约定。
- 新增失败降级、fallback、兼容策略等非直观规则。
- 同一概念在对话中被反复解释两次及以上。

文档优先落在 `docs/yeqian/` 或 `docs/architecture/` 下，命名应明确表达主题，例如：

- `*-spec.md`
- `*-contract.md`
- `*-rules.md`
- `*-acceptance.md`

## 修改后自检

生成或修改代码后，需要至少检查：

- 是否出现新增 `@Autowired`。
- 新增或修改的 Java 类、接口、枚举是否都有中文注释。
- 新增或修改的方法是否都有中文注释。
- 是否需要同步更新接口文档、配置说明或验收记录。
- 如果无法验证，必须说明原因。
