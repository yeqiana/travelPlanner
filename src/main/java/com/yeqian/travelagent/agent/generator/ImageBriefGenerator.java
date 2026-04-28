package com.yeqian.travelagent.agent.generator;

import com.yeqian.travelagent.domain.model.ImageBrief;
import com.yeqian.travelagent.domain.model.ImageBriefSection;
import com.yeqian.travelagent.domain.model.TravelPlan;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 一图流文案生成器。
 *
 * <p>根据最终旅行计划生成适合前端图片化展示的结构化文案。</p>
 */
@Component
public class ImageBriefGenerator {

    /**
     * 生成一图流文案。
     *
     * @param plan 最终旅行计划
     * @return 一图流文案
     */
    public ImageBrief generate(TravelPlan plan) {
        return new ImageBrief(
                plan.title(),
                "路线总览 · 每日安排 · 风险提醒",
                List.of(
                        new ImageBriefSection("路线", String.join(" -> ", plan.route())),
                        new ImageBriefSection("每日安排", dailyPlanText(plan)),
                        new ImageBriefSection("提醒清单", String.join("；", plan.todoList())),
                        new ImageBriefSection("风险提示", String.join("；", plan.risks()))
                )
        );
    }

    /**
     * 拼接每日计划摘要。
     *
     * @param plan 最终旅行计划
     * @return 每日计划摘要
     */
    private String dailyPlanText(TravelPlan plan) {
        return plan.dailyPlans().stream()
                .map(day -> "Day" + day.day() + " " + day.city() + "：" + day.morning() + " / " + day.afternoon() + " / " + day.evening())
                .collect(Collectors.joining("；"));
    }
}
