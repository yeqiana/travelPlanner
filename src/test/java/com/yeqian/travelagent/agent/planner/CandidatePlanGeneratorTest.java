package com.yeqian.travelagent.agent.planner;

import com.yeqian.travelagent.domain.enums.EvidenceType;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 候选方案生成器测试。
 */
class CandidatePlanGeneratorTest {

    private final CandidatePlanGenerator generator = new CandidatePlanGenerator();

    /**
     * 验证杭州和上海需求可以生成 2 到 3 个候选方案。
     */
    @Test
    void shouldGenerateTwoOrThreeCandidatePlans() {
        TravelIntent intent = new TravelIntent(
                "西安",
                "五一",
                4,
                2,
                BigDecimal.valueOf(3000),
                List.of("杭州", "上海"),
                List.of("不想太累"),
                null,
                null,
                List.of()
        );

        List<TravelCandidatePlan> plans = generator.generate(intent, List.of());

        assertThat(plans).hasSizeBetween(2, 3);
        assertThat(plans).allSatisfy(plan -> {
            assertThat(plan.name()).isNotBlank();
            assertThat(plan.route()).isNotEmpty();
            assertThat(plan.reason()).isNotBlank();
        });
    }

    /**
     * 验证候选方案会保留证据引用和风险说明。
     */
    @Test
    void shouldAttachEvidenceRefsAndRiskReason() {
        TravelIntent intent = new TravelIntent("西安", "五一", 4, 2, BigDecimal.valueOf(3000), List.of("杭州", "上海"), List.of("不想太累"), null, null, List.of());
        TravelEvidence routeEvidence = evidence(EvidenceType.ROUTE, "杭州路线证据", "MockRouteTool", Map.of(
                "routeRisk", "HIGH",
                "durationMinutes", 300,
                "sourceStatus", "FALLBACK",
                "fallback", true,
                "needSecondConfirm", true
        ));

        List<TravelCandidatePlan> plans = generator.generate(intent, List.of(routeEvidence));

        assertThat(plans).allSatisfy(plan -> {
            assertThat(plan.evidenceRefs()).contains("杭州路线证据/MockRouteTool");
            assertThat(plan.reason()).contains("二次确认");
        });
    }

    /**
     * 构造测试证据。
     *
     * @param evidenceType 证据类型
     * @param title 标题
     * @param source 来源
     * @param facts 关键事实
     * @return 旅行证据
     */
    private TravelEvidence evidence(EvidenceType evidenceType, String title, String source, Map<String, Object> facts) {
        return new TravelEvidence(evidenceType, "杭州", title, "摘要", facts, 0.8, source, null, OffsetDateTime.now());
    }
}
