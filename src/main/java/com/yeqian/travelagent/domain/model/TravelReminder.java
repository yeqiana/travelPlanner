package com.yeqian.travelagent.domain.model;

import com.yeqian.travelagent.domain.enums.ReminderType;

/**
 * 旅行提醒。
 *
 * <p>描述购票、预约和出发准备等可执行提醒事项。</p>
 *
 * @param title 提醒标题
 * @param type 提醒类型
 * @param remindRule 提醒规则
 * @param description 提醒说明
 */
public record TravelReminder(
        String title,
        ReminderType type,
        String remindRule,
        String description
) {
}
