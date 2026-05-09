package com.yeqian.travelagent.agent.planner;

import com.yeqian.travelagent.domain.enums.EvidenceType;
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

    private static final Map<String, List<String>> CITY_ATTRACTIONS = Map.of(
            "杭州", List.of("西湖苏堤", "灵隐寺", "河坊街"),
            "上海", List.of("外滩", "豫园", "陆家嘴"),
            "西安", List.of("陕西历史博物馆", "大雁塔", "回民街"),
            "宝鸡", List.of("法门寺文化景区", "太白山游客中心", "陈仓老街")
    );

    private static final Map<String, String> CITY_RESTAURANTS = Map.of(
            "杭州", "湖滨银泰或河坊街周边餐厅",
            "上海", "南京东路或豫园周边餐厅",
            "西安", "大雁塔或回民街周边餐厅",
            "宝鸡", "陈仓老街周边餐厅"
    );

    private static final Map<String, String> CITY_DAY_THEME = Map.of(
            "杭州", "西湖西线与城市烟火慢游",
            "上海", "外滩城市景观与老城街区轻游",
            "西安", "历史博物馆与城南夜景慢游",
            "宝鸡", "人文景区与老街低强度游览"
    );

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
                buildDailyPlans(days, travelCities, evidences),
                buildTransportSuggestions(intent, route, evidences),
                buildHotelSuggestions(intent, travelCities),
                buildBudgetEstimate(intent, route, travelCities, days),
                buildRisks(evidences),
                buildTodoList(evidences)
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
    private List<DailyPlan> buildDailyPlans(int days, List<String> travelCities, List<TravelEvidence> evidences) {
        List<DailyPlan> dailyPlans = new ArrayList<>();
        for (int day = 1; day <= days; day++) {
            String city = travelCities.get(Math.min(day - 1, travelCities.size() - 1));
            boolean firstDay = day == 1;
            boolean lastDay = day == days;
            TravelEvidence attraction = firstAttractionEvidence(city, evidences);
            String attractionName = attractionName(attraction, city);
            List<String> places = dayPlaces(city, attractionName);
            dailyPlans.add(new DailyPlan(
                    day,
                    city,
                    buildMorningPlan(city, places, firstDay),
                    buildAfternoonPlan(city, places, attractionName, lastDay),
                    buildEveningPlan(city, places, lastDay),
                    fatigueLevel(day, days),
                    notes(day, firstDay, lastDay, attraction)
            ));
        }
        return dailyPlans;
    }

    /**
     * 构造上午详细安排。
     *
     * @param city 当前城市
     * @param places 当天候选地点
     * @param firstDay 是否第一天
     * @return 上午详细安排
     */
    private String buildMorningPlan(String city, List<String> places, boolean firstDay) {
        String theme = cityTheme(city);
        if (firstDay) {
            return formatSegment(
                    "08:30-12:00",
                    places.get(0),
                    "围绕“" + theme + "”从酒店或交通站前往并游览" + places.get(0),
                    "建议地铁/打车，路况需二次确认",
                    "预计3.5小时",
                    "路况、开放时间、预约和门票需二次确认"
            );
        }
        return formatSegment(
                "09:00-12:30",
                places.get(0),
                "按“" + theme + "”主线游览" + places.get(0) + "并在附近午餐",
                "建议地铁/打车前往，午餐步行或短途打车衔接",
                "预计3.5小时",
                "开放、预约和餐厅排队情况需二次确认"
        );
    }

    /**
     * 构造下午详细安排。
     *
     * @param city 当前城市
     * @param places 当天候选地点
     * @param attractionName 证据推荐景点
     * @param lastDay 是否最后一天
     * @return 下午详细安排
     */
    private String buildAfternoonPlan(String city, List<String> places, String attractionName, boolean lastDay) {
        if (lastDay) {
            return formatSegment(
                    "13:30-17:00",
                    places.get(1),
                    "前往" + places.get(1) + "或" + attractionName + "做低强度补充游览，并预留行李寄存和返程缓冲",
                    "建议地铁/打车并提前规划返回酒店/车站路线",
                    "预计3.5小时",
                    "开放状态、行李寄存和当天路况需二次确认"
            );
        }
        return formatSegment(
                "13:30-17:30",
                places.get(1),
                "游览" + places.get(1) + "和" + attractionName + "，结束后前往" + places.get(2) + "周边休整",
                "建议地铁/打车衔接，避免连续远距离步行",
                "预计4小时",
                "门票、预约和营业状态需二次确认"
        );
    }

    /**
     * 构造晚上详细安排。
     *
     * @param city 当前城市
     * @param places 当天候选地点
     * @param lastDay 是否最后一天
     * @return 晚上详细安排
     */
    private String buildEveningPlan(String city, List<String> places, boolean lastDay) {
        String restaurant = CITY_RESTAURANTS.getOrDefault(city, city + "当地特色餐厅");
        if (lastDay) {
            return formatSegment(
                    "18:00-20:30",
                    restaurant,
                    "用餐或打包简餐后前往返程交通点",
                    "步行/短途打车优先，返程班次需提前核对",
                    "预计2.5小时",
                    "营业时间、班次和进站时间需二次确认"
            );
        }
        return formatSegment(
                "18:30-21:00",
                restaurant,
                "晚餐后步行体验" + places.get(2) + "夜间街区，体力不足可取消并直接返回酒店休息",
                "优先选择离当日最后景点近的位置，步行或短途打车返回",
                "预计2.5小时",
                "营业、排队和夜间返程路况需二次确认"
        );
    }

    /**
     * 构造 P6 兼容增强格式的时间段文本。
     *
     * @param timeRange 时间范围
     * @param place 地点
     * @param arrangement 安排内容
     * @param transport 交通建议
     * @param duration 预计耗时
     * @param confirm 二次确认提示
     * @return 标准化时间段文本
     */
    private String formatSegment(String timeRange, String place, String arrangement, String transport, String duration, String confirm) {
        return timeRange
                + " 地点：" + place
                + "；安排：" + arrangement
                + "；交通：" + transport
                + "；耗时：" + duration
                + "；确认：" + confirm + "。";
    }

    /**
     * 获取当天可用地点列表。
     *
     * @param city 当前城市
     * @param attractionName 证据推荐景点
     * @return 至少三个地点名称
     */
    private List<String> dayPlaces(String city, String attractionName) {
        List<String> places = new ArrayList<>(CITY_ATTRACTIONS.getOrDefault(city, List.of(
                city + "城市地标候选点（需二次确认）",
                city + "博物馆或公园候选点（需二次确认）",
                city + "特色街区候选点（需二次确认）"
        )));
        if (!isBlank(attractionName) && !attractionName.equals(city) && !places.contains(attractionName)) {
            places.set(1, attractionName);
        }
        while (places.size() < 3) {
            places.add(city + "待确认地点" + places.size());
        }
        return places;
    }

    /**
     * 获取城市当日主线主题。
     *
     * @param city 当前城市
     * @return 城市主线主题
     */
    private String cityTheme(String city) {
        return CITY_DAY_THEME.getOrDefault(city, city + "低强度候选路线，具体地点需二次确认");
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
    private List<String> notes(int day, boolean firstDay, boolean lastDay, TravelEvidence attraction) {
        List<String> notes = new ArrayList<>();
        if (firstDay) {
            notes.add("第一天降低强度，避免到达后赶景点。");
        } else {
            notes.add("热门景点尽量预约上午时段，减少排队不确定性。");
        }
        if (lastDay) {
            notes.add("最后一天预留返程缓冲，避免交通延误影响行程。");
        }
        if (attraction != null) {
            Map<String, Object> facts = attraction.keyFacts();
            String openTime = textFact(facts, "openTime");
            String ticketInfo = textFact(facts, "ticketInfo");
            if (!isBlank(openTime)) {
                notes.add("开放时间：" + confirmText(openTime) + "。");
            }
            if (!isBlank(ticketInfo)) {
                notes.add("门票信息：" + confirmText(ticketInfo) + "。");
            }
            if (boolFact(facts, "reservationRequired")) {
                notes.add("该景点建议提前预约。");
            }
            if ("HIGH".equalsIgnoreCase(textFact(facts, "holidayRisk"))) {
                notes.add("节假日人流风险较高，建议错峰并预留排队时间。");
            }
            if (boolFact(facts, "needSecondConfirm") || boolFact(facts, "fallback") || !isBlank(textFact(facts, "failureReason"))) {
                notes.add("该景点信息存在不确定性，开放、预约和门票以官方平台为准。");
            }
        } else {
            notes.add("Day " + day + " 的具体开放时间和门票以官方平台为准。");
        }
        return notes;
    }

    /**
     * 构造交通建议。
     *
     * @param intent 旅行意图
     * @param route 完整路线
     * @return 交通建议列表
     */
    private List<String> buildTransportSuggestions(TravelIntent intent, List<String> route, List<TravelEvidence> evidences) {
        String preference = isBlank(intent.transportPreference()) ? "高铁/飞机按总耗时和价格择优" : intent.transportPreference();
        List<String> suggestions = new ArrayList<>();
        suggestions.add("路线顺序：" + String.join(" -> ", route));
        suggestions.add("交通偏好：" + preference + "。");
        for (TravelEvidence routeEvidence : routeEvidences(evidences)) {
            Map<String, Object> facts = routeEvidence.keyFacts();
            String transferSuggestion = textFact(facts, "transferSuggestion");
            if (!isBlank(transferSuggestion)) {
                suggestions.add("路线建议：" + transferSuggestion + "。");
            }
            if (boolFact(facts, "needSecondConfirm") || boolFact(facts, "fallback") || "HIGH".equalsIgnoreCase(textFact(facts, "routeRisk"))) {
                suggestions.add("该路线需二次确认交通时间、班次和换乘安排。");
            }
        }
        suggestions.add("跨城车次、航班余票和价格需在购票平台二次确认。");
        return suggestions.stream().distinct().toList();
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
    private List<String> buildRisks(List<TravelEvidence> evidences) {
        List<String> risks = new ArrayList<>();
        risks.add("车票/酒店价格需二次确认。");
        risks.add("景点预约需二次确认。");
        for (TravelEvidence evidence : safeEvidences(evidences)) {
            Map<String, Object> facts = evidence.keyFacts();
            if (evidence.evidenceType() == EvidenceType.ATTRACTION && boolFact(facts, "reservationRequired")) {
                risks.add("景点“" + attractionName(evidence, "热门景点") + "”建议提前预约。");
            }
            if ("HIGH".equalsIgnoreCase(textFact(facts, "holidayRisk"))) {
                risks.add("节假日人流风险较高，热门景区建议提前预约并预留排队时间。");
            }
            if (evidence.evidenceType() == EvidenceType.ROUTE && "HIGH".equalsIgnoreCase(textFact(facts, "routeRisk"))) {
                risks.add("路线风险较高，需二次确认交通方案。");
            }
            if (boolFact(facts, "needSecondConfirm") || boolFact(facts, "fallback") || !isBlank(textFact(facts, "failureReason"))) {
                risks.add(evidence.title() + " 信息需二次确认。");
            }
        }
        return risks.stream().distinct().toList();
    }

    /**
     * 构造待办事项。
     *
     * @return 待办事项列表
     */
    private List<String> buildTodoList(List<TravelEvidence> evidences) {
        List<String> todos = new ArrayList<>();
        todos.add("确认往返和跨城车票/机票。");
        todos.add("确认酒店价格、位置和取消政策。");
        todos.add("确认热门景点预约、开放时间和入园规则。");
        for (TravelEvidence evidence : safeEvidences(evidences)) {
            Map<String, Object> facts = evidence.keyFacts();
            if (evidence.evidenceType() == EvidenceType.ATTRACTION && boolFact(facts, "reservationRequired")) {
                todos.add("提前预约“" + attractionName(evidence, "热门景点") + "”。");
            }
            if (evidence.evidenceType() == EvidenceType.ROUTE
                    && ("HIGH".equalsIgnoreCase(textFact(facts, "routeRisk")) || boolFact(facts, "needSecondConfirm"))) {
                todos.add("二次确认路线/交通时间、班次和换乘。");
            }
            if (textFact(facts, "ticketInfo").contains("需二次确认")) {
                todos.add("完成票务/门票确认。");
            }
        }
        todos.add("出发前检查证件、充电器、雨具和常用药。");
        return todos.stream().distinct().toList();
    }

    /**
     * 查找指定城市的景点证据。
     *
     * @param city 城市
     * @param evidences 旅行证据列表
     * @return 景点证据，不存在时返回 null
     */
    private TravelEvidence firstAttractionEvidence(String city, List<TravelEvidence> evidences) {
        return safeEvidences(evidences).stream()
                .filter(evidence -> evidence.evidenceType() == EvidenceType.ATTRACTION)
                .filter(evidence -> city.equals(textFact(evidence.keyFacts(), "city")) || city.equals(evidence.city()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取路线证据列表。
     *
     * @param evidences 旅行证据列表
     * @return 路线证据列表
     */
    private List<TravelEvidence> routeEvidences(List<TravelEvidence> evidences) {
        return safeEvidences(evidences).stream()
                .filter(evidence -> evidence.evidenceType() == EvidenceType.ROUTE)
                .toList();
    }

    /**
     * 获取景点名称。
     *
     * @param evidence 景点证据
     * @param fallback 兜底名称
     * @return 景点名称
     */
    private String attractionName(TravelEvidence evidence, String fallback) {
        if (evidence == null) {
            return fallback;
        }
        String name = textFact(evidence.keyFacts(), "attractionName");
        return isBlank(name) ? fallback : name;
    }

    /**
     * 将不确定文本转换为保守表达。
     *
     * @param value 原始文本
     * @return 保守表达文本
     */
    private String confirmText(String value) {
        return value.contains("需二次确认") ? "需二次确认，以官方平台为准" : value;
    }

    /**
     * 获取安全证据列表。
     *
     * @param evidences 原始证据列表
     * @return 安全证据列表
     */
    private List<TravelEvidence> safeEvidences(List<TravelEvidence> evidences) {
        return evidences == null ? List.of() : evidences;
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
}
