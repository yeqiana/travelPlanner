package com.yeqian.travelagent.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置。
 *
 * <p>集中维护 Swagger 文档的基础信息，避免在业务代码中重复声明项目级文档元数据。</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * 创建 OpenAPI 文档基础配置。
     *
     * @return OpenAPI 文档基础配置
     */
    @Bean
    public OpenAPI travelPlannerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Travel Planner Agent API")
                        .description("旅游规划 Agent MVP 接口文档，面向前后端联调、接口自测和 AI 辅助理解接口契约。")
                        .version("0.0.1")
                        .contact(new Contact()
                                .name("叶纤 yeqian"))
                        .license(new License()
                                .name("Internal Use")));
    }
}
