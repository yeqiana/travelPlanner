package com.yeqian.travelagent.agent.scorer;

import com.yeqian.travelagent.domain.enums.EvidenceType;
import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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
     * 验证评分明细包含 6 个固定维度。
     */
    @Test
    void shouldReturnSixScoreDetails() {
        TravelCandidatePlan plan = new TravelCandidatePlan("杭州上海低疲劳", List.of("西安", "杭州", "上海", "西安"), "跨城较少。");

        TravelScore score = scorer.score(List.of(plan), intent(BigDecimal.valueOf(10000), List.of()), List.of()).get(0).score();

        assertThat(score.scoreDetails()).hasSize(6);
        assertThat(score.scoreDetails()).extracting(detail -> detail.dimension())
                .containsExactly("ROUTE_CONVENIENCE", "COST", "FATIGUE", "ATTRACTION_VALUE", "HOLIDAY_RISK", "WEATHER_TICKET_RISK");
    }

    /**
     * 验证路线耗时过高时交通顺路分和疲劳分下降。
     */
    @Test
    void shouldLowerRouteAndFatigueScoreWhenRouteDurationIsHigh() {
        TravelCandidatePlan plan = new TravelCandidatePlan("杭州上海低疲劳", List.of("西安", "杭州", "上海", "西安"), "跨城较少。");
        TravelScore normalScore = scorer.score(List.of(plan), intent(BigDecimal.valueOf(10000), List.of()), List.of(successRoute(120))).get(0).score();
        TravelScore longRouteScore = scorer.score(List.of(plan), intent(BigDecimal.valueOf(10000), List.of()), List.of(successRoute(420))).get(0).score();

        assertThat(longRouteScore.routeConvenienceScore()).isLessThan(normalScore.routeConvenienceScore());
        assertThat(longRouteScore.fatigueScore()).isLessThan(normalScore.fatigueScore());
    }

    /**
     * 验证五一遇到高节假日风险时节假日风险分下降。
     */
    @Test
    void shouldLowerHolidayRiskScoreWhenHolidayRiskHighOnMayDay() {
        TravelCandidatePlan plan = new TravelCandidatePlan("杭州上海低疲劳", List.of("西安", "杭州", "上海", "西安"), "跨城较少。");
        TravelScore normalScore = scorer.score(List.of(plan), intent(BigDecimal.valueOf(10000), List.of()), List.of()).get(0).score();
        TravelScore riskScore = scorer.score(List.of(plan), intent(BigDecimal.valueOf(10000), List.of()), List.of(attractionRisk())).get(0).score();

        assertThat(riskScore.holidayRiskScore()).isLessThan(normalScore.holidayRiskScore());
    }

    /**
     * 验证降级或失败证据不会产生确定性票价和余票结论。
     */
    @Test
    void shouldKeepFallbackAndFailedEvidenceConservative() {
        TravelCandidatePlan plan = new TravelCandidatePlan("杭州上海低疲劳", List.of("西安", "杭州", "上海", "西安"), "跨城较少。");
        TravelEvidence failedEvidence = evidence(EvidenceType.ATTRACTION, "景点失败证据", "MockAttractionInfoTool", Map.of(
                "ticketInfo", "需二次确认",
                "sourceStatus", "FAILED",
                "fallback", false,
                "needSecondConfirm", true
        ));

        TravelScore score = scorer.score(List.of(plan), intent(BigDecimal.valueOf(10000), List.of()), List.of(failedEvidence)).get(0).score();

        assertThat(score.scoreDetails()).anySatisfy(detail ->
                assertThat(detail.reason()).contains("不把降级或失败证据当作确定余票或票价")
        );
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

    /**
     * 构造成功路线证据。
     *
     * @param durationMinutes 路线耗时
     * @return 路线证据
     */
    private TravelEvidence successRoute(int durationMinutes) {
        return evidence(EvidenceType.ROUTE, "路线证据", "AMAP", Map.of(
                "durationMinutes", durationMinutes,
                "routeRisk", "LOW",
                "sourceStatus", "SUCCESS",
                "fallback", false,
                "needSecondConfirm", false
        ));
    }

    /**
     * 构造高风险景点证据。
     *
     * @return 景点证据
     */
    private TravelEvidence attractionRisk() {
        return evidence(EvidenceType.ATTRACTION, "景点证据", "OFFICIAL", Map.of(
                "holidayRisk", "HIGH",
                "reservationRequired", true,
                "sourceStatus", "SUCCESS",
                "fallback", false,
                "needSecondConfirm", false
        ));
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
