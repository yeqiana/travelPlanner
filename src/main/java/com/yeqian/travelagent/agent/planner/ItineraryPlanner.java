package com.yeqian.travelagent.agent.planner;

import com.yeqian.travelagent.domain.enums.FatigueLevel;
import com.yeqian.travelagent.domain.model.DailyPlan;
import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 行程计划生成器。
 *
 * <p>根据最高分候选方案生成最终可展示的旅行计划。</p>
 */
@Component
public class ItineraryPlanner {

    /**
     * 生成最终旅行计划。
     *
     * @param intent 旅行意图
     * @param scoredPlans 已评分候选方案列表
     * @param evidences 旅行证据列表
     * @return 最终旅行计划
     */
    public TravelPlan generate(TravelIntent intent, List<ScoredTravelPlan> scoredPlans, List<TravelEvidence> evidences) {
        ScoredTravelPlan best = firstPlan(scoredPlans, intent);
        List<String> route = best.candidatePlan().route();
        List<String> travelCities = travelCities(route, intent);
        int days = normalizedDays(intent, travelCities.size());
        return new TravelPlan(
                best.candidatePlan().name() + " " + days + "天旅行计划",
                buildSummary(intent, best),
                route,
                buildDailyPlans(days, travelCities),
                buildTransportSuggestions(intent, route),
                buildHotelSuggestions(intent, travelCities),
                buildBudgetEstimate(intent, route, travelCities, days),
                buildRisks(),
                buildTodoList()
        );
    }

    /**
     * 获取最高分方案，缺省时使用用户目的地生成兜底方案。
     *
     * @param scoredPlans 已评分候选方案列表
     * @param intent 旅行意图
     * @return 最高分方案
     */
    private ScoredTravelPlan firstPlan(List<ScoredTravelPlan> scoredPlans, TravelIntent intent) {
        if (scoredPlans != null && !scoredPlans.isEmpty()) {
            return scoredPlans.get(0);
        }
        List<String> route = new ArrayList<>();
        String departure = isBlank(intent.departureCity()) ? "出发地" : intent.departureCity();
        route.add(departure);
        if (intent.destinationPreferences().isEmpty()) {
            route.add("目的地待推荐");
        } else {
            route.addAll(intent.destinationPreferences());
        }
        route.add(departure);
        return new ScoredTravelPlan(new TravelCandidatePlan("规则兜底方案", route, "根据用户已填写信息生成。"), null);
    }

    /**
     * 提取真正游玩的城市，去掉首尾出发地。
     *
     * @param route 完整路线
     * @param intent 旅行意图
     * @return 游玩城市列表
     */
    private List<String> travelCities(List<String> route, TravelIntent intent) {
        List<String> cities = new ArrayList<>();
        String departure = intent.departureCity();
        for (String city : route) {
            if (isBlank(city) || city.equals(departure)) {
                continue;
            }
            if (!cities.contains(city)) {
                cities.add(city);
            }
        }
        if (cities.isEmpty()) {
            cities.add("目的地待推荐");
        }
        return cities;
    }

    /**
     * 规整旅行天数。
     *
     * @param intent 旅行意图
     * @param cityCount 游玩城市数量
     * @return 可用于生成日程的天数
     */
    private int normalizedDays(TravelIntent intent, int cityCount) {
        if (intent.days() != null && intent.days() > 0) {
            return intent.days();
        }
        return Math.max(1, cityCount + 1);
    }

    /**
     * 构造计划摘要。
     *
     * @param intent 旅行意图
     * @param best 最高分方案
     * @return 摘要文本
     */
    private String buildSummary(TravelIntent intent, ScoredTravelPlan best) {
        String dateText = isBlank(intent.dateText()) ? "出行日期待定" : intent.dateText();
        String scoreText = best.score() == null ? "规则推荐" : "综合评分 " + best.score().totalScore() + " 分";
        return dateText + "出发，采用“" + best.candidatePlan().name() + "”，" + scoreText + "。票务、酒店和预约信息用于规划参考，实际下单前需要二次确认。";
    }

    /**
     * 构造每日计划。
     *
     * @param days 出行天数
     * @param travelCities 游玩城市列表
     * @return 每日计划列表
     */
    private List<DailyPlan> buildDailyPlans(int days, List<String> travelCities) {
        List<DailyPlan> dailyPlans = new ArrayList<>();
        for (int day = 1; day <= days; day++) {
            String city = travelCities.get(Math.min(day - 1, travelCities.size() - 1));
            boolean firstDay = day == 1;
            boolean lastDay = day == days;
            dailyPlans.add(new DailyPlan(
                    day,
                    city,
                    firstDay ? "抵达" + city + "，办理入住或寄存行李" : city + "核心景点游览",
                    lastDay ? "预留返程交通和退房时间" : city + "热门景点或街区深度游",
                    lastDay ? "返程或轻松收尾" : "安排本地餐食和夜间轻量活动",
                    fatigueLevel(day, days),
                    notes(day, firstDay, lastDay)
            ));
        }
        return dailyPlans;
    }

