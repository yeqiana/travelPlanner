package com.yeqian.travelagent.agent.scorer;

import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 旅行方案评分器测试。
 */
class TravelScorerTest {

    private final TravelScorer scorer = new TravelScorer();

    /**
     * 验证用户不想太累时，高跨城方案疲劳分会降低。
     */
    @Test
    void shouldLowerFatigueScoreForHighCrossCityPlanWhenUserPrefersLowFatigue() {
        TravelIntent intent = intent(BigDecimal.valueOf(10000), List.of("不想太累"));
        TravelCandidatePlan lowCrossCityPlan = new TravelCandidatePlan("杭州上海低疲劳", List.of("西安", "杭州", "上海", "西安"), "跨城较少。");
        TravelCandidatePlan highCrossCityPlan = new TravelCandidatePlan("杭州苏州南京上海完整游", List.of("西安", "杭州", "苏州", "南京", "上海", "西安"), "跨城较多。");

        List<ScoredTravelPlan> scoredPlans = scorer.score(List.of(lowCrossCityPlan, highCrossCityPlan), intent, List.of());

        TravelScore lowScore = findScore(scoredPlans, "杭州上海低疲劳");
        TravelScore highScore = findScore(scoredPlans, "杭州苏州南京上海完整游");
        assertThat(highScore.fatigueScore()).isLessThan(lowScore.fatigueScore());
    }

    /**
     * 验证预算超出时预算分降低。
     */
    @Test
    void shouldLowerBudgetScoreWhenEstimatedCostExceedsBudget() {
        TravelCandidatePlan plan = new TravelCandidatePlan("杭州上海低疲劳", List.of("西安", "杭州", "上海", "西安"), "跨城较少。");

        TravelScore enoughBudgetScore = scorer.score(List.of(plan), intent(BigDecimal.valueOf(10000), List.of()), List.of()).get(0).score();
        TravelScore lowBudgetScore = scorer.score(List.of(plan), intent(BigDecimal.valueOf(1000), List.of()), List.of()).get(0).score();

        assertThat(lowBudgetScore.budgetScore()).isLessThan(enoughBudgetScore.budgetScore());
    }

    /**
     * 验证总分按固定权重计算。
     */
    @Test
    void shouldCalculateTotalScoreByWeights() {
        TravelCandidatePlan plan = new TravelCandidatePlan("杭州上海低疲劳", List.of("西安", "杭州", "上海", "西安"), "跨城较少。");

        TravelScore score = scorer.score(List.of(plan), intent(BigDecimal.valueOf(10000), List.of()), List.of()).get(0).score();

        int expectedTotal = (int) Math.round(score.routeConvenienceScore() * 0.25
                + score.fatigueScore() * 0.25
                + score.budgetScore() * 0.20
                + score.attractionValueScore() * 0.15
                + score.holidayRiskScore() * 0.10
                + score.weatherTicketRiskScore() * 0.05);
        assertThat(score.totalScore()).isEqualTo(expectedTotal);
    }

    /**
     * 构造旅行意图。
     *
     * @param budget 总预算
     * @param styles 旅行风格
     * @return 旅行意图
     */
    private TravelIntent intent(BigDecimal budget, List<String> styles) {
        return new TravelIntent(
                "西安",
                "五一",
                4,
                2,
                budget,
                List.of("杭州", "上海"),
                styles,
                null,
                null,
                List.of()
        );
    }

    /**
     * 按方案名称查找评分。
     *
     * @param plans 已评分方案列表
     * @param name 方案名称
     * @return 旅行评分
     */
    private TravelScore findScore(List<ScoredTravelPlan> plans, String name) {
        return plans.stream()
                .filter(plan -> plan.candidatePlan().name().equals(name))
                .findFirst()
                .orElseThrow()
                .score();
    }
}
