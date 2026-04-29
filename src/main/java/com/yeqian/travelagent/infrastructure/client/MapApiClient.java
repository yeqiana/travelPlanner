package com.yeqian.travelagent.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.domain.model.GeoPoint;
import com.yeqian.travelagent.domain.model.RouteQuery;
import com.yeqian.travelagent.domain.model.RouteToolPayload;
import com.yeqian.travelagent.infrastructure.config.TravelAgentProperties;
import jakarta.annotation.Resource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * 地图 API 客户端。
 *
 * <p>负责调用高德地图地理编码和路线查询接口，并转换为路线工具可使用的结构化结果。</p>
 */
@Component
public class MapApiClient {

    @Resource
    private TravelAgentProperties travelAgentProperties;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 查询地点对应的经纬度。
     *
     * @param place 地点名称
     * @return 地理坐标点
     */
    public GeoPoint geocode(String place) {
        TravelAgentProperties.Amap config = travelAgentProperties.getExternalApi().getAmap();
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置高德地图 API Key");
        }
        if (place == null || place.isBlank()) {
            throw new IllegalArgumentException("地理编码地点不能为空");
        }

        String response = restClient(config.getBaseUrl(), config.getTimeoutMillis())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/geocode/geo")
                        .queryParam("address", place)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(String.class);
        return parseGeocode(response, place);
    }

    /**
     * 查询两地之间的路线。
     *
     * @param query 路线查询参数
     * @return 路线工具结构化载荷
     */
    public RouteToolPayload queryRoute(RouteQuery query) {
        TravelAgentProperties.Amap config = travelAgentProperties.getExternalApi().getAmap();
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置高德地图 API Key");
        }
        if (query == null || query.originLocation() == null || query.destinationLocation() == null) {
            throw new IllegalArgumentException("路线查询坐标不能为空");
        }

        String response = restClient(config.getBaseUrl(), config.getTimeoutMillis())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/direction/driving")
                        .queryParam("origin", query.originLocation().location())
                        .queryParam("destination", query.destinationLocation().location())
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(String.class);
        return parseRoute(response, query);
    }

    /**
     * 解析地理编码响应。
     *
     * @param response 接口原始响应
     * @param place 地点名称
     * @return 地理坐标点
     */
    private GeoPoint parseGeocode(String response, String place) {
        try {
            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                throw new IllegalStateException(root.path("info").asText("地理编码失败"));
            }
            JsonNode geocodes = root.path("geocodes");
            if (!geocodes.isArray() || geocodes.isEmpty()) {
                throw new IllegalStateException("未查询到地点坐标：" + place);
            }
            JsonNode first = geocodes.get(0);
            String location = first.path("location").asText("");
            String[] parts = location.split(",");
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalStateException("地理编码坐标格式异常：" + location);
            }
            return new GeoPoint(parts[0], parts[1], first.path("formatted_address").asText(place));
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException) {
                throw (IllegalStateException) exception;
            }
            throw new IllegalStateException("解析地理编码响应失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 解析路线响应。
     *
     * @param response 接口原始响应
     * @param query 路线查询参数
     * @return 路线工具结构化载荷
     */
    private RouteToolPayload parseRoute(String response, RouteQuery query) {
        try {
            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                throw new IllegalStateException(root.path("info").asText("路线查询失败"));
            }
            JsonNode paths = root.path("route").path("paths");
            if (!paths.isArray() || paths.isEmpty()) {
                throw new IllegalStateException("未查询到路线方案");
            }
            JsonNode first = paths.get(0);
            int durationMinutes = Math.max(1, (int) Math.ceil(first.path("duration").asDouble(0) / 60.0));
            double distanceKm = Math.round(first.path("distance").asDouble(0) / 10.0) / 100.0;
            String routeRisk = durationMinutes > 180 ? "HIGH" : durationMinutes > 90 ? "MEDIUM" : "LOW";
            return new RouteToolPayload(
                    query.origin(),
                    query.destination(),
                    durationMinutes,
                    distanceKm,
                    "路线耗时来自高德地图，节假日建议预留额外时间并二次确认实时路况。",
                    routeRisk,
                    "AMAP",
                    "SUCCESS",
                    false,
                    false,
                    0.85,
                    "",
                    OffsetDateTime.now()
            );
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException) {
                throw (IllegalStateException) exception;
            }
            throw new IllegalStateException("解析路线响应失败：" + exception.getMessage(), exception);
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
