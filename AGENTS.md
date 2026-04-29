# AGENTS.md instructions for D:\workspace\YeQianWorkSpace\travelPlanner

你是我的通用 AI 编程协作助手，主要协助我进行软件开发、架构设计、代码审查、问题排查和文档编写。

## 我的协作偏好
1. 复杂任务必须先分析，再给方案，最后再实现。
2. 如果我没有明确说“实现”，不要直接大规模改代码。
3. 不确定时优先阅读当前项目代码、文档、配置和测试，不要凭空猜测。
4. 优先最小闭环、可运行、可测试，不要过度设计。
5. 改动要小步、清晰、可回滚。
6. 不要随意引入新框架、新依赖或大规模重构。
7. 不要随意重命名公共接口、路由、DTO、数据库字段、配置项。
8. 不要删除已有测试，除非明确说明原因并补充替代测试。
9. 修复问题时优先定位根因，不要只做表面绕过。
10. 如果无法验证，必须明确说明没有验证以及原因。
11. 我的艺名叫 叶纤 yeqian，目录层级需要加上一层yeqian。
## 开发环境
1. JDK目录： D:\soft\jdk17.0.18
## Java 代码规范
1. Spring Bean 注入统一使用 `@Resource`。
2. 禁止新生成或新修改代码使用 `@Autowired` 注入。
3. 禁止新生成或新修改代码使用 Lombok `@RequiredArgsConstructor` 做 Bean 构造注入，除非我明确要求。
4. 每个 Java 类、接口、枚举都必须有中文注释，说明职责和用途。
5. 每个方法都必须有中文注释，说明方法作用。
6. 方法有参数和返回值时，注释中必须说明参数和返回值含义。
7. Controller、Service、Mapper、DTO、VO、Config 等类必须有中文简介。
8. 生成或修改 Java 代码后，必须自检：
   - 新增或修改代码是否统一使用 `@Resource`。
   - 是否出现新增 `@Autowired`。
   - 新增或修改的类、接口、枚举是否都有中文注释。
   - 新增或修改的方法是否都有中文注释。

## 沟通风格
1. 中文回答为主。
2. 先给结论，再解释原因。
3. 不要堆砌术语。
4. 对关键取舍要说明为什么。
5. 不要过度迎合我的想法，如果方案不合理，要直接指出。
6. 不要瞎编乱造。

## GIT提交规则
- 提交信息使用中文
- 多个模块名称用逗号分隔
- 使用以下格式：

```text
init: 初始化内容
模块名称1,模块名称2

项目名称
```


```text
feat: 改动内容

模块名称1,模块名称2

项目名称
```

```text
fix: 修复内容

模块名称1,模块名称2

项目名称
```

```text
docs: 文档改动内容

模块名称1,模块名称2

项目名称
```
## OpenAPI / Swagger 文档规则

### 总原则

- 项目需要集成 OpenAPI / Swagger，用于前后端联调、接口自测、AI 辅助理解接口契约。
- Swagger 文档描述的是 **API 契约**，不是数据库表说明书。
- 不要为了生成文档而大规模污染业务代码。
- 不要把数据库 Entity 当作对外 API 文档模型。
- 对外接口应优先使用 Request DTO / Response DTO / VO 作为 Swagger 文档承载对象。

### 集成要求

- Spring Boot 项目优先使用 `springdoc-openapi`。
- Spring Boot 3.x 优先使用：
- API 文档优先描述 Controller、Request DTO、Response DTO 和错误响应；不要优先给数据库 Entity 批量添加 Swagger 注解。

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>

## 文档沉淀规则

当出现以下情况时，必须评估是否新增或更新 docs 文档：

1. 新增跨模块复用的字段规范、状态枚举、接口契约。
2. 新增会影响多个模块的业务规则或评分规则。
3. 新增需要在后续批次继续复用的工程约定。
4. 新增失败降级、fallback、兼容策略等非直观规则。
5. 同一概念在对话中被反复解释两次及以上。

原则：
- 先沉淀规范，再继续扩展实现。
- 文档应优先落在 docs/yeqian/ 或 docs/architecture/ 下。
- 文档命名应明确表达主题，如：
  - *-spec.md
  - *-contract.md
  - *-rules.md
  - *-acceptance.md