    /**
     * 判断每日疲劳等级。
     *
     * @param day 当前天数
     * @param days 总天数
     * @return 疲劳等级
     */
    private FatigueLevel fatigueLevel(int day, int days) {
        if (day == 1 || day == days) {
            return FatigueLevel.LOW;
        }
        return days >= 4 ? FatigueLevel.MEDIUM : FatigueLevel.LOW;
    }

    /**
     * 构造每日注意事项。
     *
     * @param day 当前天数
     * @param firstDay 是否第一天
     * @param lastDay 是否最后一天
     * @return 注意事项列表
     */
    private List<String> notes(int day, boolean firstDay, boolean lastDay) {
        List<String> notes = new ArrayList<>();
        if (firstDay) {
            notes.add("第一天降低强度，避免到达后赶景点。");
        } else {
            notes.add("热门景点尽量预约上午时段，减少排队不确定性。");
        }
        if (lastDay) {
            notes.add("最后一天预留返程缓冲，避免交通延误影响行程。");
        }
        notes.add("Day " + day + " 的具体开放时间和门票以官方平台为准。");
        return notes;
    }

    /**
     * 构造交通建议。
     *
     * @param intent 旅行意图
     * @param route 完整路线
     * @return 交通建议列表
     */
    private List<String> buildTransportSuggestions(TravelIntent intent, List<String> route) {
        String preference = isBlank(intent.transportPreference()) ? "高铁/飞机按总耗时和价格择优" : intent.transportPreference();
        return List.of(
                "路线顺序：" + String.join(" -> ", route),
                "交通偏好：" + preference + "。",
                "跨城车次、航班余票和价格需在购票平台二次确认。"
        );
    }

    /**
     * 构造酒店建议。
     *
     * @param intent 旅行意图
     * @param travelCities 游玩城市列表
     * @return 酒店建议列表
     */
    private List<String> buildHotelSuggestions(TravelIntent intent, List<String> travelCities) {
        String budget = intent.hotelBudgetPerNight() == null ? "按总预算选择经济舒适型" : "每晚约 " + intent.hotelBudgetPerNight() + " 元";
        return List.of(
                "优先选择靠近地铁、火车站或核心景区之间的区域。",
                "住宿预算：" + budget + "。",
                "重点城市：" + String.join("、", travelCities) + "，酒店价格需二次确认。"
        );
    }

    /**
     * 构造预算估算。
     *
     * @param intent 旅行意图
     * @param route 完整路线
     * @param travelCities 游玩城市列表
     * @param days 出行天数
     * @return 预算估算信息
     */
    private Map<String, Object> buildBudgetEstimate(TravelIntent intent, List<String> route, List<String> travelCities, int days) {
        int people = Math.max(1, intent.peopleCount());
        BigDecimal transport = BigDecimal.valueOf(260L * Math.max(1, route.size() - 1) * people);
        BigDecimal hotel = BigDecimal.valueOf(350L * Math.max(1, days - 1));
        BigDecimal attraction = BigDecimal.valueOf(160L * travelCities.size() * people);
        BigDecimal mealAndLocal = BigDecimal.valueOf(180L * days * people);
        Map<String, Object> estimate = new LinkedHashMap<>();
        estimate.put("transport", transport);
        estimate.put("hotel", hotel);
        estimate.put("attraction", attraction);
        estimate.put("mealAndLocal", mealAndLocal);
        estimate.put("total", transport.add(hotel).add(attraction).add(mealAndLocal));
        estimate.put("note", "当前为规则粗估，车票、酒店和门票价格需二次确认。");
        return estimate;
    }

    /**
     * 构造风险提示。
     *
     * @return 风险提示列表
     */
    private List<String> buildRisks() {
        return List.of(
                "车票/酒店价格需二次确认。",
                "景点预约需二次确认。",
                "节假日人流风险较高，热门景区建议提前预约并预留排队时间。"
        );
    }

    /**
     * 构造待办事项。
     *
     * @return 待办事项列表
     */
    private List<String> buildTodoList() {
        return List.of(
                "确认往返和跨城车票/机票。",
                "确认酒店价格、位置和取消政策。",
                "确认热门景点预约、开放时间和入园规则。",
                "出发前检查证件、充电器、雨具和常用药。"
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
