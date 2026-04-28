package com.yeqian.travelagent.agent.generator;

import com.yeqian.travelagent.domain.enums.ReminderType;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelReminder;
import org.springframework.stereotype.Component;

import java.util.List;

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
        String departureCity = isBlank(intent.departureCity()) ? "出发地" : intent.departureCity();
        String routeText = plan == null || plan.route().isEmpty() ? "目标城市" : String.join(" -> ", plan.route());
        return List.of(
                new TravelReminder(
                        "确认" + departureCity + "出发车票/机票",
                        ReminderType.TICKET,
                        "出发前5天 09:00",
                        "检查往返和跨城交通余票、时间、退改规则；车票/酒店价格需二次确认。"
                ),
                new TravelReminder(
                        "预约热门景点",
                        ReminderType.ATTRACTION,
                        "出发前3天 10:00",
                        "按路线“" + routeText + "”逐一确认热门景点预约、开放时间和入园要求；景点预约需二次确认。"
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
     * 判断文本是否为空。
     *
     * @param value 待判断文本
     * @return 为空时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
