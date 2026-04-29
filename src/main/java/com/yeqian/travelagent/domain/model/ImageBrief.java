package com.yeqian.travelagent.domain.model;

import java.util.List;

/**
 * 一图流文案。
 *
 * <p>承载适合前端渲染成图片的标题、副标题和结构化内容。</p>
 *
 * @param title 标题
 * @param subtitle 副标题
 * @param sections 内容区块列表
 * @param routeLine 路线城市列表
 * @param dayCards 每日卡片列表
 * @param budgetCards 预算卡片列表
 * @param riskTags 风险标签列表
 * @param reminderCards 提醒卡片列表
 * @param footerNote 底部说明
 */
public record ImageBrief(
        String title,
        String subtitle,
        List<ImageBriefSection> sections,
        List<String> routeLine,
        List<ImageBriefDayCard> dayCards,
        List<ImageBriefBudgetCard> budgetCards,
        List<String> riskTags,
        List<ImageBriefReminderCard> reminderCards,
        String footerNote
) {
    /**
     * 创建兼容旧调用的一图流文案。
     *
     * @param title 标题
     * @param subtitle 副标题
     * @param sections 内容区块列表
     */
    public ImageBrief(String title, String subtitle, List<ImageBriefSection> sections) {
        this(title, subtitle, sections, List.of(), List.of(), List.of(), List.of(), List.of(), "");
    }

    /**
     * 创建一图流文案并规整空集合字段。
     */
    public ImageBrief {
        sections = sections == null ? List.of() : List.copyOf(sections);
        routeLine = routeLine == null ? List.of() : List.copyOf(routeLine);
        dayCards = dayCards == null ? List.of() : List.copyOf(dayCards);
        budgetCards = budgetCards == null ? List.of() : List.copyOf(budgetCards);
        riskTags = riskTags == null ? List.of() : List.copyOf(riskTags);
        reminderCards = reminderCards == null ? List.of() : List.copyOf(reminderCards);
    }
}
