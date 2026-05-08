package com.yeqian.travelagent.domain.model;

import java.util.List;

/**
 * 上一轮完整旅行计划上下文。
 *
 * <p>用于在完成态会话中保存上一轮推荐计划、提醒和风险，支撑后续按天、餐饮、酒店和交通的局部调整。</p>
 *
 * @param planId 上一轮计划编号
 * @param intent 上一轮完整旅行意图
 * @param recommendedPlan 上一轮推荐旅行计划
 * @param reminders 上一轮旅行提醒列表
 * @param risks 上一轮风险提示列表
 */
public record PreviousTravelPlanContext(
        String planId,
        TravelIntent intent,
        TravelPlan recommendedPlan,
        List<TravelReminder> reminders,
        List<String> risks
) {
    /**
     * 创建上一轮完整旅行计划上下文并规整空集合字段。
     */
    public PreviousTravelPlanContext {
        reminders = reminders == null ? List.of() : List.copyOf(reminders);
        risks = risks == null ? List.of() : List.copyOf(risks);
    }
}
