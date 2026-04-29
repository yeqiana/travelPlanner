package com.yeqian.travelagent.agent.normalizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yeqian.travelagent.domain.enums.EvidenceType;
import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.AttractionToolPayload;
import com.yeqian.travelagent.domain.model.RouteToolPayload;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelTask;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 证据归一化器。
 *
 * <p>把工具调用结果转换为统一证据结构，并保留来源、状态、置信度和二次确认提示。</p>
 */
@Component
public class EvidenceNormalizer {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 批量归一化工具结果。
     *
     * @param toolResults 工具调用结果列表
     * @return 旅行证据列表
     */
    public List<TravelEvidence> normalize(List<ToolResult> toolResults) {
        return normalize(toolResults, List.of());
    }

    /**
     * 按查询任务批量归一化工具结果。
     *
     * @param toolResults 工具调用结果列表
     * @param tasks 查询任务列表
     * @return 旅行证据列表
     */
    public List<TravelEvidence> normalize(List<ToolResult> toolResults, List<TravelTask> tasks) {
        if (toolResults == null || toolResults.isEmpty()) {
            return List.of();
        }
        return IntStream.range(0, toolResults.size())
                .mapToObj(index -> normalizeOne(toolResults.get(index), taskAt(tasks, index)))
                .toList();
    }

    /**
     * 归一化单个工具结果。
     *
     * @param result 工具调用结果
     * @param task 查询任务
     * @return 旅行证据
     */
    private TravelEvidence normalizeOne(ToolResult result, TravelTask task) {
        if (result.taskType() == TravelTaskType.ROUTE) {
            return normalizeRoute(result, task);
        }
        if (result.taskType() == TravelTaskType.ATTRACTION) {
            return normalizeAttraction(result, task);
        }
        return normalizeGeneric(result, task);
    }

    /**
     * 归一化路线工具结果。
     *
     * @param result 工具调用结果
     * @param task 查询任务
     * @return 路线证据
     */
    private TravelEvidence normalizeRoute(ToolResult result, TravelTask task) {
        EvidenceType evidenceType = EvidenceType.ROUTE;
        if (!result.success()) {
            Map<String, Object> keyFacts = failedRouteFacts(result, task);
            return evidence(evidenceType, task, "路线查询失败，路线耗时和距离需二次确认。", keyFacts, result);
        }
        try {
            RouteToolPayload payload = mapper().readValue(result.rawContent(), RouteToolPayload.class);
            Map<String, Object> keyFacts = routeFacts(payload, result, task);
            String summary = "路线建议：" + keyFacts.get("origin") + " 到 " + keyFacts.get("destination")
                    + "，耗时约 " + keyFacts.get("durationMinutes") + " 分钟，路线风险 " + keyFacts.get("routeRisk") + "。";
            return evidence(evidenceType, task, summary, keyFacts, result);
        } catch (Exception exception) {
            Map<String, Object> keyFacts = legacyRouteFacts(result, task, exception.getMessage());
            return evidence(evidenceType, task, result.rawContent(), keyFacts, result);
        }
    }

    /**
     * 归一化景点工具结果。
     *
     * @param result 工具调用结果
     * @param task 查询任务
     * @return 景点证据
     */
    private TravelEvidence normalizeAttraction(ToolResult result, TravelTask task) {
        EvidenceType evidenceType = EvidenceType.ATTRACTION;
        if (!result.success()) {
            Map<String, Object> keyFacts = failedAttractionFacts(result, task);
            return evidence(evidenceType, task, "景点信息查询失败，开放、预约和门票信息需二次确认。", keyFacts, result);
        }
        try {
            AttractionToolPayload payload = mapper().readValue(result.rawContent(), AttractionToolPayload.class);
            Map<String, Object> keyFacts = attractionFacts(payload, result, task);
            String summary = keyFacts.get("attractionName") + "开放与预约信息："
                    + keyFacts.get("openTime") + "，门票：" + keyFacts.get("ticketInfo") + "。";
            return evidence(evidenceType, task, summary, keyFacts, result);
        } catch (Exception exception) {
            Map<String, Object> keyFacts = legacyAttractionFacts(result, task, exception.getMessage());
            return evidence(evidenceType, task, result.rawContent(), keyFacts, result);
        }
    }

