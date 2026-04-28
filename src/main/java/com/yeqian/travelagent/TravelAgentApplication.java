package com.yeqian.travelagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 旅游规划 Agent 应用启动类。
 *
 * <p>负责启动 Spring Boot 应用，并扫描配置属性。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TravelAgentApplication {

    /**
     * 启动旅行规划 Agent 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TravelAgentApplication.class, args);
    }
}
