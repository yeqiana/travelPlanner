package com.yeqian.travelagent.agent.generator;

import com.yeqian.travelagent.domain.enums.EvidenceType;
import com.yeqian.travelagent.domain.enums.ReminderType;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelReminder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 旅行提醒生成器。
 *
 * <p>根据旅行意图和最终计划生成购票、预约和出发准备提醒。</p>
 */
@Component
public class ReminderGenerator {

    /**
     * 生成旅行提醒列表。
     *
     * @param intent 旅行意图
     * @param plan 最终旅行计划
     * @return 旅行提醒列表
     */
    public List<TravelReminder> generate(TravelIntent intent, TravelPlan plan) {
        return generate(intent, plan, List.of());
    }

    /**
     * 根据证据生成旅行提醒列表。
     *
     * @param intent 旅行意图
     * @param plan 最终旅行计划
     * @param evidences 旅行证据列表
     * @return 旅行提醒列表
     */
    public List<TravelReminder> generate(TravelIntent intent, TravelPlan plan, List<TravelEvidence> evidences) {
        String departureCity = isBlank(intent.departureCity()) ? "出发地" : intent.departureCity();
        String routeText = plan == null || plan.route().isEmpty() ? "目标城市" : String.join(" -> ", plan.route());
        ReminderSignals signals = signals(evidences);
        List<String> ticketDescriptions = new ArrayList<>();
        ticketDescriptions.add("检查往返和跨城交通余票、时间、退改规则；车票/酒店价格需二次确认。");
        if (signals.routeNeedConfirm()) {
            ticketDescriptions.add("二次确认路线/交通时间、班次和换乘安排。");
        }
        if (signals.ticketNeedConfirm()) {
            ticketDescriptions.add("完成票务/门票确认。");
        }
        List<String> attractionDescriptions = new ArrayList<>();
        attractionDescriptions.add("按路线“" + routeText + "”逐一确认热门景点预约、开放时间和入园要求；景点预约需二次确认。");
        if (signals.reservationRequired()) {
            attractionDescriptions.add("预约热门景点，节假日建议提前完成。");
        }
        return List.of(
                new TravelReminder(
                        "确认" + departureCity + "出发车票/机票",
                        ReminderType.TICKET,
                        "出发前5天 09:00",
                        String.join(" ", ticketDescriptions)
                ),
                new TravelReminder(
                        "预约热门景点",
                        ReminderType.ATTRACTION,
                        "出发前3天 10:00",
                        String.join(" ", attractionDescriptions)
                ),
                new TravelReminder(
                        "完成出发前准备",
                        ReminderType.PREPARE,
                        "出发前1天 20:00",
                        "检查身份证件、充电器、雨具、常用药、酒店订单和节假日人流风险预案。"
                )
        );
    }

    /**
     * 汇总提醒证据信号。
     *
     * @param evidences 旅行证据列表
     * @return 提醒证据信号
     */
    private ReminderSignals signals(List<TravelEvidence> evidences) {
        boolean reservationRequired = false;
        boolean routeNeedConfirm = false;
        boolean ticketNeedConfirm = false;
        for (TravelEvidence evidence : evidences == null ? List.<TravelEvidence>of() : evidences) {
            Map<String, Object> facts = evidence.keyFacts();
            if (evidence.evidenceType() == EvidenceType.ATTRACTION && boolFact(facts, "reservationRequired")) {
                reservationRequired = true;
            }
            if (evidence.evidenceType() == EvidenceType.ROUTE
                    && ("HIGH".equalsIgnoreCase(textFact(facts, "routeRisk")) || boolFact(facts, "needSecondConfirm"))) {
                routeNeedConfirm = true;
            }
            if (textFact(facts, "ticketInfo").contains("需二次确认")) {
                ticketNeedConfirm = true;
            }
        }
        return new ReminderSignals(reservationRequired, routeNeedConfirm, ticketNeedConfirm);
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
     * 判断文本是否为空。
     *
     * @param value 待判断文本
     * @return 为空时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 提醒生成使用的证据信号。
     *
     * @param reservationRequired 是否需要景点预约
     * @param routeNeedConfirm 是否需要二次确认路线交通
     * @param ticketNeedConfirm 是否需要票务门票确认
     */
    private record ReminderSignals(
            boolean reservationRequired,
            boolean routeNeedConfirm,
            boolean ticketNeedConfirm
    ) {
    }
}
