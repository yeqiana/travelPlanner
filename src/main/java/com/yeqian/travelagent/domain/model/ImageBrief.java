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
 */
public record ImageBrief(
        String title,
        String subtitle,
        List<ImageBriefSection> sections
) {
    /**
     * 创建一图流文案并规整空集合字段。
     */
    public ImageBrief {
        sections = sections == null ? List.of() : List.copyOf(sections);
    }
}
