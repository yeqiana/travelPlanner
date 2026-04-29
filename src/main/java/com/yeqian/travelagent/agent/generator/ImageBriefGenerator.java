package com.yeqian.travelagent.agent.generator;

import com.yeqian.travelagent.domain.model.ImageBrief;
import com.yeqian.travelagent.domain.model.ImageBriefBudgetCard;
import com.yeqian.travelagent.domain.model.ImageBriefDayCard;
import com.yeqian.travelagent.domain.model.ImageBriefReminderCard;
import com.yeqian.travelagent.domain.model.ImageBriefSection;
import com.yeqian.travelagent.domain.model.TravelPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                ),
                plan.route(),
                dayCards(plan),
                budgetCards(plan),
                plan.risks(),
                reminderCards(plan),
                "实时价格、余票、开放时间和预约规则以官方平台为准"
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

    /**
     * 构造每日行程卡片。
     *
     * @param plan 最终旅行计划
     * @return 每日行程卡片列表
     */
    private List<ImageBriefDayCard> dayCards(TravelPlan plan) {
        return plan.dailyPlans().stream()
                .map(day -> new ImageBriefDayCard(
                        day.day(),
                        "Day " + day.day() + " " + day.city(),
                        List.of(day.morning(), day.afternoon(), day.evening())
                ))
                .toList();
    }

    /**
     * 构造预算卡片。
     *
     * @param plan 最终旅行计划
     * @return 预算卡片列表
     */
    private List<ImageBriefBudgetCard> budgetCards(TravelPlan plan) {
        Map<String, Object> budget = plan.budgetEstimate();
        List<ImageBriefBudgetCard> cards = new ArrayList<>();
        cards.add(new ImageBriefBudgetCard("交通", valueText(budget.get("transport"))));
        cards.add(new ImageBriefBudgetCard("住宿", valueText(budget.get("hotel"))));
        cards.add(new ImageBriefBudgetCard("景点", valueText(budget.get("attraction"))));
        cards.add(new ImageBriefBudgetCard("合计", valueText(budget.get("total"))));
        return cards;
    }

    /**
     * 构造提醒卡片。
     *
     * @param plan 最终旅行计划
     * @return 提醒卡片列表
     */
    private List<ImageBriefReminderCard> reminderCards(TravelPlan plan) {
        return plan.todoList().stream()
                .limit(4)
                .map(todo -> new ImageBriefReminderCard(todo, "出发前确认"))
                .toList();
    }

    /**
     * 转换预算展示值。
     *
     * @param value 原始预算值
     * @return 预算展示值
     */
    private String valueText(Object value) {
        return value == null ? "需二次确认" : String.valueOf(value);
    }
}
