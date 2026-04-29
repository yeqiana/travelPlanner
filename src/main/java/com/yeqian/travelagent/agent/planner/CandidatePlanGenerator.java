package com.yeqian.travelagent.agent.planner;

import com.yeqian.travelagent.domain.enums.EvidenceType;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 候选方案生成器。
 *
 * <p>根据旅行意图和证据信息生成少量候选路线方案。</p>
 */
@Component
public class CandidatePlanGenerator {

    /**
     * 生成候选旅行方案。
     *
     * @param intent 旅行意图
     * @param evidences 旅行证据列表
     * @return 候选旅行方案列表
     */
    public List<TravelCandidatePlan> generate(TravelIntent intent, List<TravelEvidence> evidences) {
        String departure = isBlank(intent.departureCity()) ? "出发地" : intent.departureCity();
        List<String> destinations = normalizedDestinations(intent);
        EvidenceSummary evidenceSummary = summarizeEvidence(evidences);
        boolean hasHangzhou = destinations.contains("杭州");
        boolean hasShanghai = destinations.stream().anyMatch(item -> item.contains("上海"));
        if (hasHangzhou && hasShanghai) {
            return withEvidence(List.of(
                    new TravelCandidatePlan("杭州 + 上海低疲劳精选", List.of(departure, "杭州", "上海", departure), "跨城次数少，优先保留杭州和上海核心体验，适合不想太累的用户。"),
                    new TravelCandidatePlan("杭州 + 南浔 + 上海顺路游", List.of(departure, "杭州", "南浔", "上海", departure), "南浔位于江浙沪路线中间，能补充古镇体验，整体仍保持顺路。"),
                    new TravelCandidatePlan("杭州 + 苏州 + 上海经典游", List.of(departure, "杭州", "苏州", "上海", departure), "经典城市组合交通便利，景点价值高，但节假日热门城市人流风险更高。")
            ), evidenceSummary);
        }

        if (destinations.size() >= 2) {
            return withEvidence(multiCityPlans(departure, destinations), evidenceSummary);
        }

        String destination = destinations.isEmpty() ? "目的地待推荐" : destinations.get(0);
        return withEvidence(List.of(
                new TravelCandidatePlan(destination + "轻量游", List.of(departure, destination, departure), "只保留一个核心目的地，交通和体力成本最低。"),
                new TravelCandidatePlan(destination + "深度游", List.of(departure, destination, destination + "周边", departure), "在同城或近郊增加体验层次，兼顾景点丰富度和可控疲劳。")
        ), evidenceSummary);
    }

    /**
     * 生成多城市通用候选方案。
     *
     * @param departure 出发城市
     * @param destinations 目的地列表
     * @return 候选旅行方案列表
     */
    private List<TravelCandidatePlan> multiCityPlans(String departure, List<String> destinations) {
        List<String> compactRoute = new ArrayList<>();
        compactRoute.add(departure);
        compactRoute.addAll(destinations.subList(0, Math.min(2, destinations.size())));
        compactRoute.add(departure);

        List<String> fullRoute = new ArrayList<>();
        fullRoute.add(departure);
        fullRoute.addAll(destinations);
        fullRoute.add(departure);

        return List.of(
                new TravelCandidatePlan(String.join(" + ", compactRoute.subList(1, compactRoute.size() - 1)) + "轻量串联", compactRoute, "压缩跨城数量，优先匹配低疲劳和预算可控。"),
                new TravelCandidatePlan(String.join(" + ", destinations) + "完整串联", fullRoute, "覆盖用户提到的主要目的地，体验更完整，但跨城强度更高。")
        );
    }

