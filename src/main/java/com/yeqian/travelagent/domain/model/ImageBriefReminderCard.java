package com.yeqian.travelagent.domain.model;

/**
 * 一图流提醒卡片。
 *
 * <p>描述前端一图流中提醒模块的一条提醒。</p>
 *
 * @param title 提醒标题
 * @param time 提醒时间
 */
public record ImageBriefReminderCard(
        String title,
        String time
) {
}