    /**
     * 归一化通用工具结果。
     *
     * @param result 工具调用结果
     * @param task 查询任务
     * @return 旅行证据
     */
    private TravelEvidence normalizeGeneric(ToolResult result, TravelTask task) {
        EvidenceType evidenceType = EvidenceType.valueOf(result.taskType().name());
        boolean needSecondConfirm = !result.success() || safeText(result.rawContent()).contains("二次确认");
        Map<String, Object> keyFacts = new LinkedHashMap<>();
        keyFacts.put("needSecondConfirm", needSecondConfirm);
        keyFacts.put("success", result.success());
        keyFacts.put("query", task == null ? "" : safeText(task.query()));
        return new TravelEvidence(
                evidenceType,
                task == null ? null : task.city(),
                buildTitle(evidenceType, task == null ? null : task.city()),
                result.success() ? result.rawContent() : result.errorMessage(),
                keyFacts,
                result.success() ? 0.75 : 0.2,
                result.source(),
                null,
                result.fetchedAt()
        );
    }

    /**
     * 构造路线关键事实。
     *
     * @param payload 路线工具载荷
     * @param result 工具调用结果
     * @param task 查询任务
     * @return 关键事实
     */
    private Map<String, Object> routeFacts(RouteToolPayload payload, ToolResult result, TravelTask task) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("origin", defaultText(payload.origin(), "出发地"));
        facts.put("destination", defaultText(payload.destination(), task == null ? "目的地" : task.city()));
        facts.put("durationMinutes", defaultInteger(payload.durationMinutes()));
        facts.put("distanceKm", defaultDouble(payload.distanceKm()));
        facts.put("transferSuggestion", defaultText(payload.transferSuggestion(), "路线信息需二次确认"));
        facts.put("routeRisk", defaultText(payload.routeRisk(), "MEDIUM"));
        addStatusFacts(facts, payload.source(), payload.sourceStatus(), payload.fallback(), payload.needSecondConfirm(), payload.confidence(), payload.failureReason(), result);
        return facts;
    }

    /**
     * 构造景点关键事实。
     *
     * @param payload 景点工具载荷
     * @param result 工具调用结果
     * @param task 查询任务
     * @return 关键事实
     */
    private Map<String, Object> attractionFacts(AttractionToolPayload payload, ToolResult result, TravelTask task) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("attractionName", defaultText(payload.attractionName(), cityText(task) + "热门景点"));
        facts.put("city", defaultText(payload.city(), cityText(task)));
        facts.put("openTime", defaultText(payload.openTime(), "需二次确认"));
        facts.put("reservationRequired", payload.reservationRequired() == null ? true : payload.reservationRequired());
        facts.put("ticketInfo", defaultText(payload.ticketInfo(), "需二次确认"));
        facts.put("holidayRisk", defaultText(payload.holidayRisk(), "HIGH"));
        facts.put("sourceUrl", defaultText(payload.officialSourceUrl(), ""));
        addStatusFacts(facts, payload.source(), payload.sourceStatus(), payload.fallback(), payload.needSecondConfirm(), payload.confidence(), payload.failureReason(), result);
        return facts;
    }

    /**
     * 构造失败路线关键事实。
     *
     * @param result 工具调用结果
     * @param task 查询任务
     * @return 关键事实
     */
    private Map<String, Object> failedRouteFacts(ToolResult result, TravelTask task) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("origin", "出发地");
        facts.put("destination", cityText(task));
        facts.put("durationMinutes", 0);
        facts.put("distanceKm", 0.0);
        facts.put("transferSuggestion", "路线查询失败，需二次确认");
        facts.put("routeRisk", "HIGH");
        addStatusFacts(facts, result.source(), "FAILED", false, true, 0.2, result.errorMessage(), result);
        return facts;
    }

    /**
     * 构造失败景点关键事实。
     *
     * @param result 工具调用结果
     * @param task 查询任务
     * @return 关键事实
     */
    private Map<String, Object> failedAttractionFacts(ToolResult result, TravelTask task) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("attractionName", cityText(task) + "热门景点");
        facts.put("city", cityText(task));
        facts.put("openTime", "需二次确认");
        facts.put("reservationRequired", true);
        facts.put("ticketInfo", "需二次确认");
        facts.put("holidayRisk", "HIGH");
        facts.put("sourceUrl", "");
        addStatusFacts(facts, result.source(), "FAILED", false, true, 0.2, result.errorMessage(), result);
        return facts;
    }

    /**
     * 构造旧文本路线关键事实。
     *
     * @param result 工具调用结果
     * @param task 查询任务
     * @param failureReason 解析失败原因
     * @return 关键事实
     */
    private Map<String, Object> legacyRouteFacts(ToolResult result, TravelTask task, String failureReason) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("origin", "出发地");
        facts.put("destination", cityText(task));
        facts.put("durationMinutes", 0);
        facts.put("distanceKm", 0.0);
        facts.put("transferSuggestion", defaultText(result.rawContent(), "路线信息需二次确认"));
        facts.put("routeRisk", "MEDIUM");
        addStatusFacts(facts, result.source(), "FALLBACK", true, true, 0.35, failureReason, result);
        return facts;
    }

    /**
     * 构造旧文本景点关键事实。
     *
     * @param result 工具调用结果
     * @param task 查询任务
     * @param failureReason 解析失败原因
     * @return 关键事实
     */
    private Map<String, Object> legacyAttractionFacts(ToolResult result, TravelTask task, String failureReason) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("attractionName", cityText(task) + "热门景点");
        facts.put("city", cityText(task));
        facts.put("openTime", "需二次确认");
        facts.put("reservationRequired", true);
        facts.put("ticketInfo", "需二次确认");
        facts.put("holidayRisk", "HIGH");
        facts.put("sourceUrl", "");
        addStatusFacts(facts, result.source(), "FALLBACK", true, true, 0.35, failureReason, result);
        return facts;
    }

    /**
     * 补充通用来源状态字段。
     *
     * @param facts 关键事实
     * @param source 来源
     * @param sourceStatus 来源状态
     * @param fallback 是否降级
     * @param needSecondConfirm 是否需要二次确认
     * @param confidence 置信度
     * @param failureReason 失败原因
     * @param result 工具调用结果
     */
    private void addStatusFacts(
            Map<String, Object> facts,
            String source,
            String sourceStatus,
            Boolean fallback,
            Boolean needSecondConfirm,
            Double confidence,
            String failureReason,
            ToolResult result
    ) {
        facts.put("source", defaultText(source, result.source()));
        facts.put("sourceStatus", defaultText(sourceStatus, result.success() ? "SUCCESS" : "FAILED"));
        facts.put("fallback", fallback == null ? false : fallback);
        facts.put("needSecondConfirm", needSecondConfirm == null ? false : needSecondConfirm);
        facts.put("confidence", confidence == null ? (result.success() ? 0.75 : 0.2) : confidence);
        facts.put("failureReason", defaultText(failureReason, ""));
    }

    /**
     * 构造旅行证据。
     *
     * @param evidenceType 证据类型
     * @param task 查询任务
     * @param summary 摘要
     * @param keyFacts 关键事实
     * @param result 工具调用结果
     * @return 旅行证据
     */
    private TravelEvidence evidence(EvidenceType evidenceType, TravelTask task, String summary, Map<String, Object> keyFacts, ToolResult result) {
        return new TravelEvidence(
                evidenceType,
                task == null ? null : task.city(),
                buildTitle(evidenceType, task == null ? null : task.city()),
                summary,
                keyFacts,
                ((Number) keyFacts.getOrDefault("confidence", result.success() ? 0.75 : 0.2)).doubleValue(),
                result.source(),
                defaultText((String) keyFacts.getOrDefault("sourceUrl", ""), null),
                result.fetchedAt()
        );
    }

    /**
     * 按下标获取查询任务。
     *
     * @param tasks 查询任务列表
     * @param index 下标
     * @return 查询任务，缺失时返回 null
     */
    private TravelTask taskAt(List<TravelTask> tasks, int index) {
        if (tasks == null || index >= tasks.size()) {
            return null;
        }
        return tasks.get(index);
    }

    /**
     * 生成证据标题。
     *
     * @param evidenceType 证据类型
     * @param city 关联城市
     * @return 证据标题
     */
    private String buildTitle(EvidenceType evidenceType, String city) {
        String prefix = city == null || city.isBlank() ? "" : city + " ";
        return switch (evidenceType) {
            case WEATHER -> prefix + "天气证据";
            case TRANSPORT -> prefix + "交通证据";
            case HOTEL -> prefix + "住宿证据";
            case ATTRACTION -> prefix + "景点证据";
            case ROUTE -> prefix + "路线证据";
            case GENERAL_WEB -> prefix + "通用网页证据";
        };
    }

    /**
     * 获取城市文本。
     *
     * @param task 查询任务
     * @return 城市文本
     */
    private String cityText(TravelTask task) {
        return task == null || task.city() == null || task.city().isBlank() ? "目的地" : task.city();
    }

    /**
     * 获取非空文本。
     *
     * @param value 原始文本
     * @return 非空文本
     */
    private String safeText(String value) {
        return value == null ? "" : value;
    }

    /**
     * 获取带默认值的文本。
     *
     * @param value 原始文本
     * @param defaultValue 默认文本
     * @return 规整后的文本
     */
    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 获取带默认值的整数。
     *
     * @param value 原始整数
     * @return 规整后的整数
     */
    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 获取带默认值的小数。
     *
     * @param value 原始小数
     * @return 规整后的小数
     */
    private double defaultDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    /**
     * 获取 JSON 解析器。
     *
     * @return JSON 解析器
     */
    private ObjectMapper mapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
        }
        return objectMapper;
    }
}
