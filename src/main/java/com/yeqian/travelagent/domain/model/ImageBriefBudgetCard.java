package com.yeqian.travelagent.domain.model;

/**
 * 一图流预算卡片。
 *
 * <p>描述前端一图流中预算模块的一项名称和值。</p>
 *
 * @param name 预算项名称
 * @param value 预算项展示值
 */
public record ImageBriefBudgetCard(
        String name,
        String value
) {
}
