package com.yeqian.travelagent.agent.generator;

import com.yeqian.travelagent.domain.enums.FatigueLevel;
import com.yeqian.travelagent.domain.model.DailyPlan;
import com.yeqian.travelagent.domain.model.ImageBrief;
import com.yeqian.travelagent.domain.model.TravelPlan;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 一图流文案生成器测试。
 */
class ImageBriefGeneratorTest {

    private final ImageBriefGenerator generator = new ImageBriefGenerator();

    /**
     * 验证最终计划可以生成包含路线、每日安排、提醒和风险的一图流结构。
     */
    @Test
    void shouldGenerateImageBriefSections() {
        ImageBrief brief = generator.generate(plan());

        assertThat(brief.title()).contains("杭州");
        assertThat(brief.sections()).hasSize(4);
        assertThat(brief.sections()).extracting(section -> section.title())
                .contains("路线", "每日安排", "提醒清单", "风险提示");
    }

    /**
     * 构造测试最终计划。
     *
     * @return 最终计划
     */
    private TravelPlan plan() {
        DailyPlan dailyPlan = new DailyPlan(1, "杭州", "抵达杭州", "西湖轻量游", "早点休息", FatigueLevel.LOW, List.of("二次确认开放时间"));
        return new TravelPlan("杭州3天旅行计划", "摘要", List.of("西安", "杭州", "西安"), List.of(dailyPlan), List.of(), List.of(), Map.of(), List.of("节假日人流风险"), List.of("确认车票"));
    }
}
