# Java 代码风格规则

## 结论

本项目 Java 代码以清晰、稳定、可维护为优先。新生成和新修改的代码必须遵守本文档，避免同一项目内出现多套注入方式和注释风格。

## Bean 注入规则

- Spring Bean 注入统一使用 `@Resource`。
- 禁止新增 `@Autowired`。
- 禁止默认使用 Lombok `@RequiredArgsConstructor` 做 Bean 构造注入，除非明确要求。
- 字段名应表达依赖职责，不使用过短或含义不明的命名。

示例：

```java
@Resource
private TravelPlanningApplicationService travelPlanningApplicationService;
```

## 中文注释规则

- 每个 Java 类、接口、枚举都必须有中文注释。
- 类注释需要说明职责和用途。
- 每个方法都必须有中文注释。
- 方法注释需要说明方法作用。
- 方法有参数和返回值时，需要说明参数和返回值含义。
- Controller、Service、Mapper、DTO、VO、Config 等类必须有中文简介。

示例：

```java
/**
 * 旅行计划控制器。
 *
 * <p>提供旅行计划生成、多轮补充和历史计划查询接口。</p>
 */
public class TravelPlanController {

    /**
     * 创建旅行计划。
     *
     * @param request 旅行计划请求
     * @return 统一旅行计划响应
     */
    public Result<TravelPlanResponse> createPlan(TravelPlanRequest request) {
        return Result.success(null);
    }
}
```

## 修改规则

- 修复问题时优先定位根因，不只做表面绕过。
- 不随意重命名公共接口、路由、DTO、数据库字段、配置项。
- 不随意引入新框架、新依赖或大规模重构。
- 不删除已有测试，除非明确说明原因并补充替代测试。
- 改动应小步、清晰、可回滚。

## 自检规则

生成或修改 Java 代码后，必须检查：

- 是否出现新增 `@Autowired`。
- 是否出现未经要求的 `@RequiredArgsConstructor` Bean 构造注入。
- 新增或修改的类、接口、枚举是否都有中文注释。
- 新增或修改的方法是否都有中文注释。
- 是否需要更新接口文档、配置示例或验收文档。
