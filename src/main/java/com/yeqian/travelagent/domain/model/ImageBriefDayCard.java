package com.yeqian.travelagent.domain.model;

import java.util.List;

/**
 * 一图流每日卡片。
 *
 * <p>描述某一天在前端一图流中的标题和展示条目。</p>
 *
 * @param day 第几天
 * @param title 卡片标题
 * @param items 展示条目
 */
public record ImageBriefDayCard(
        int day,
        String title,
        List<String> items
) {
    /**
     * 创建一图流每日卡片并规整空集合字段。
     */
    public ImageBriefDayCard {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
