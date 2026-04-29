package com.yeqian.travelagent.agent.normalizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.domain.enums.EvidenceType;
import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.AttractionToolPayload;
import com.yeqian.travelagent.domain.model.RouteToolPayload;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelTask;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 证据归一化器测试。
 */
class EvidenceNormalizerTest {

    private final EvidenceNormalizer normalizer = new EvidenceNormalizer();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    /**
     * 验证成功工具结果可以转换为统一证据结构。
     */
    @Test
    void shouldNormalizeSuccessfulToolResult() {
        TravelTask task = new TravelTask(TravelTaskType.WEATHER, "杭州 五一 天气", "杭州", 1);
        ToolResult result = new ToolResult(TravelTaskType.WEATHER, "MockWeatherTool", true, "杭州天气晴转多云", null, OffsetDateTime.now());

        List<TravelEvidence> evidences = normalizer.normalize(List.of(result), List.of(task));

        assertThat(evidences).hasSize(1);
        assertThat(evidences.get(0).evidenceType()).isEqualTo(EvidenceType.WEATHER);
        assertThat(evidences.get(0).city()).isEqualTo("杭州");
        assertThat(evidences.get(0).summary()).contains("杭州天气");
        assertThat(evidences.get(0).keyFacts()).containsEntry("success", true);
        assertThat(evidences.get(0).keyFacts()).containsKeys("weatherSummary", "temperatureRange", "dressingAdvice", "weatherRisk");
    }

    /**
     * 验证失败工具结果会保留为低置信度证据，不阻断后续规划。
     */
    @Test
    void shouldNormalizeFailedToolResultAsRiskEvidence() {
        TravelTask task = new TravelTask(TravelTaskType.HOTEL, "杭州 酒店", "杭州", 2);
        ToolResult result = new ToolResult(TravelTaskType.HOTEL, "HotelTool", false, "", "搜索接口超时", OffsetDateTime.now());

        TravelEvidence evidence = normalizer.normalize(List.of(result), List.of(task)).get(0);

        assertThat(evidence.confidence()).isEqualTo(0.2);
        assertThat(evidence.summary()).contains("搜索接口超时");
        assertThat(evidence.keyFacts()).containsEntry("needSecondConfirm", true);
        assertThat(evidence.keyFacts()).containsKeys("areaSuggestion", "budgetSuggestion", "transportConvenience", "priceReliability");
    }

    /**
     * 验证交通工具结果会归一化出结构化票务风险。
     */
    @Test
    void shouldNormalizeTransportKeyFacts() {
        TravelTask task = new TravelTask(TravelTaskType.TRANSPORT, "西安到杭州交通", "杭州", 1);
        ToolResult result = new ToolResult(TravelTaskType.TRANSPORT, "MockTransportSearchTool", true, "高铁约7小时，票价600-900元，五一票务紧张", null, OffsetDateTime.now());

        TravelEvidence evidence = normalizer.normalize(List.of(result), List.of(task)).get(0);

        assertThat(evidence.evidenceType()).isEqualTo(EvidenceType.TRANSPORT);
        assertThat(evidence.keyFacts())
                .containsEntry("transportMode", "高铁优先")
                .containsEntry("durationText", "7小时")
                .containsEntry("costRange", "600-900元")
                .containsEntry("ticketRisk", "HIGH");
    }

    /**
     * 验证路线成功载荷可以归一化为结构化 ROUTE evidence。
     */
    @Test
    void shouldNormalizeRouteSuccessPayload() throws Exception {
        RouteToolPayload payload = new RouteToolPayload("西安", "杭州", 120, 90.5, "高铁优先", "MEDIUM", "AMAP", "SUCCESS", false, false, 0.85, "", OffsetDateTime.now());
        TravelTask task = new TravelTask(TravelTaskType.ROUTE, "西安到杭州路线", "杭州", 1);
        ToolResult result = new ToolResult(TravelTaskType.ROUTE, "MapRouteTool", true, objectMapper.writeValueAsString(payload), null, OffsetDateTime.now());

        TravelEvidence evidence = normalizer.normalize(List.of(result), List.of(task)).get(0);

        assertThat(evidence.evidenceType()).isEqualTo(EvidenceType.ROUTE);
        assertThat(evidence.keyFacts())
                .containsEntry("origin", "西安")
                .containsEntry("destination", "杭州")
                .containsEntry("durationMinutes", 120)
                .containsEntry("sourceStatus", "SUCCESS")
                .containsEntry("fallback", false);
    }

