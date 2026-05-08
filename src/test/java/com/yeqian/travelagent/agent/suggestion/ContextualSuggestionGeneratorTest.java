package com.yeqian.travelagent.agent.suggestion;

import com.yeqian.travelagent.domain.enums.FatigueLevel;
import com.yeqian.travelagent.domain.enums.TravelDialogIntent;
import com.yeqian.travelagent.domain.model.ClarificationQuestion;
import com.yeqian.travelagent.domain.model.DailyPlan;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上下文快捷提示生成器测试。
 */
class ContextualSuggestionGeneratorTest {

    private final ContextualSuggestionGenerator generator = new ContextualSuggestionGenerator();

    /**
     * 验证追问场景会生成补充字段提示。
     */
    @Test
    void shouldGenerateClarificationSuggestions() {
        List<String> suggestions = generator.forClarification(List.of(
                new ClarificationQuestion("departureCity", "你是从哪个城市出发？", "例如：西安", true)
        ));

        assertThat(suggestions).contains("补充出发城市");
    }

    /**
     * 验证预算缺失时会提示补充预算。
     */
    @Test
    void shouldSuggestBudgetWhenBudgetMissing() {
        TravelIntent intent = new TravelIntent("西安", "五一", 3, 2, null, List.of("杭州"), List.of(), null, null, List.of());

        List<String> suggestions = generator.forPlan(intent, plan(), TravelDialogIntent.CREATE_PLAN);

        assertThat(suggestions).contains("补充预算范围");
    }

    /**
     * 验证完整计划场景会围绕当前计划生成调整提示。
     */
    @Test
    void shouldGeneratePlanAdjustSuggestions() {
        TravelIntent intent = new TravelIntent("西安", "五一", 3, 2, null, List.of("杭州"), List.of(), null, null, List.of());

        List<String> suggestions = generator.forPlan(intent, plan(), TravelDialogIntent.DETAIL_PLAN);

        assertThat(suggestions).contains("继续细化交通和餐饮", "调整第2天节奏");
    }

    /**
     * 构造测试计划。
     *
     * @return 旅行计划
     */
    private TravelPlan plan() {
        return new TravelPlan(
                "杭州上海3天旅行计划",
                "摘要",
                List.of("西安", "杭州", "上海", "西安"),
                List.of(
                        new DailyPlan(1, "杭州", "上午", "下午", "晚上", FatigueLevel.LOW, List.of()),
                        new DailyPlan(2, "上海", "上午", "下午", "晚上", FatigueLevel.LOW, List.of())
                ),
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                List.of()
        );
    }
}
