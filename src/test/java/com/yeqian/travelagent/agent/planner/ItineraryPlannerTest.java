package com.yeqian.travelagent.agent.planner;

import com.yeqian.travelagent.domain.enums.EvidenceType;
import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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
        assertThat(plan.dailyPlans().get(0).morning()).contains("08:30", "10:00", "需二次确认");
        assertThat(plan.risks()).isNotEmpty();
        assertThat(plan.budgetEstimate()).containsKeys("transport", "hotel", "total");
        assertThat(plan.transportSuggestions()).isNotEmpty();
        assertThat(plan.hotelSuggestions()).isNotEmpty();
    }

    /**
     * 验证景点和路线证据会进入行程、风险和待办。
     */
    @Test
    void shouldUseAttractionAndRouteEvidenceInPlan() {
        TravelIntent intent = new TravelIntent("西安", "五一", 4, 2, BigDecimal.valueOf(5000), List.of("杭州", "上海"), List.of("不想太累"), null, null, List.of());
        TravelCandidatePlan candidate = new TravelCandidatePlan("杭州 + 上海低疲劳精选", List.of("西安", "杭州", "上海", "西安"), "跨城次数少");
        TravelScore score = new TravelScore(88, 90, 86, 85, 88, 80, 78, "推荐");
        ScoredTravelPlan scoredPlan = new ScoredTravelPlan(candidate, score);
        List<TravelEvidence> evidences = List.of(
                evidence(EvidenceType.ATTRACTION, "杭州景点证据", Map.of(
                        "attractionName", "西湖",
                        "city", "杭州",
                        "openTime", "需二次确认",
                        "ticketInfo", "需二次确认",
                        "reservationRequired", true,
                        "holidayRisk", "HIGH",
                        "needSecondConfirm", true,
                        "fallback", true,
                        "failureReason", "mock 降级"
                )),
                evidence(EvidenceType.ROUTE, "杭州路线证据", Map.of(
                        "transferSuggestion", "建议提前确认高铁或大巴班次",
                        "routeRisk", "HIGH",
                        "needSecondConfirm", true,
                        "fallback", false
                ))
        );

        TravelPlan plan = planner.generate(intent, List.of(scoredPlan), evidences);

        assertThat(plan.dailyPlans()).anySatisfy(day -> assertThat(day.afternoon()).contains("西湖"));
        assertThat(plan.transportSuggestions()).anySatisfy(suggestion -> assertThat(suggestion).contains("提前确认高铁或大巴班次"));
        assertThat(plan.risks()).anySatisfy(risk -> assertThat(risk).contains("二次确认"));
        assertThat(plan.todoList()).anySatisfy(todo -> assertThat(todo).contains("预约"));
        assertThat(String.join("；", plan.risks())).doesNotContain("余票充足", "实时票价");
    }

    /**
     * 验证常见城市会生成带时间、地点和耗时提示的详细行程。
     */
    @Test
    void shouldGenerateDetailedScheduleForKnownCities() {
        TravelIntent intent = new TravelIntent("西安", "五一", 2, 2, BigDecimal.valueOf(3000), List.of("宝鸡"), List.of("不想太累"), null, null, List.of());
        TravelCandidatePlan candidate = new TravelCandidatePlan("宝鸡低疲劳精选", List.of("西安", "宝鸡", "西安"), "低疲劳");
        TravelScore score = new TravelScore(88, 90, 86, 85, 88, 80, 78, "推荐");

        TravelPlan plan = planner.generate(intent, List.of(new ScoredTravelPlan(candidate, score)), List.of());
        String dayText = plan.dailyPlans().get(0).morning() + plan.dailyPlans().get(0).afternoon() + plan.dailyPlans().get(0).evening();

        assertThat(dayText).contains("法门寺文化景区", "陈仓老街", "预计");
        assertThat(dayText).doesNotContain("核心景点游览", "自由活动", "轻松收尾");
    }

    /**
     * 构造测试证据。
     *
     * @param evidenceType 证据类型
     * @param title 标题
     * @param facts 关键事实
     * @return 旅行证据
     */
    private TravelEvidence evidence(EvidenceType evidenceType, String title, Map<String, Object> facts) {
        return new TravelEvidence(evidenceType, "杭州", title, "摘要", facts, 0.8, "TEST", null, OffsetDateTime.now());
    }
}
