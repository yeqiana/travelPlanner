package com.yeqian.travelagent.agent.scorer;

import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelScore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 旅行方案评分器。
 *
 * <p>根据路线便利度、疲劳度、预算和风险等维度对候选方案进行排序。</p>
 */
@Component
public class TravelScorer {

    /**
     * 批量评分候选方案。
     *
     * @param candidates 候选旅行方案列表
     * @param intent 旅行意图
     * @param evidences 旅行证据列表
     * @return 已评分并按总分倒序排列的方案列表
     */
    public List<ScoredTravelPlan> score(List<TravelCandidatePlan> candidates, TravelIntent intent, List<TravelEvidence> evidences) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream()
                .map(candidate -> new ScoredTravelPlan(candidate, scoreOne(candidate, intent)))
                .sorted(Comparator.comparingInt((ScoredTravelPlan plan) -> plan.score().totalScore()).reversed())
                .toList();
    }

    /**
     * 评分单个候选方案。
     *
     * @param candidate 候选旅行方案
     * @param intent 旅行意图
     * @return 旅行评分
     */
    private TravelScore scoreOne(TravelCandidatePlan candidate, TravelIntent intent) {
        int destinationCount = Math.max(1, candidate.route().size() - 2);
        int routeConvenience = clamp(96 - Math.max(0, destinationCount - 1) * 8);
        int fatigue = fatigueScore(destinationCount, intent);
        int budget = budgetScore(candidate, intent, destinationCount);
        int attraction = attractionValueScore(candidate, destinationCount);
        int holidayRisk = holidayRiskScore(candidate, intent);
        int weatherTicketRisk = weatherTicketRiskScore();
        int total = calculateTotal(routeConvenience, fatigue, budget, attraction, holidayRisk, weatherTicketRisk);
        String reason = "按交通顺路、疲劳、预算、景点价值、节假日和天气票务风险加权计算，当前方案总分为 " + total + "。";
        return new TravelScore(total, routeConvenience, fatigue, budget, attraction, holidayRisk, weatherTicketRisk, reason);
    }

    /**
     * 计算疲劳维度分数。
     *
     * @param destinationCount 目的地数量
     * @param intent 旅行意图
     * @return 疲劳维度分数
     */
    private int fatigueScore(int destinationCount, TravelIntent intent) {
        int score = 96 - Math.max(0, destinationCount - 1) * 10;
        if (prefersLowFatigue(intent) && destinationCount >= 3) {
            score -= 20;
        }
        if (intent.days() != null && intent.days() > 0 && destinationCount > intent.days()) {
            score -= 15;
        }
        return clamp(score);
    }

    /**
     * 计算预算匹配分数。
     *
     * @param candidate 候选旅行方案
     * @param intent 旅行意图
     * @param destinationCount 目的地数量
     * @return 预算匹配分数
     */
    private int budgetScore(TravelCandidatePlan candidate, TravelIntent intent, int destinationCount) {
        if (intent.budget() == null || intent.budget().compareTo(BigDecimal.ZERO) <= 0) {
            return 80;
        }
        BigDecimal estimatedCost = estimatedCost(candidate, intent, destinationCount);
        double ratio = estimatedCost.doubleValue() / intent.budget().doubleValue();
        if (ratio <= 0.9) {
            return 92;
        }
        if (ratio <= 1.0) {
            return 84;
        }
        if (ratio <= 1.2) {
            return 70;
        }
        if (ratio <= 1.5) {
            return 55;
        }
        return 40;
    }

    /**
     * 粗估候选方案成本。
     *
     * @param candidate 候选旅行方案
     * @param intent 旅行意图
     * @param destinationCount 目的地数量
     * @return 粗估总成本
     */
    private BigDecimal estimatedCost(TravelCandidatePlan candidate, TravelIntent intent, int destinationCount) {
        int people = Math.max(1, intent.peopleCount());
        int days = intent.days() == null || intent.days() <= 0 ? Math.max(2, destinationCount + 1) : intent.days();
        int crossCityLegs = Math.max(1, candidate.route().size() - 1);
        BigDecimal transport = BigDecimal.valueOf(260L * crossCityLegs * people);
        BigDecimal hotel = BigDecimal.valueOf(350L * Math.max(1, days - 1));
        BigDecimal attraction = BigDecimal.valueOf(160L * destinationCount * people);
        BigDecimal mealAndLocal = BigDecimal.valueOf(180L * days * people);
        return transport.add(hotel).add(attraction).add(mealAndLocal);
    }

    /**
     * 计算景点价值分数。
     *
     * @param candidate 候选旅行方案
     * @param destinationCount 目的地数量
     * @return 景点价值分数
     */
    private int attractionValueScore(TravelCandidatePlan candidate, int destinationCount) {
        int score = 76 + Math.min(destinationCount, 3) * 5;
        if (candidate.name().contains("经典") || candidate.name().contains("南浔") || candidate.name().contains("深度")) {
            score += 5;
        }
        return clamp(score);
    }

    /**
     * 计算节假日风险分数。
     *
     * @param candidate 候选旅行方案
     * @param intent 旅行意图
     * @return 节假日风险分数
     */
    private int holidayRiskScore(TravelCandidatePlan candidate, TravelIntent intent) {
        int score = isHoliday(intent.dateText()) ? 76 : 86;
        if (candidate.name().contains("苏州") || candidate.name().contains("上海") || candidate.name().contains("杭州")) {
            score -= isHoliday(intent.dateText()) ? 6 : 2;
        }
        return clamp(score);
    }

    /**
     * 计算天气和票务风险分数。
     *
     * @return 天气和票务风险分数
     */
    private int weatherTicketRiskScore() {
        return 78;
    }

    /**
     * 按固定权重计算总分。
     *
     * @param routeConvenience 交通顺路程度分
     * @param fatigue 疲劳程度分
     * @param budget 预算匹配分
     * @param attraction 景点价值分
     * @param holidayRisk 节假日风险分
     * @param weatherTicketRisk 天气和票务风险分
     * @return 总分
     */
    private int calculateTotal(int routeConvenience, int fatigue, int budget, int attraction, int holidayRisk, int weatherTicketRisk) {
        return (int) Math.round(routeConvenience * 0.25
                + fatigue * 0.25
                + budget * 0.20
                + attraction * 0.15
                + holidayRisk * 0.10
                + weatherTicketRisk * 0.05);
    }

    /**
     * 判断用户是否偏好低疲劳。
     *
     * @param intent 旅行意图
     * @return 偏好低疲劳时返回 true
     */
    private boolean prefersLowFatigue(TravelIntent intent) {
        return intent.travelStyles().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains("不想太累") || value.contains("轻松") || value.contains("低疲劳"));
    }

    /**
     * 判断日期文本是否为节假日。
     *
     * @param dateText 日期文本
     * @return 节假日返回 true
     */
    private boolean isHoliday(String dateText) {
        if (dateText == null) {
            return false;
        }
        return dateText.contains("五一") || dateText.contains("国庆") || dateText.contains("春节") || dateText.contains("假期");
    }

    /**
     * 限制分数在 0 到 100 之间。
     *
     * @param score 原始分数
     * @return 规整后的分数
     */
    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
