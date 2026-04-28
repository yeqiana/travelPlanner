package com.yeqian.travelagent.domain.model;

import com.yeqian.travelagent.domain.enums.TravelTaskType;

import java.time.OffsetDateTime;

/**
 * 工具调用结果。
 *
 * <p>统一承载 mock 或真实工具返回的原始内容，供证据归一化使用。</p>
 *
 * @param taskType 任务类型
 * @param source 数据来源
 * @param success 是否调用成功
 * @param rawContent 原始内容
 * @param errorMessage 错误信息，成功时为空
 * @param fetchedAt 获取时间
 */
public record ToolResult(
        TravelTaskType taskType,
        String source,
        boolean success,
        String rawContent,
        String errorMessage,
        OffsetDateTime fetchedAt
) {
}
