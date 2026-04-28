package com.yeqian.travelagent.agent.normalizer;

import com.yeqian.travelagent.domain.enums.EvidenceType;
import com.yeqian.travelagent.domain.enums.TravelTaskType;
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
    }
}
