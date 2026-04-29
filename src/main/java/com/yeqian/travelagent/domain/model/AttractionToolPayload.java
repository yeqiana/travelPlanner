package com.yeqian.travelagent.domain.model;

import java.time.OffsetDateTime;

/**
 * 景点工具结构化载荷。
 *
 * <p>用于在景点信息工具和证据归一化器之间传递开放、预约、门票和来源信息。</p>
 *
 * @param attractionName 景点名称
 * @param city 城市
 * @param openTime 开放时间
 * @param reservationRequired 是否建议预约
 * @param ticketInfo 门票信息
 * @param holidayRisk 节假日风险等级
 * @param officialSourceUrl 官方或参考来源链接
 * @param source 数据来源
 * @param sourceStatus 来源状态
 * @param fallback 是否为降级结果
 * @param needSecondConfirm 是否需要二次确认
 * @param confidence 置信度
 * @param failureReason 失败原因
 * @param fetchedAt 获取时间
 */
public record AttractionToolPayload(
        String attractionName,
        String city,
        String openTime,
        Boolean reservationRequired,
        String ticketInfo,
        String holidayRisk,
        String officialSourceUrl,
        String source,
        String sourceStatus,
        Boolean fallback,
        Boolean needSecondConfirm,
        Double confidence,
        String failureReason,
        OffsetDateTime fetchedAt
) {
}
