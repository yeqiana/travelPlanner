package com.yeqian.travelagent.agent.adjuster;

import com.yeqian.travelagent.domain.enums.FatigueLevel;
import com.yeqian.travelagent.domain.enums.TravelDialogIntent;
import com.yeqian.travelagent.domain.model.DailyPlan;
import com.yeqian.travelagent.domain.model.PreviousTravelPlanContext;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelSessionContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 旅行计划局部调整器测试。
 */
class TravelPlanLocalAdjusterTest {

    private final TravelPlanLocalAdjuster adjuster = new TravelPlanLocalAdjuster();

    /**
     * 验证细化计划会基于上一轮计划补充时间、交通、耗时和二次确认提示。
     */
    @Test
    void shouldDetailPreviousPlan() {
        TravelPlan adjustedPlan = adjuster.adjust(
                TravelDialogIntent.DETAIL_PLAN,
                "没有详细的旅游计划啊",
                context(previousPlan()),
                generatedPlan()
        );

        assertThat(adjustedPlan.dailyPlans().get(0).morning()).contains("09:00 西湖", "预计2.5小时", "二次确认", "地铁/打车");
        assertThat(adjustedPlan.dailyPlans().get(0).afternoon()).contains("14:00 灵隐寺", "地铁/打车", "预计2.5小时");
        assertThat(adjustedPlan.dailyPlans().get(0).evening()).contains("18:00 湖滨晚餐", "预计2小时");
        assertThat(adjustedPlan.todoList()).anyMatch(value -> value.contains("细化为可执行时间段"));
    }

    /**
     * 验证原计划已有时间但缺少交通、耗时和确认提示时，细化逻辑仍会补齐缺失信息。
     */
    @Test
    void shouldCompleteDetailHintsWhenOriginalAlreadyHasTime() {
        TravelPlan adjustedPlan = adjuster.adjust(
                TravelDialogIntent.DETAIL_PLAN,
                "太笼统了，具体一点",
                context(previousPlan()),
                generatedPlan()
        );

        assertThat(adjustedPlan.dailyPlans().get(0).morning())
                .contains("09:00 西湖", "建议地铁/打车", "预计2.5小时", "二次确认");
        assertThat(adjustedPlan.dailyPlans().get(0).afternoon())
                .contains("14:00 灵隐寺", "建议地铁/打车", "预计2.5小时", "二次确认");
        assertThat(adjustedPlan.dailyPlans().get(0).evening())
                .contains("18:00 湖滨晚餐", "建议地铁/打车", "预计2小时", "二次确认");
    }

    /**
     * 验证指定天调整只影响目标天，不重写其他天。
     */
    @Test
    void shouldAdjustOnlyTargetDay() {
        TravelPlan previousPlan = previousPlan();
        TravelPlan adjustedPlan = adjuster.adjust(
                TravelDialogIntent.ADJUST_PLAN,
                "第二天轻松一点",
                context(previousPlan),
                generatedPlan()
        );

        assertThat(adjustedPlan.dailyPlans().get(0).morning()).isEqualTo(previousPlan.dailyPlans().get(0).morning());
        assertThat(adjustedPlan.dailyPlans().get(1).morning()).contains("近距离候选点", "预计1.5小时", "二次确认");
        assertThat(adjustedPlan.dailyPlans().get(1).evening()).contains("不再安排远距离夜游", "二次确认");
        assertThat(adjustedPlan.dailyPlans().get(1).fatigueLevel()).isEqualTo(FatigueLevel.LOW);
    }

    /**
     * 验证餐饮调整优先替换餐饮表达。
     */
    @Test
    void shouldAdjustDiningOnly() {
        TravelPlan adjustedPlan = adjuster.adjust(
                TravelDialogIntent.ADJUST_PLAN,
                "换一些餐厅",
                context(previousPlan()),
                generatedPlan()
        );

        assertThat(adjustedPlan.dailyPlans()).allSatisfy(dailyPlan -> assertThat(dailyPlan.evening()).contains("口碑餐厅"));
        assertThat(adjustedPlan.hotelSuggestions()).containsExactly("住西湖附近");
    }

    /**
     * 验证酒店调整只追加住宿建议。
     */
    @Test
    void shouldAdjustHotelSuggestions() {
        TravelPlan adjustedPlan = adjuster.adjust(
                TravelDialogIntent.ADJUST_PLAN,
                "酒店换到交通方便的商圈",
                context(previousPlan()),
                generatedPlan()
        );

        assertThat(adjustedPlan.hotelSuggestions()).anyMatch(value -> value.contains("住宿局部调整"));
        assertThat(adjustedPlan.dailyPlans().get(0).morning()).isEqualTo("09:00 西湖");
    }

    /**
     * 验证交通调整只追加交通建议。
     */
    @Test
    void shouldAdjustTransportSuggestions() {
        TravelPlan adjustedPlan = adjuster.adjust(
                TravelDialogIntent.ADJUST_PLAN,
                "减少跨城交通，别太折腾",
                context(previousPlan()),
                generatedPlan()
        );

        assertThat(adjustedPlan.transportSuggestions()).anyMatch(value -> value.contains("减少跨城"));
        assertThat(adjustedPlan.hotelSuggestions()).containsExactly("住西湖附近");
    }

    /**
     * 构造上一轮会话上下文。
     *
     * @param plan 上一轮旅行计划
     * @return 会话上下文
     */
    private TravelSessionContext context(TravelPlan plan) {
        TravelIntent intent = new TravelIntent("西安", "五一", 2, 2, BigDecimal.valueOf(3000), List.of("杭州"), List.of(), null, null, List.of());
        PreviousTravelPlanContext previousPlanContext = new PreviousTravelPlanContext("plan-1", intent, plan, List.of(), plan.risks());
        return new TravelSessionContext("session-1", intent, List.of(), previousPlanContext, LocalDateTime.now(), LocalDateTime.now(), "COMPLETED");
    }

    /**
     * 构造上一轮旅行计划。
     *
     * @return 旅行计划
     */
    private TravelPlan previousPlan() {
        return new TravelPlan(
                "杭州2日游",
                "上一轮计划",
                List.of("西安", "杭州"),
                List.of(
                        new DailyPlan(1, "杭州", "09:00 西湖", "14:00 灵隐寺", "18:00 湖滨晚餐", FatigueLevel.MEDIUM, List.of()),
                        new DailyPlan(2, "杭州", "09:00 西溪湿地", "14:00 河坊街", "18:00 武林夜游", FatigueLevel.MEDIUM, List.of())
                ),
                List.of("高铁往返"),
                List.of("住西湖附近"),
                Map.of("total", 3000),
                List.of("节假日人流需确认"),
                List.of("确认门票")
        );
    }

    /**
     * 构造本轮兜底生成计划。
     *
     * @return 旅行计划
     */
    private TravelPlan generatedPlan() {
        return new TravelPlan("新计划", "不应优先使用", List.of(), List.of(), List.of(), List.of(), Map.of(), List.of(), List.of());
    }
}
