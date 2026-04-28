package com.yeqian.travelagent.domain.model;

/**
 * 一图流文案区块。
 *
 * <p>描述适合前端排版成图片的一个内容分区。</p>
 *
 * @param title 区块标题
 * @param content 区块内容
 */
public record ImageBriefSection(
        String title,
        String content
) {
}
