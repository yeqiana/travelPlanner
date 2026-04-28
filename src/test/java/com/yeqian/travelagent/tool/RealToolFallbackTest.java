package com.yeqian.travelagent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yeqian.travelagent.agent.normalizer.EvidenceNormalizer;
import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelTask;
import com.yeqian.travelagent.infrastructure.client.SearchApiClient;
import com.yeqian.travelagent.infrastructure.client.WeatherApiClient;
import com.yeqian.travelagent.infrastructure.config.TravelAgentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实工具接入和降级测试。
 */
class RealToolFallbackTest {

    /**
     * 验证有天气 API Key 时会调用真实天气客户端。
     */
    @Test
    void shouldCallWeatherApiWhenApiKeyExists() throws IOException {
        HttpServer server = startServer("/data/2.5/weather", """
                {"name":"杭州","weather":[{"description":"多云"}],"main":{"temp":24.5,"feels_like":25.1,"humidity":62},"wind":{"speed":2.3}}
                """, 0);
        try {
            WeatherApiClient client = weatherApiClient(propertiesWithWeather(server, "test-key"));

            String result = client.queryCurrentWeather("杭州");

            assertThat(result).contains("杭州 实时天气").contains("多云").contains("OpenWeatherMap");
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证有搜索 API Key 时会调用真实搜索客户端。
     */
    @Test
    void shouldCallSearchApiWhenApiKeyExists() throws IOException {
        HttpServer server = startServer("/search", """
                {"answer":"杭州五一游客较多。","results":[{"title":"杭州旅游提示","content":"热门景点建议预约。","url":"https://example.com/hz"}]}
                """, 0);
        try {
            SearchApiClient client = searchApiClient(propertiesWithSearch(server, "test-key"));

            String result = client.search("杭州 五一 旅游");

            assertThat(result).contains("杭州五一游客较多").contains("杭州旅游提示").contains("Tavily");
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证无天气 API Key 时工具不会崩溃，并降级到 mock 结果。
     */
    @Test
    void shouldFallbackToMockWhenWeatherApiKeyMissing() {
        WeatherTool tool = weatherTool(weatherApiClient(new TravelAgentProperties()));
        TravelTask task = task(TravelTaskType.WEATHER);

        ToolResult result = tool.execute(task);

        assertThat(result.success()).isTrue();
        assertThat(result.source()).contains("WeatherTool").contains("MockWeatherTool");
        assertThat(result.errorMessage()).contains("未配置天气 API Key");
    }

    /**
     * 验证天气 API 超时时不会影响工具返回，并降级到 mock 结果。
     */
    @Test
    void shouldFallbackToMockWhenWeatherApiTimeout() throws IOException {
        HttpServer server = startServer("/data/2.5/weather", """
                {"name":"杭州","weather":[{"description":"晴"}],"main":{"temp":25,"feels_like":25,"humidity":50},"wind":{"speed":1}}
                """, 500);
        try {
            TravelAgentProperties properties = propertiesWithWeather(server, "test-key");
            properties.getExternalApi().getWeather().setTimeoutMillis(100);
            WeatherTool tool = weatherTool(weatherApiClient(properties));

            ToolResult result = tool.execute(task(TravelTaskType.WEATHER));

            assertThat(result.success()).isTrue();
            assertThat(result.source()).contains("MockWeatherTool");
            assertThat(result.errorMessage()).contains("真实天气 API 调用失败");
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证降级后的工具结果仍可进入证据归一化器。
     */
    @Test
    void shouldNormalizeFallbackToolResult() {
        WebSearchTool tool = webSearchTool(searchApiClient(new TravelAgentProperties()));
        TravelTask task = task(TravelTaskType.GENERAL_WEB);

        ToolResult result = tool.execute(task);
        List<TravelEvidence> evidences = new EvidenceNormalizer().normalize(List.of(result), List.of(task));

        assertThat(evidences).hasSize(1);
        assertThat(evidences.get(0).sourceName()).contains("WebSearchTool").contains("MockWebSearchTool");
        assertThat(evidences.get(0).summary()).contains("mock 搜索摘要");
    }

    /**
     * 构造测试旅行任务。
     *
     * @param taskType 任务类型
     * @return 测试旅行任务
     */
    private TravelTask task(TravelTaskType taskType) {
        return new TravelTask(taskType, "杭州 五一 旅游", "杭州", 1);
    }

    /**
     * 构造真实天气工具。
     *
     * @param weatherApiClient 天气 API 客户端
     * @return 真实天气工具
     */
    private WeatherTool weatherTool(WeatherApiClient weatherApiClient) {
        WeatherTool tool = new WeatherTool();
        ReflectionTestUtils.setField(tool, "weatherApiClient", weatherApiClient);
        ReflectionTestUtils.setField(tool, "mockWeatherTool", new MockWeatherTool());
        return tool;
    }

    /**
     * 构造真实搜索工具。
     *
     * @param searchApiClient 搜索 API 客户端
     * @return 真实搜索工具
     */
    private WebSearchTool webSearchTool(SearchApiClient searchApiClient) {
        WebSearchTool tool = new WebSearchTool();
        ReflectionTestUtils.setField(tool, "searchApiClient", searchApiClient);
        ReflectionTestUtils.setField(tool, "mockWebSearchTool", new MockWebSearchTool());
        return tool;
    }

    /**
     * 构造天气 API 客户端。
     *
     * @param properties 配置属性
     * @return 天气 API 客户端
     */
    private WeatherApiClient weatherApiClient(TravelAgentProperties properties) {
        WeatherApiClient client = new WeatherApiClient();
        ReflectionTestUtils.setField(client, "travelAgentProperties", properties);
        ReflectionTestUtils.setField(client, "objectMapper", new ObjectMapper());
        return client;
    }

    /**
     * 构造搜索 API 客户端。
     *
     * @param properties 配置属性
     * @return 搜索 API 客户端
     */
    private SearchApiClient searchApiClient(TravelAgentProperties properties) {
        SearchApiClient client = new SearchApiClient();
        ReflectionTestUtils.setField(client, "travelAgentProperties", properties);
        ReflectionTestUtils.setField(client, "objectMapper", new ObjectMapper());
        return client;
    }

    /**
     * 构造天气 API 测试配置。
     *
     * @param server 本地测试服务
     * @param apiKey API Key
     * @return 测试配置
     */
    private TravelAgentProperties propertiesWithWeather(HttpServer server, String apiKey) {
        TravelAgentProperties properties = new TravelAgentProperties();
        properties.getExternalApi().getWeather().setApiKey(apiKey);
        properties.getExternalApi().getWeather().setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.getExternalApi().getWeather().setTimeoutMillis(1000);
        return properties;
    }

    /**
     * 构造搜索 API 测试配置。
     *
     * @param server 本地测试服务
     * @param apiKey API Key
     * @return 测试配置
     */
    private TravelAgentProperties propertiesWithSearch(HttpServer server, String apiKey) {
        TravelAgentProperties properties = new TravelAgentProperties();
        properties.getExternalApi().getSearch().setApiKey(apiKey);
        properties.getExternalApi().getSearch().setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.getExternalApi().getSearch().setTimeoutMillis(1000);
        properties.getExternalApi().getSearch().setMaxResults(2);
        return properties;
    }

    /**
     * 启动本地测试 HTTP 服务。
     *
     * @param path 请求路径
     * @param response 响应内容
     * @param delayMillis 延迟时间，单位毫秒
     * @return 本地测试 HTTP 服务
     */
    private HttpServer startServer(String path, String response, long delayMillis) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> handle(exchange, response, delayMillis));
        server.start();
        return server;
    }

    /**
     * 处理本地测试 HTTP 请求。
     *
     * @param exchange HTTP 交换对象
     * @param response 响应内容
     * @param delayMillis 延迟时间，单位毫秒
     */
    private void handle(HttpExchange exchange, String response, long delayMillis) throws IOException {
        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
