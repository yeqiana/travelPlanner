package com.yeqian.travelagent.agent.scorer;

import com.yeqian.travelagent.domain.enums.EvidenceType;
import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelScore;
import com.yeqian.travelagent.domain.model.TravelScoreDetail;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 旅行方案评分器。
 *
 * <p>根据路线便利度、疲劳度、预算和风险等维度对候选方案进行排序。</p>
 */
@Component
public class TravelScorer {

    private static final double ROUTE_CONVENIENCE_WEIGHT = 0.25;
    private static final double COST_WEIGHT = 0.20;
    private static final double FATIGUE_WEIGHT = 0.25;
    private static final double ATTRACTION_VALUE_WEIGHT = 0.15;
    private static final double HOLIDAY_RISK_WEIGHT = 0.10;
    private static final double WEATHER_TICKET_RISK_WEIGHT = 0.05;

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
        EvidenceView evidenceView = new EvidenceView(evidences);
        return candidates.stream()
                .map(candidate -> new ScoredTravelPlan(candidate, scoreOne(candidate, intent, evidenceView)))
                .sorted(Comparator.comparingInt((ScoredTravelPlan plan) -> plan.score().totalScore()).reversed())
                .toList();
    }

    /**
     * 评分单个候选方案。
     *
     * @param candidate 候选旅行方案
     * @param intent 旅行意图
     * @param evidenceView 证据视图
     * @return 旅行评分
     */
    private TravelScore scoreOne(TravelCandidatePlan candidate, TravelIntent intent, EvidenceView evidenceView) {
        int destinationCount = Math.max(1, candidate.route().size() - 2);
        ScoreItem routeConvenience = routeConvenienceScore(candidate, evidenceView, destinationCount);
        ScoreItem fatigue = fatigueScore(candidate, intent, evidenceView, destinationCount);
        ScoreItem cost = budgetScore(candidate, intent, evidenceView, destinationCount);
        ScoreItem attraction = attractionValueScore(candidate, evidenceView, destinationCount);
        ScoreItem holidayRisk = holidayRiskScore(intent, evidenceView);
        ScoreItem weatherTicketRisk = weatherTicketRiskScore(evidenceView);
        int total = calculateTotal(routeConvenience.score(), fatigue.score(), cost.score(), attraction.score(), holidayRisk.score(), weatherTicketRisk.score());
        List<TravelScoreDetail> details = List.of(
                detail("ROUTE_CONVENIENCE", routeConvenience, ROUTE_CONVENIENCE_WEIGHT),
                detail("COST", cost, COST_WEIGHT),
                detail("FATIGUE", fatigue, FATIGUE_WEIGHT),
                detail("ATTRACTION_VALUE", attraction, ATTRACTION_VALUE_WEIGHT),
                detail("HOLIDAY_RISK", holidayRisk, HOLIDAY_RISK_WEIGHT),
                detail("WEATHER_TICKET_RISK", weatherTicketRisk, WEATHER_TICKET_RISK_WEIGHT)
        );
        String reason = "按交通顺路、疲劳、预算、景点价值、节假日和天气票务风险加权计算，当前方案总分为 " + total + "。";
        return new TravelScore(total, routeConvenience.score(), fatigue.score(), cost.score(), attraction.score(), holidayRisk.score(), weatherTicketRisk.score(), reason, details);
    }

    /**
     * 计算交通顺路程度分数。
     *
     * @param candidate 候选旅行方案
     * @param evidenceView 证据视图
     * @param destinationCount 目的地数量
     * @return 分数项
     */
    private ScoreItem routeConvenienceScore(TravelCandidatePlan candidate, EvidenceView evidenceView, int destinationCount) {
        int score = 96 - Math.max(0, destinationCount - 1) * 8;
        int totalRouteMinutes = evidenceView.totalRouteMinutes();
        if (totalRouteMinutes >= 360) {
            score -= 18;
        } else if (totalRouteMinutes >= 240) {
            score -= 10;
        }
        if (evidenceView.hasHighRouteRisk()) {
            score -= 12;
        }
        if (evidenceView.hasFallbackOrFailedRoute()) {
            score -= 6;
        }
        String reason = "根据路线数量、跨城数量和路线风险计算。";
        if (totalRouteMinutes > 0) {
            reason = reason + " 已参考路线耗时约 " + totalRouteMinutes + " 分钟。";
        }
        if (evidenceView.hasFallbackOrFailedRoute()) {
            reason = reason + " 部分路线证据为降级或失败结果，仅按二次确认风险保守扣分。";
        }
        return new ScoreItem(clamp(score), reason, refs(candidate, evidenceView.routeRefs()));
    }

    /**
     * 计算疲劳维度分数。
     *
     * @param candidate 候选旅行方案
     * @param intent 旅行意图
     * @param evidenceView 证据视图
     * @param destinationCount 目的地数量
     * @return 分数项
     */
    private ScoreItem fatigueScore(TravelCandidatePlan candidate, TravelIntent intent, EvidenceView evidenceView, int destinationCount) {
        int score = 96 - Math.max(0, destinationCount - 1) * 10;
        if (prefersLowFatigue(intent) && destinationCount >= 3) {
            score -= 20;
        }
        if (intent.days() != null && intent.days() > 0 && destinationCount > intent.days()) {
            score -= 15;
        }
        if (evidenceView.totalRouteMinutes() >= 300) {
            score -= 10;
        }
        if (evidenceView.hasHighRouteRisk()) {
            score -= 6;
        }
        String reason = "根据跨城次数、旅行天数和用户疲劳偏好计算。";
        if (prefersLowFatigue(intent)) {
            reason = reason + " 用户偏好不想太累，高跨城方案会额外扣分。";
        }
        return new ScoreItem(clamp(score), reason, refs(candidate, evidenceView.routeRefs()));
    }

    /**
     * 计算预算匹配分数。
     *
     * @param candidate 候选旅行方案
     * @param intent 旅行意图
     * @param evidenceView 证据视图
     * @param destinationCount 目的地数量
     * @return 分数项
     */
    private ScoreItem budgetScore(TravelCandidatePlan candidate, TravelIntent intent, EvidenceView evidenceView, int destinationCount) {
        if (intent.budget() == null || intent.budget().compareTo(BigDecimal.ZERO) <= 0) {
            return new ScoreItem(80, "用户未提供明确总预算，按中性预算匹配处理。", refs(candidate, evidenceView.costRefs()));
        }
        BigDecimal estimatedCost = estimatedCost(candidate, intent, destinationCount);
        double ratio = estimatedCost.doubleValue() / intent.budget().doubleValue();
        int score;
        if (ratio <= 0.9) {
            score = 92;
        } else if (ratio <= 1.0) {
            score = 84;
        } else if (ratio <= 1.2) {
            score = 70;
        } else if (ratio <= 1.5) {
            score = 55;
        } else {
            score = 40;
        }
        if (evidenceView.hasHighTicketRisk()) {
            score -= 5;
        }
        String reason = "按规则粗估交通、住宿、景点和餐饮成本后与用户预算比较；实时票价和酒店价格不做确定性承诺。";
        return new ScoreItem(clamp(score), reason, refs(candidate, evidenceView.costRefs()));
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
     * @param evidenceView 证据视图
     * @param destinationCount 目的地数量
     * @return 分数项
     */
    private ScoreItem attractionValueScore(TravelCandidatePlan candidate, EvidenceView evidenceView, int destinationCount) {
        int score = 76 + Math.min(destinationCount, 3) * 5;
        if (candidate.name().contains("经典") || candidate.name().contains("南浔") || candidate.name().contains("深度")) {
            score += 5;
        }
        score += Math.min(6, evidenceView.successAttractionCount() * 2);
        if (evidenceView.hasFallbackOrFailedAttraction()) {
            score -= 4;
        }
        String reason = "根据目的地丰富度、方案主题和可用景点证据计算。";
        if (evidenceView.hasFallbackOrFailedAttraction()) {
            reason = reason + " 降级或失败的景点证据不作为确定开放状态加分。";
        }
        return new ScoreItem(clamp(score), reason, refs(candidate, evidenceView.attractionRefs()));
    }

    /**
     * 计算节假日风险分数。
     *
     * @param intent 旅行意图
     * @param evidenceView 证据视图
     * @return 分数项
     */
    private ScoreItem holidayRiskScore(TravelIntent intent, EvidenceView evidenceView) {
        int score = isHoliday(intent.dateText()) ? 76 : 86;
        if (evidenceView.hasHighHolidayRisk()) {
            score -= isHoliday(intent.dateText()) ? 16 : 8;
        }
        if (evidenceView.hasReservationRequired()) {
            score -= 4;
        }
        String reason = "根据出行日期和景点节假日风险计算。";
        if (evidenceView.hasHighHolidayRisk()) {
            reason = reason + " 景点证据提示节假日风险较高。";
        }
        return new ScoreItem(clamp(score), reason, evidenceView.attractionRefs());
    }

    /**
     * 计算天气和票务风险分数。
     *
     * @param evidenceView 证据视图
     * @return 分数项
     */
    private ScoreItem weatherTicketRiskScore(EvidenceView evidenceView) {
        int score = 82;
        if (evidenceView.hasRainRisk()) {
            score -= 10;
        }
        if (evidenceView.hasHighTicketRisk()) {
            score -= 10;
        }
        if (evidenceView.hasTicketSecondConfirm()) {
            score -= 6;
        }
        if (evidenceView.hasFailedEvidence()) {
            score -= 4;
        }
        String reason = "根据天气、票务和二次确认信号计算；不把降级或失败证据当作确定余票或票价。";
        return new ScoreItem(clamp(score), reason, evidenceView.weatherTicketRefs());
    }

    /**
     * 构造评分维度明细。
     *
     * @param dimension 维度编码
     * @param item 分数项
     * @param weight 权重
     * @return 评分维度明细
     */
    private TravelScoreDetail detail(String dimension, ScoreItem item, double weight) {
        return new TravelScoreDetail(dimension, item.score(), weight, item.reason(), item.evidenceRefs());
    }

    /**
     * 合并候选方案和维度证据引用。
     *
     * @param candidate 候选方案
     * @param evidenceRefs 维度证据引用
     * @return 合并后的引用列表
     */
    private List<String> refs(TravelCandidatePlan candidate, List<String> evidenceRefs) {
        List<String> refs = new ArrayList<>();
        refs.addAll(candidate.evidenceRefs());
        refs.addAll(evidenceRefs);
        return refs.stream().filter(value -> value != null && !value.isBlank()).distinct().toList();
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
        return (int) Math.round(routeConvenience * ROUTE_CONVENIENCE_WEIGHT
                + fatigue * FATIGUE_WEIGHT
                + budget * COST_WEIGHT
                + attraction * ATTRACTION_VALUE_WEIGHT
                + holidayRisk * HOLIDAY_RISK_WEIGHT
                + weatherTicketRisk * WEATHER_TICKET_RISK_WEIGHT);
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

    /**
     * 单个维度的评分中间结果。
     *
     * @param score 分数
     * @param reason 原因
     * @param evidenceRefs 证据引用
     */
    private record ScoreItem(int score, String reason, List<String> evidenceRefs) {
    }

    /**
     * 面向评分的证据读取视图。
     *
     * @param evidences 原始证据列表
     */
    private record EvidenceView(List<TravelEvidence> evidences) {

        /**
         * 创建证据视图并规整空集合。
         */
        private EvidenceView {
            evidences = evidences == null ? List.of() : evidences;
        }

        /**
         * 统计路线总耗时。
         *
         * @return 路线总耗时分钟数
         */
        private int totalRouteMinutes() {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.ROUTE)
                    .filter(this::usableForFacts)
                    .mapToInt(evidence -> intFact(evidence.keyFacts(), "durationMinutes"))
                    .sum();
        }

        /**
         * 判断是否存在高路线风险。
         *
         * @return 存在时返回 true
         */
        private boolean hasHighRouteRisk() {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.ROUTE)
                    .anyMatch(evidence -> "HIGH".equalsIgnoreCase(textFact(evidence.keyFacts(), "routeRisk")));
        }

        /**
         * 判断路线证据是否存在降级或失败。
         *
         * @return 存在时返回 true
         */
        private boolean hasFallbackOrFailedRoute() {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.ROUTE)
                    .anyMatch(this::fallbackOrFailed);
        }

        /**
         * 判断景点证据是否存在降级或失败。
         *
         * @return 存在时返回 true
         */
        private boolean hasFallbackOrFailedAttraction() {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.ATTRACTION)
                    .anyMatch(this::fallbackOrFailed);
        }

        /**
         * 统计成功景点证据数量。
         *
         * @return 成功景点证据数量
         */
        private int successAttractionCount() {
            return (int) evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.ATTRACTION)
                    .filter(this::usableForFacts)
                    .count();
        }

        /**
         * 判断是否存在高节假日风险。
         *
         * @return 存在时返回 true
         */
        private boolean hasHighHolidayRisk() {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.ATTRACTION)
                    .anyMatch(evidence -> "HIGH".equalsIgnoreCase(textFact(evidence.keyFacts(), "holidayRisk")));
        }

        /**
         * 判断是否存在预约要求。
         *
         * @return 存在时返回 true
         */
        private boolean hasReservationRequired() {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.ATTRACTION)
                    .anyMatch(evidence -> boolFact(evidence.keyFacts(), "reservationRequired"));
        }

        /**
         * 判断是否存在降雨风险。
         *
         * @return 存在时返回 true
         */
        private boolean hasRainRisk() {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.WEATHER)
                    .anyMatch(evidence -> {
                        String risk = textFact(evidence.keyFacts(), "rainRisk");
                        return "HIGH".equalsIgnoreCase(risk) || "MEDIUM".equalsIgnoreCase(risk);
                    });
        }

        /**
         * 判断是否存在高票务风险。
         *
         * @return 存在时返回 true
         */
        private boolean hasHighTicketRisk() {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.TRANSPORT || evidence.evidenceType() == EvidenceType.ATTRACTION)
                    .anyMatch(evidence -> "HIGH".equalsIgnoreCase(textFact(evidence.keyFacts(), "ticketRisk")));
        }

        /**
         * 判断是否存在票务二次确认。
         *
         * @return 存在时返回 true
         */
        private boolean hasTicketSecondConfirm() {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.ATTRACTION || evidence.evidenceType() == EvidenceType.TRANSPORT)
                    .anyMatch(evidence -> textFact(evidence.keyFacts(), "ticketInfo").contains("需二次确认")
                            || boolFact(evidence.keyFacts(), "needSecondConfirm"));
        }

        /**
         * 判断是否存在失败证据。
         *
         * @return 存在时返回 true
         */
        private boolean hasFailedEvidence() {
            return evidences.stream().anyMatch(evidence -> "FAILED".equalsIgnoreCase(textFact(evidence.keyFacts(), "sourceStatus")));
        }

        /**
         * 获取路线证据引用。
         *
         * @return 路线证据引用
         */
        private List<String> routeRefs() {
            return refsByType(EvidenceType.ROUTE);
        }

        /**
         * 获取景点证据引用。
         *
         * @return 景点证据引用
         */
        private List<String> attractionRefs() {
            return refsByType(EvidenceType.ATTRACTION);
        }

        /**
         * 获取成本相关证据引用。
         *
         * @return 成本相关证据引用
         */
        private List<String> costRefs() {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.HOTEL || evidence.evidenceType() == EvidenceType.TRANSPORT || evidence.evidenceType() == EvidenceType.ROUTE)
                    .map(this::evidenceRef)
                    .distinct()
                    .toList();
        }

        /**
         * 获取天气和票务证据引用。
         *
         * @return 天气和票务证据引用
         */
        private List<String> weatherTicketRefs() {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == EvidenceType.WEATHER || evidence.evidenceType() == EvidenceType.TRANSPORT || evidence.evidenceType() == EvidenceType.ATTRACTION)
                    .map(this::evidenceRef)
                    .distinct()
                    .toList();
        }

        /**
         * 按类型获取证据引用。
         *
         * @param evidenceType 证据类型
         * @return 证据引用列表
         */
        private List<String> refsByType(EvidenceType evidenceType) {
            return evidences.stream()
                    .filter(evidence -> evidence.evidenceType() == evidenceType)
                    .map(this::evidenceRef)
                    .distinct()
                    .toList();
        }

        /**
         * 判断证据能否作为事实参与加分。
         *
         * @param evidence 旅行证据
         * @return 可用时返回 true
         */
        private boolean usableForFacts(TravelEvidence evidence) {
            return !fallbackOrFailed(evidence) && evidence.confidence() >= 0.5;
        }

        /**
         * 判断证据是否降级或失败。
         *
         * @param evidence 旅行证据
         * @return 降级或失败时返回 true
         */
        private boolean fallbackOrFailed(TravelEvidence evidence) {
            Map<String, Object> facts = evidence.keyFacts();
            String status = textFact(facts, "sourceStatus");
            return boolFact(facts, "fallback") || "FAILED".equalsIgnoreCase(status) || "FALLBACK".equalsIgnoreCase(status);
        }

        /**
         * 生成证据引用文本。
         *
         * @param evidence 旅行证据
         * @return 证据引用文本
         */
        private String evidenceRef(TravelEvidence evidence) {
            String title = evidence.title() == null || evidence.title().isBlank() ? evidence.evidenceType().name() : evidence.title();
            String source = evidence.sourceName() == null || evidence.sourceName().isBlank() ? "UNKNOWN" : evidence.sourceName();
            return title + "/" + source;
        }

        /**
         * 获取文本事实。
         *
         * @param facts 关键事实
         * @param key 字段名
         * @return 文本事实
         */
        private String textFact(Map<String, Object> facts, String key) {
            Object value = facts == null ? null : facts.get(key);
            return value == null ? "" : String.valueOf(value);
        }

        /**
         * 获取整数事实。
         *
         * @param facts 关键事实
         * @param key 字段名
         * @return 整数事实
         */
        private int intFact(Map<String, Object> facts, String key) {
            Object value = facts == null ? null : facts.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (Exception exception) {
                return 0;
            }
        }

        /**
         * 获取布尔事实。
         *
         * @param facts 关键事实
         * @param key 字段名
         * @return 布尔事实
         */
        private boolean boolFact(Map<String, Object> facts, String key) {
            Object value = facts == null ? null : facts.get(key);
            return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
        }
    }
}
