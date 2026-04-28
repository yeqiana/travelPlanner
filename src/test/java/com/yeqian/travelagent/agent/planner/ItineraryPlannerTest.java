package com.yeqian.travelagent.agent.planner;

import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 行程计划生成器测试。
 */
class ItineraryPlannerTest {

    private final ItineraryPlanner planner = new ItineraryPlanner();

    /**
     * 验证最高分方案可以生成包含每日行程、风险和预算估算的最终计划。
     */
    @Test
    void shouldGenerateDailyPlansRisksAndBudget() {
        TravelIntent intent = new TravelIntent("西安", "五一", 4, 2, BigDecimal.valueOf(5000), List.of("杭州", "上海"), List.of("不想太累"), null, null, List.of());
        TravelCandidatePlan candidate = new TravelCandidatePlan("杭州 + 上海低疲劳精选", List.of("西安", "杭州", "上海", "西安"), "跨城次数少");
        TravelScore score = new TravelScore(88, 90, 86, 85, 88, 80, 78, "推荐");
        ScoredTravelPlan scoredPlan = new ScoredTravelPlan(candidate, score);

        TravelPlan plan = planner.generate(intent, List.of(scoredPlan), List.of());

        assertThat(plan.dailyPlans()).hasSize(4);
        assertThat(plan.risks()).isNotEmpty();
        assertThat(plan.budgetEstimate()).containsKeys("transport", "hotel", "total");
        assertThat(plan.transportSuggestions()).isNotEmpty();
        assertThat(plan.hotelSuggestions()).isNotEmpty();
    }
}
