package com.yeqian.travelagent.agent.suggestion;

import com.yeqian.travelagent.domain.enums.TravelDialogIntent;
import com.yeqian.travelagent.domain.model.ClarificationQuestion;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下文快捷提示生成器。
 *
 * <p>根据当前追问、旅行意图、推荐计划和多轮意图生成更贴近当前会话的快捷提示。</p>
 */
@Component
public class ContextualSuggestionGenerator {

    /**
     * 为追问状态生成快捷提示。
     *
     * @param questions 结构化追问列表
     * @return 快捷提示列表
     */
    public List<String> forClarification(List<ClarificationQuestion> questions) {
        List<String> suggestions = new ArrayList<>();
        for (ClarificationQuestion question : safeQuestions(questions)) {
            suggestions.add(clarificationSuggestion(question));
        }
        suggestions.add("补充预算和人数");
        return limitSuggestions(suggestions);
    }

    /**
     * 为完整计划状态生成快捷提示。
     *
     * @param intent 旅行意图
     * @param recommendedPlan 推荐旅行计划
     * @param dialogIntent 本轮多轮意图
     * @return 快捷提示列表
     */
    public List<String> forPlan(TravelIntent intent, TravelPlan recommendedPlan, TravelDialogIntent dialogIntent) {
        List<String> suggestions = new ArrayList<>();
        if (dialogIntent == TravelDialogIntent.DETAIL_PLAN) {
            suggestions.add("继续细化交通和餐饮");
        }
        String destination = firstDestination(intent, recommendedPlan);
        if (recommendedPlan != null && recommendedPlan.dailyPlans().size() >= 2) {
            suggestions.add("调整第2天节奏");
        }
        if (intent == null || intent.budget() == null) {
            suggestions.add("补充预算范围");
        }
        if (hasMultipleCities(recommendedPlan)) {
            suggestions.add("减少跨城交通");
        }
        suggestions.add("换一些" + destination + "餐厅");
        suggestions.add("细化每天上午下午晚上安排");
        return limitSuggestions(suggestions);
    }

    /**
     * 根据追问字段生成快捷提示。
     *
     * @param question 结构化追问
     * @return 快捷提示文本
     */
    private String clarificationSuggestion(ClarificationQuestion question) {
        if (question == null || question.field() == null) {
            return "补充旅行信息";
        }
        return switch (question.field()) {
            case "departureCity" -> "补充出发城市";
            case "dateText" -> "补充出发日期";
            case "days" -> "补充出行天数";
            default -> "补充" + question.question();
        };
    }

    /**
     * 获取主要目的地。
     *
     * @param intent 旅行意图
     * @param recommendedPlan 推荐旅行计划
     * @return 主要目的地
     */
    private String firstDestination(TravelIntent intent, TravelPlan recommendedPlan) {
        if (intent != null && !intent.destinationPreferences().isEmpty()) {
            return intent.destinationPreferences().get(0);
        }
        if (recommendedPlan != null && !recommendedPlan.dailyPlans().isEmpty()) {
            return recommendedPlan.dailyPlans().get(0).city();
        }
        return "当地";
    }

    /**
     * 判断计划是否包含多个游玩城市。
     *
     * @param recommendedPlan 推荐旅行计划
     * @return 包含多个城市时返回 true
     */
    private boolean hasMultipleCities(TravelPlan recommendedPlan) {
        return recommendedPlan != null
                && recommendedPlan.dailyPlans().stream()
                .map(plan -> plan.city())
                .distinct()
                .count() > 1;
    }

    /**
     * 安全返回追问列表。
     *
     * @param questions 原始追问列表
     * @return 非空追问列表
     */
    private List<ClarificationQuestion> safeQuestions(List<ClarificationQuestion> questions) {
        return questions == null ? List.of() : questions;
    }

    /**
     * 去重并限制快捷提示数量。
     *
     * @param suggestions 原始快捷提示列表
     * @return 最多 3 条快捷提示
     */
    private List<String> limitSuggestions(List<String> suggestions) {
        return suggestions.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .limit(3)
                .toList();
    }
}
