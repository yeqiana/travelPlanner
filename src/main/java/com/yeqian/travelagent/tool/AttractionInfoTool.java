package com.yeqian.travelagent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.AttractionToolPayload;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import com.yeqian.travelagent.infrastructure.client.SearchApiClient;
import jakarta.annotation.Resource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * 真实景点信息查询工具。
 *
 * <p>通过搜索 API 查询景点开放、预约、门票和节假日风险信息，失败时降级到 mock 景点工具。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AttractionInfoTool implements TravelTool {

    @Resource
    private SearchApiClient searchApiClient;

    @Resource
    private MockAttractionInfoTool mockAttractionInfoTool;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 判断是否支持景点任务。
     *
     * @param taskType 任务类型
     * @return 支持景点任务返回 true
     */
    @Override
    public boolean supports(TravelTaskType taskType) {
        return TravelTaskType.ATTRACTION == taskType;
    }

    /**
     * 执行景点信息查询，失败时返回结构化降级结果。
     *
     * @param task 旅行查询任务
     * @return 工具调用结果
     */
    @Override
    public ToolResult execute(TravelTask task) {
        try {
            String response = searchApiClient.searchAttractionInfo(task.city(), task.query());
            AttractionToolPayload payload = parseSearchResponse(task, response);
            return new ToolResult(task.taskType(), source(), true, serialize(payload), null, OffsetDateTime.now());
        } catch (Exception exception) {
            ToolResult fallback = mockAttractionInfoTool.execute(task);
            AttractionToolPayload payload = fallbackPayload(task, exception.getMessage());
            return new ToolResult(
                    task.taskType(),
                    source() + " -> " + fallback.source(),
                    true,
                    serialize(payload),
                    "景点信息 API 调用失败，已降级到 mock：" + exception.getMessage(),
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
        return "AttractionInfoTool";
    }

    /**
     * 解析搜索响应为景点工具载荷。
     *
     * @param task 旅行查询任务
     * @param response 搜索响应
     * @return 景点工具载荷
     */
    private AttractionToolPayload parseSearchResponse(TravelTask task, String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode bestResult = bestResult(root.path("results"));
            String sourceUrl = bestResult.path("url").asText("");
            String content = bestResult.path("content").asText(root.path("answer").asText(""));
            SourceLevel sourceLevel = sourceLevel(sourceUrl);
            boolean official = sourceLevel == SourceLevel.OFFICIAL;
            boolean uncertain = !official || content.isBlank();
            String openTime = uncertain ? "需二次确认" : extractOpenTime(content);
            String ticketInfo = uncertain ? "需二次确认" : extractTicketInfo(content);
            boolean reservationRequired = content.contains("预约") || content.contains("限流") || content.contains("实名");
            String holidayRisk = content.contains("五一") || content.contains("节假日") || content.contains("人流") ? "HIGH" : "MEDIUM";
            return new AttractionToolPayload(
                    attractionName(task),
                    city(task),
                    openTime,
                    reservationRequired,
                    ticketInfo,
                    holidayRisk,
                    sourceUrl,
                    sourceName(sourceLevel),
                    "SUCCESS",
                    false,
                    uncertain,
                    confidence(sourceLevel, uncertain),
                    "",
                    OffsetDateTime.now()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("解析景点搜索结果失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 获取搜索结果中的最优结果。
     *
     * @param results 搜索结果数组
     * @return 最优搜索结果
     */
    private JsonNode bestResult(JsonNode results) {
        if (!results.isArray() || results.isEmpty()) {
            return objectMapper.createObjectNode();
        }
        JsonNode best = results.get(0);
        SourceLevel bestLevel = sourceLevel(best.path("url").asText(""));
        for (JsonNode result : results) {
            SourceLevel currentLevel = sourceLevel(result.path("url").asText(""));
            if (currentLevel.weight > bestLevel.weight) {
                best = result;
                bestLevel = currentLevel;
            }
        }
        return best;
    }

    /**
     * 判断来源可信度等级。
     *
     * @param url 来源链接
     * @return 来源可信度等级
     */
    private SourceLevel sourceLevel(String url) {
        String normalizedUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);
        if (normalizedUrl.contains("gov.cn") || normalizedUrl.contains("wlj") || normalizedUrl.contains("culture")) {
            return SourceLevel.OFFICIAL;
        }
        if (normalizedUrl.contains("amap.com") || normalizedUrl.contains("baidu.com") || normalizedUrl.contains("map")) {
            return SourceLevel.MAP;
        }
        if (normalizedUrl.contains("ctrip.com") || normalizedUrl.contains("fliggy") || normalizedUrl.contains("meituan")) {
            return SourceLevel.OTA;
        }
        return SourceLevel.GUIDE;
    }

    /**
     * 获取景点名称。
     *
     * @param task 旅行查询任务
     * @return 景点名称
     */
    private String attractionName(TravelTask task) {
        return city(task) + "热门景点";
    }

    /**
     * 获取城市名称。
     *
     * @param task 旅行查询任务
     * @return 城市名称
     */
    private String city(TravelTask task) {
        return task.city() == null || task.city().isBlank() ? "目的地" : task.city();
    }

    /**
     * 提取开放时间信息。
     *
     * @param content 搜索内容
     * @return 开放时间信息
     */
    private String extractOpenTime(String content) {
        return content.contains("开放") || content.contains("营业") ? "以官方平台当日开放时间为准" : "需二次确认";
    }

    /**
     * 提取门票信息。
     *
     * @param content 搜索内容
     * @return 门票信息
     */
    private String extractTicketInfo(String content) {
        return content.contains("门票") || content.contains("票") ? "以官方平台实时门票规则为准" : "需二次确认";
    }

    /**
     * 计算置信度。
     *
     * @param sourceLevel 来源可信度等级
     * @param uncertain 是否不确定
     * @return 置信度
     */
    private double confidence(SourceLevel sourceLevel, boolean uncertain) {
        double value = switch (sourceLevel) {
            case OFFICIAL -> 0.82;
            case MAP -> 0.68;
            case OTA -> 0.6;
            case GUIDE -> 0.45;
        };
        return uncertain ? Math.min(value, 0.55) : value;
    }

    /**
     * 获取来源名称。
     *
     * @param sourceLevel 来源可信度等级
     * @return 来源名称
     */
    private String sourceName(SourceLevel sourceLevel) {
        return switch (sourceLevel) {
            case OFFICIAL -> "OFFICIAL";
            case MAP -> "MAP_PLATFORM";
            case OTA -> "OTA_PLATFORM";
            case GUIDE -> "GUIDE_SITE";
        };
    }

    /**
     * 构造降级景点载荷。
     *
     * @param task 旅行查询任务
     * @param failureReason 失败原因
     * @return 景点工具载荷
     */
    private AttractionToolPayload fallbackPayload(TravelTask task, String failureReason) {
        return new AttractionToolPayload(
                attractionName(task),
                city(task),
                "需二次确认",
                true,
                "需二次确认",
                "HIGH",
                "",
                "MockAttractionInfoTool",
                "FALLBACK",
                true,
                true,
                0.35,
                failureReason == null ? "景点信息 API 不可用" : failureReason,
                OffsetDateTime.now()
        );
    }

    /**
     * 序列化景点工具载荷。
     *
     * @param payload 景点工具载荷
     * @return JSON 文本
     */
    private String serialize(AttractionToolPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("序列化景点工具结果失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 来源可信度等级。
     */
    private enum SourceLevel {
        OFFICIAL(4),
        MAP(3),
        OTA(2),
        GUIDE(1);

        private final int weight;

        /**
         * 创建来源可信度等级。
         *
         * @param weight 排序权重
         */
        SourceLevel(int weight) {
            this.weight = weight;
        }
    }
}
