package com.yeqian.travelagent.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.infrastructure.config.TravelAgentProperties;
import jakarta.annotation.Resource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 天气 API 客户端。
 *
 * <p>负责调用 OpenWeatherMap 当前天气接口，并把响应转换为旅行规划可直接使用的摘要。</p>
 */
@Component
public class WeatherApiClient {

    @Resource
    private TravelAgentProperties travelAgentProperties;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 查询指定城市当前天气。
     *
     * @param city 城市名称
     * @return 天气摘要文本
     */
    public String queryCurrentWeather(String city) {
        TravelAgentProperties.Weather config = travelAgentProperties.getExternalApi().getWeather();
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置天气 API Key");
        }
        String response = restClient(config.getBaseUrl(), config.getTimeoutMillis())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/2.5/weather")
                        .queryParam("q", city)
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric")
                        .queryParam("lang", "zh_cn")
                        .build())
                .retrieve()
                .body(String.class);
        return summarizeWeather(response);
    }

    /**
     * 把天气接口原始响应转换为摘要文本。
     *
     * @param response 天气接口原始响应
     * @return 天气摘要文本
     */
    private String summarizeWeather(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String cityName = root.path("name").asText("目的地");
            JsonNode weather = root.path("weather").isArray() && !root.path("weather").isEmpty()
                    ? root.path("weather").get(0)
                    : objectMapper.createObjectNode();
            JsonNode main = root.path("main");
            JsonNode wind = root.path("wind");
            return String.format(
                    "%s 实时天气：%s，当前 %.1f 度，体感 %.1f 度，湿度 %d%%，风速 %.1f m/s。天气信息来自 OpenWeatherMap，出发前建议二次确认。",
                    cityName,
                    weather.path("description").asText("暂无天气描述"),
                    main.path("temp").asDouble(),
                    main.path("feels_like").asDouble(),
                    main.path("humidity").asInt(),
                    wind.path("speed").asDouble()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("解析天气 API 响应失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 创建带超时设置的 REST 客户端。
     *
     * @param baseUrl 基础地址
     * @param timeoutMillis 超时时间，单位毫秒
     * @return REST 客户端
     */
    private RestClient restClient(String baseUrl, int timeoutMillis) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofMillis(Math.max(timeoutMillis, 100));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
