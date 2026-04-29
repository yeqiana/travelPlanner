package com.yeqian.travelagent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.GeoPoint;
import com.yeqian.travelagent.domain.model.RouteQuery;
import com.yeqian.travelagent.domain.model.RouteToolPayload;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import com.yeqian.travelagent.infrastructure.client.MapApiClient;
import jakarta.annotation.Resource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 真实地图路线查询工具。
 *
 * <p>优先调用地图 API 进行地理编码和路线查询，失败时降级到 mock 路线工具并明确标记降级状态。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MapRouteTool implements TravelTool {

    @Resource
    private MapApiClient mapApiClient;

    @Resource
    private MockRouteTool mockRouteTool;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 判断是否支持路线任务。
     *
     * @param taskType 任务类型
     * @return 支持路线任务返回 true
     */
    @Override
    public boolean supports(TravelTaskType taskType) {
        return TravelTaskType.ROUTE == taskType;
    }

    /**
     * 执行地图路线查询，失败时返回结构化降级结果。
     *
     * @param task 旅行查询任务
     * @return 工具调用结果
     */
    @Override
    public ToolResult execute(TravelTask task) {
        RouteEndpoints endpoints = parseRouteEndpoints(task);
        try {
            GeoPoint originLocation = mapApiClient.geocode(endpoints.origin());
            GeoPoint destinationLocation = mapApiClient.geocode(endpoints.destination());
            RouteQuery query = new RouteQuery(
                    endpoints.origin(),
                    endpoints.destination(),
                    originLocation,
                    destinationLocation,
                    "",
                    "DRIVING"
            );
            RouteToolPayload payload = mapApiClient.queryRoute(query);
            return new ToolResult(task.taskType(), source(), true, serialize(payload), null, OffsetDateTime.now());
        } catch (Exception exception) {
            ToolResult fallback = mockRouteTool.execute(task);
            RouteToolPayload payload = fallbackPayload(endpoints, exception.getMessage());
            return new ToolResult(
                    task.taskType(),
                    source() + " -> " + fallback.source(),
                    true,
                    serialize(payload),
                    "地图路线 API 调用失败，已降级到 mock：" + exception.getMessage(),
                    OffsetDateTime.now()
            );
        }
    }

    /**
     * 获取工具来源名称。
     *
     * @return 工具来源名称
     */
    @Override
    public String source() {
        return "MapRouteTool";
    }

    /**
     * 从任务中提取出发地和目的地。
     *
     * @param task 旅行查询任务
     * @return 路线端点
     */
    private RouteEndpoints parseRouteEndpoints(TravelTask task) {
        String query = task.query() == null ? "" : task.query();
        Matcher matcher = Pattern.compile("(.+?)到(.+?)(\\s|路线|交通|$)").matcher(query);
        if (matcher.find()) {
            return new RouteEndpoints(cleanPlace(matcher.group(1)), cleanPlace(matcher.group(2)));
        }
        String city = task.city() == null || task.city().isBlank() ? "目的地" : task.city();
        return new RouteEndpoints("出发地", city);
    }

    /**
     * 清理地点文本。
     *
     * @param place 原始地点文本
     * @return 清理后的地点文本
     */
    private String cleanPlace(String place) {
        if (place == null || place.isBlank()) {
            return "未知地点";
        }
        return place.replace("出发地", "")
                .replace("路线可行性", "")
                .replace("城市顺序", "")
                .trim();
    }

    /**
     * 构造降级路线载荷。
     *
     * @param endpoints 路线端点
     * @param failureReason 失败原因
     * @return 路线工具载荷
     */
    private RouteToolPayload fallbackPayload(RouteEndpoints endpoints, String failureReason) {
        return new RouteToolPayload(
                endpoints.origin(),
                endpoints.destination(),
                null,
                null,
                "路线耗时和距离需二次确认；当前使用 mock 路线建议，节假日请预留额外时间。",
                "MEDIUM",
                "MockRouteTool",
                "FALLBACK",
                true,
                true,
                0.35,
                failureReason == null ? "地图路线 API 不可用" : failureReason,
                OffsetDateTime.now()
        );
    }

    /**
     * 序列化路线工具载荷。
     *
     * @param payload 路线工具载荷
     * @return JSON 文本
     */
    private String serialize(RouteToolPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("序列化路线工具结果失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 路线端点。
     *
     * @param origin 出发地
     * @param destination 目的地
     */
    private record RouteEndpoints(String origin, String destination) {
    }
}
