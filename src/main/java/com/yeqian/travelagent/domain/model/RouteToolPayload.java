package com.yeqian.travelagent.domain.model;

import java.time.OffsetDateTime;

/**
 * 路线工具结构化载荷。
 *
 * <p>用于在路线工具和证据归一化器之间传递结构化路线结果。</p>
 *
 * @param origin 出发地
 * @param destination 目的地
 * @param durationMinutes 路线耗时分钟数
 * @param distanceKm 路线距离公里数
 * @param transferSuggestion 换乘或出行建议
 * @param routeRisk 路线风险等级
 * @param source 数据来源
 * @param sourceStatus 来源状态
 * @param fallback 是否为降级结果
 * @param needSecondConfirm 是否需要二次确认
 * @param confidence 置信度
 * @param failureReason 失败原因
 * @param fetchedAt 获取时间
 */
public record RouteToolPayload(
        String origin,
        String destination,
        Integer durationMinutes,
        Double distanceKm,
        String transferSuggestion,
        String routeRisk,
        String source,
        String sourceStatus,
        Boolean fallback,
        Boolean needSecondConfirm,
        Double confidence,
        String failureReason,
        OffsetDateTime fetchedAt
) {
}