    /**
     * 验证路线降级载荷可以归一化为结构化 ROUTE evidence。
     */
    @Test
    void shouldNormalizeRouteFallbackPayload() throws Exception {
        RouteToolPayload payload = new RouteToolPayload("西安", "杭州", null, null, "需二次确认", "MEDIUM", "MockRouteTool", "FALLBACK", true, true, 0.35, "timeout", OffsetDateTime.now());
        TravelTask task = new TravelTask(TravelTaskType.ROUTE, "西安到杭州路线", "杭州", 1);
        ToolResult result = new ToolResult(TravelTaskType.ROUTE, "MapRouteTool -> MockRouteTool", true, objectMapper.writeValueAsString(payload), "timeout", OffsetDateTime.now());

        TravelEvidence evidence = normalizer.normalize(List.of(result), List.of(task)).get(0);

        assertThat(evidence.keyFacts())
                .containsEntry("sourceStatus", "FALLBACK")
                .containsEntry("fallback", true)
                .containsEntry("needSecondConfirm", true)
                .containsEntry("failureReason", "timeout");
    }

    /**
     * 验证路线失败结果也会生成结构化 ROUTE evidence。
     */
    @Test
    void shouldNormalizeRouteFailedResult() {
        TravelTask task = new TravelTask(TravelTaskType.ROUTE, "西安到杭州路线", "杭州", 1);
        ToolResult result = new ToolResult(TravelTaskType.ROUTE, "MapRouteTool", false, "", "route failed", OffsetDateTime.now());

        TravelEvidence evidence = normalizer.normalize(List.of(result), List.of(task)).get(0);

        assertThat(evidence.keyFacts())
                .containsEntry("sourceStatus", "FAILED")
                .containsEntry("needSecondConfirm", true)
                .containsEntry("failureReason", "route failed");
    }

    /**
     * 验证景点成功载荷可以归一化为结构化 ATTRACTION evidence。
     */
    @Test
    void shouldNormalizeAttractionSuccessPayload() throws Exception {
        AttractionToolPayload payload = new AttractionToolPayload("杭州热门景点", "杭州", "以官方平台为准", true, "以官方平台为准", "HIGH", "https://example.gov.cn", "OFFICIAL", "SUCCESS", false, false, 0.82, "", OffsetDateTime.now());
        TravelTask task = new TravelTask(TravelTaskType.ATTRACTION, "杭州景点", "杭州", 1);
        ToolResult result = new ToolResult(TravelTaskType.ATTRACTION, "AttractionInfoTool", true, objectMapper.writeValueAsString(payload), null, OffsetDateTime.now());

        TravelEvidence evidence = normalizer.normalize(List.of(result), List.of(task)).get(0);

        assertThat(evidence.evidenceType()).isEqualTo(EvidenceType.ATTRACTION);
        assertThat(evidence.keyFacts())
                .containsEntry("attractionName", "杭州热门景点")
                .containsEntry("openTime", "以官方平台为准")
                .containsEntry("sourceStatus", "SUCCESS")
                .containsEntry("fallback", false);
    }

    /**
     * 验证景点降级载荷可以归一化为结构化 ATTRACTION evidence。
     */
    @Test
    void shouldNormalizeAttractionFallbackPayload() throws Exception {
        AttractionToolPayload payload = new AttractionToolPayload("杭州热门景点", "杭州", "需二次确认", true, "需二次确认", "HIGH", "", "MockAttractionInfoTool", "FALLBACK", true, true, 0.35, "missing key", OffsetDateTime.now());
        TravelTask task = new TravelTask(TravelTaskType.ATTRACTION, "杭州景点", "杭州", 1);
        ToolResult result = new ToolResult(TravelTaskType.ATTRACTION, "AttractionInfoTool -> MockAttractionInfoTool", true, objectMapper.writeValueAsString(payload), "missing key", OffsetDateTime.now());

        TravelEvidence evidence = normalizer.normalize(List.of(result), List.of(task)).get(0);

        assertThat(evidence.keyFacts())
                .containsEntry("sourceStatus", "FALLBACK")
                .containsEntry("fallback", true)
                .containsEntry("needSecondConfirm", true)
                .containsEntry("ticketInfo", "需二次确认");
    }

    /**
     * 验证旧 mock 文本路线结果仍可兼容归一化。
     */
    @Test
    void shouldNormalizeLegacyRouteTextResult() {
        TravelTask task = new TravelTask(TravelTaskType.ROUTE, "西安到杭州路线", "杭州", 1);
        ToolResult result = new ToolResult(TravelTaskType.ROUTE, "MockRouteTool", true, "mock 路线建议，需二次确认", null, OffsetDateTime.now());

        TravelEvidence evidence = normalizer.normalize(List.of(result), List.of(task)).get(0);

        assertThat(evidence.keyFacts())
                .containsEntry("sourceStatus", "FALLBACK")
                .containsEntry("fallback", true)
                .containsEntry("needSecondConfirm", true);
    }
}
