package com.yeqian.travelagent.domain.model;

import java.util.List;

/**
 * 旅行评分维度明细。
 *
 * <p>描述单个评分维度的分数、权重、解释原因和使用的证据引用。</p>
 *
 * @param dimension 评分维度编码
 * @param score 维度分数
 * @param weight 维度权重
 * @param reason 评分原因
 * @param evidenceRefs 使用的证据引用列表
 */
public record TravelScoreDetail(
        String dimension,
        int score,
        double weight,
        String reason,
        List<String> evidenceRefs
) {
    /**
     * 创建旅行评分维度明细并规整空集合字段。
     */
    public TravelScoreDetail {
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
    }
}
