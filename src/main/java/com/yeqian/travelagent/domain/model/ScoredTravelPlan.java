package com.yeqian.travelagent.domain.model;

/**
 * 已评分旅行方案。
 *
 * <p>把候选方案和评分绑定在一起，便于排序和选择推荐方案。</p>
 *
 * @param candidatePlan 候选旅行方案
 * @param score 旅行方案评分
 */
public record ScoredTravelPlan(
        TravelCandidatePlan candidatePlan,
        TravelScore score
) {
}
