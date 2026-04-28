package com.yeqian.travelagent.domain.model;

import com.yeqian.travelagent.domain.enums.EvidenceType;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 旅行证据。
 *
 * <p>把不同工具结果统一为可追溯、可评分、可展示的结构化事实信息。</p>
 *
 * @param evidenceType 证据类型
 * @param city 关联城市
 * @param title 证据标题
 * @param summary 证据摘要
 * @param keyFacts 关键事实
 * @param confidence 可信度
 * @param sourceName 来源名称
 * @param sourceUrl 来源链接
 * @param fetchedAt 获取时间
 */
public record TravelEvidence(
        EvidenceType evidenceType,
        String city,
        String title,
        String summary,
        Map<String, Object> keyFacts,
        double confidence,
        String sourceName,
        String sourceUrl,
        OffsetDateTime fetchedAt
) {
    /**
     * 创建旅行证据并规整空集合字段。
     */
    public TravelEvidence {
        keyFacts = keyFacts == null ? Map.of() : Map.copyOf(keyFacts);
    }
}
