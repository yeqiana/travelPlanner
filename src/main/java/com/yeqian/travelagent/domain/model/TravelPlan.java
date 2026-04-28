package com.yeqian.travelagent.domain.model;

import java.util.List;
import java.util.Map;

/**
 * 最终旅行计划。
 *
 * <p>承载推荐路线、每日行程、交通住宿建议、预算估算和风险提示。</p>
 *
 * @param title 计划标题
 * @param summary 计划摘要
 * @param route 路线城市列表
 * @param dailyPlans 每日行程列表
 * @param transportSuggestions 交通建议列表
 * @param hotelSuggestions 酒店建议列表
 * @param budgetEstimate 预算估算
 * @param risks 风险提示列表
 * @param todoList 待办事项列表
 */
public record TravelPlan(
        String title,
        String summary,
        List<String> route,
        List<DailyPlan> dailyPlans,
        List<String> transportSuggestions,
        List<String> hotelSuggestions,
        Map<String, Object> budgetEstimate,
        List<String> risks,
        List<String> todoList
) {
    /**
     * 创建最终旅行计划并规整空集合字段。
     */
    public TravelPlan {
        route = route == null ? List.of() : List.copyOf(route);
        dailyPlans = dailyPlans == null ? List.of() : List.copyOf(dailyPlans);
        transportSuggestions = transportSuggestions == null ? List.of() : List.copyOf(transportSuggestions);
        hotelSuggestions = hotelSuggestions == null ? List.of() : List.copyOf(hotelSuggestions);
        budgetEstimate = budgetEstimate == null ? Map.of() : Map.copyOf(budgetEstimate);
        risks = risks == null ? List.of() : List.copyOf(risks);
        todoList = todoList == null ? List.of() : List.copyOf(todoList);
    }
}
