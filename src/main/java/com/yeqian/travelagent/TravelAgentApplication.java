package com.yeqian.travelagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
    public static void main(String[] args) throws UnknownHostException {

        SpringApplication application = new SpringApplication(TravelAgentApplication.class);
        application.setAllowBeanDefinitionOverriding(true);
        application.setAllowCircularReferences(true);
        Environment env = application.run(args).getEnvironment();
        String protocol = "http";
        String ssl = "server.ssl.key-store";
        if (env.getProperty(ssl) != null) {
            protocol = "https";
        }
//        log.info("\n----------------------------------------------------------\n\t" +
//                        "Application '{}' is running! Access URLs:\n\t" +
//                        "Local: \t\t{}://localhost:{}\n\t" +
//                        "External: \t{}://{}:{}\n\t" +
//                        "Profile(s): \t{}\n\t" +
//                        "Api Docs: {}\n " +
//                        "----------------------------------------------------------",
//                env.getProperty("spring.application.name"),
//                protocol,
//                env.getProperty("server.port") != null ? env.getProperty("server.port") : "8080",
//                protocol,
//                InetAddress.getLocalHost().getHostAddress(),
//                env.getProperty("server.port") != null ? env.getProperty("server.port") : "8080",
//                env.getActiveProfiles().length > 0 ? env.getActiveProfiles() : new String[]{"default"},
//                "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + env.getProperty("server.port") + "/comet-ui.html"
//        );
    }
}
