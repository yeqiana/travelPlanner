package com.yeqian.travelagent.domain.model;

/**
 * 旅行方案评分。
 *
 * <p>描述候选方案在顺路程度、疲劳度、预算匹配和风险等维度的分数。</p>
 *
 * @param totalScore 总分
 * @param routeConvenienceScore 路线便利分
 * @param fatigueScore 疲劳度分
 * @param budgetScore 预算匹配分
 * @param attractionValueScore 景点价值分
 * @param holidayRiskScore 节假日风险分
 * @param weatherTicketRiskScore 天气和票务风险分
 * @param recommendationReason 推荐理由
 */
public record TravelScore(
        int totalScore,
        int routeConvenienceScore,
        int fatigueScore,
        int budgetScore,
        int attractionValueScore,
        int holidayRiskScore,
        int weatherTicketRiskScore,
        String recommendationReason
) {
}