    /**
     * 给候选方案补充证据引用和保守说明。
     *
     * @param plans 原始候选方案
     * @param evidenceSummary 证据摘要
     * @return 带证据引用的候选方案
     */
    private List<TravelCandidatePlan> withEvidence(List<TravelCandidatePlan> plans, EvidenceSummary evidenceSummary) {
        if (evidenceSummary.evidenceRefs().isEmpty()) {
            return plans;
        }
        return plans.stream()
                .map(plan -> new TravelCandidatePlan(plan.name(), plan.route(), plan.reason() + evidenceReason(evidenceSummary), evidenceSummary.evidenceRefs()))
                .toList();
    }

    /**
     * 汇总路线和景点证据。
     *
     * @param evidences 旅行证据列表
     * @return 证据摘要
     */
    private EvidenceSummary summarizeEvidence(List<TravelEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return new EvidenceSummary(List.of(), false, false, false);
        }
        List<String> refs = new ArrayList<>();
        boolean routeRisk = false;
        boolean attractionRisk = false;
        boolean secondConfirm = false;
        for (TravelEvidence evidence : evidences) {
            if (evidence == null || (evidence.evidenceType() != EvidenceType.ROUTE && evidence.evidenceType() != EvidenceType.ATTRACTION)) {
                continue;
            }
            refs.add(evidenceRef(evidence));
            Map<String, Object> facts = evidence.keyFacts();
            routeRisk = routeRisk || evidence.evidenceType() == EvidenceType.ROUTE
                    && ("HIGH".equalsIgnoreCase(textFact(facts, "routeRisk")) || intFact(facts, "durationMinutes") >= 240);
            attractionRisk = attractionRisk || evidence.evidenceType() == EvidenceType.ATTRACTION
                    && (boolFact(facts, "reservationRequired") || "HIGH".equalsIgnoreCase(textFact(facts, "holidayRisk")));
            secondConfirm = secondConfirm || boolFact(facts, "fallback") || boolFact(facts, "needSecondConfirm")
                    || "FAILED".equalsIgnoreCase(textFact(facts, "sourceStatus"));
        }
        return new EvidenceSummary(refs.stream().distinct().toList(), routeRisk, attractionRisk, secondConfirm);
    }

    /**
     * 构造证据驱动的推荐说明。
     *
     * @param evidenceSummary 证据摘要
     * @return 推荐说明补充文本
     */
    private String evidenceReason(EvidenceSummary evidenceSummary) {
        List<String> reasons = new ArrayList<>();
        if (evidenceSummary.routeRisk()) {
            reasons.add("路线证据提示部分交通耗时或风险偏高，需预留缓冲");
        }
        if (evidenceSummary.attractionRisk()) {
            reasons.add("景点证据提示节假日预约或人流风险");
        }
        if (evidenceSummary.secondConfirm()) {
            reasons.add("部分证据为降级或需二次确认，推荐结论按保守口径处理");
        }
        if (reasons.isEmpty()) {
            reasons.add("已参考路线和景点证据");
        }
        return " " + String.join("；", reasons) + "。";
    }

    /**
     * 生成证据引用文本。
     *
     * @param evidence 旅行证据
     * @return 证据引用文本
     */
    private String evidenceRef(TravelEvidence evidence) {
        String title = isBlank(evidence.title()) ? evidence.evidenceType().name() : evidence.title();
        return title + "/" + (isBlank(evidence.sourceName()) ? "UNKNOWN" : evidence.sourceName());
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

    /**
     * 规整目的地偏好。
     *
     * @param intent 旅行意图
     * @return 去重后的目的地列表
     */
    private List<String> normalizedDestinations(TravelIntent intent) {
        return intent.destinationPreferences().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    /**
     * 判断文本是否为空。
     *
     * @param value 待判断文本
     * @return 为空时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 候选方案使用的证据摘要。
     *
     * @param evidenceRefs 证据引用列表
     * @param routeRisk 是否存在路线风险
     * @param attractionRisk 是否存在景点风险
     * @param secondConfirm 是否需要二次确认
     */
    private record EvidenceSummary(
            List<String> evidenceRefs,
            boolean routeRisk,
            boolean attractionRisk,
            boolean secondConfirm
    ) {
    }
}
