package com.yeqian.travelagent.agent.planner;

import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelIntent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

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
}
