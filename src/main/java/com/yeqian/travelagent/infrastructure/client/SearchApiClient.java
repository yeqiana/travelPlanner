package com.yeqian.travelagent.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.infrastructure.config.TravelAgentProperties;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Web 搜索 API 客户端。
 *
 * <p>负责调用 Tavily 搜索接口，并把搜索结果整理为旅行规划证据摘要。</p>
 */
@Component
public class SearchApiClient {

    @Resource
    private TravelAgentProperties travelAgentProperties;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 搜索通用旅行信息。
     *
     * @param query 搜索关键词
     * @return 搜索摘要文本
     */
    public String search(String query) {
        TravelAgentProperties.Search config = travelAgentProperties.getExternalApi().getSearch();
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置搜索 API Key");
        }
        String response = restClient(config.getBaseUrl(), config.getTimeoutMillis())
                .post()
                .uri("/search")
                .headers(headers -> headers.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody(query, config.getMaxResults()))
                .retrieve()
                .body(String.class);
        return summarizeSearch(response);
    }

    /**
     * 构造搜索请求体。
     *
     * @param query 搜索关键词
     * @param maxResults 搜索结果数量上限
     * @return 搜索请求体
     */
    private Map<String, Object> requestBody(String query, int maxResults) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("search_depth", "basic");
        body.put("include_answer", true);
        body.put("max_results", Math.max(maxResults, 1));
        return body;
    }

    /**
     * 把搜索接口原始响应转换为摘要文本。
     *
     * @param response 搜索接口原始响应
     * @return 搜索摘要文本
     */
    private String summarizeSearch(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            StringBuilder builder = new StringBuilder("实时搜索摘要：");
            String answer = root.path("answer").asText("");
            if (!answer.isBlank()) {
                builder.append(answer).append(" ");
            }
            JsonNode results = root.path("results");
            if (results.isArray()) {
                int index = 1;
                for (JsonNode result : results) {
                    builder.append(index++)
                            .append(". ")
                            .append(result.path("title").asText("未命名结果"))
                            .append(" - ")
                            .append(result.path("content").asText(""))
                            .append(" 来源：")
                            .append(result.path("url").asText(""))
                            .append(" ");
                }
            }
            builder.append("搜索信息来自 Tavily，关键政策、票务和营业时间仍需二次确认。");
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("解析搜索 API 响应失败：" + exception.getMessage(), exception);
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
