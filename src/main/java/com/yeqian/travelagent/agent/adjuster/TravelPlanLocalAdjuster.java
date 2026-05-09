package com.yeqian.travelagent.agent.adjuster;

import com.yeqian.travelagent.domain.enums.FatigueLevel;
import com.yeqian.travelagent.domain.enums.TravelDialogIntent;
import com.yeqian.travelagent.domain.model.DailyPlan;
import com.yeqian.travelagent.domain.model.PreviousTravelPlanContext;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelSessionContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 旅行计划局部调整器。
 *
 * <p>基于上一轮完整计划上下文，优先处理按天、餐饮、酒店和交通维度的局部调整，避免简单需求触发整份计划重写。</p>
 */
@Component
public class TravelPlanLocalAdjuster {

    /**
     * 根据多轮意图和用户原文调整推荐计划。
     *
     * @param dialogIntent 本轮多轮对话意图
     * @param message 用户原始输入
     * @param sessionContext 会话上下文
     * @param generatedPlan 本轮新生成的兜底计划
     * @return 调整后的推荐计划
     */
    public TravelPlan adjust(
            TravelDialogIntent dialogIntent,
            String message,
            TravelSessionContext sessionContext,
            TravelPlan generatedPlan
    ) {
        TravelPlan previousPlan = previousPlan(sessionContext);
        if (previousPlan == null || dialogIntent == TravelDialogIntent.REGENERATE_PLAN) {
            return generatedPlan;
        }
        if (dialogIntent == TravelDialogIntent.DETAIL_PLAN) {
            return detailPlan(previousPlan);
        }
        if (dialogIntent != TravelDialogIntent.ADJUST_PLAN && dialogIntent != TravelDialogIntent.ADD_CONSTRAINT) {
            return generatedPlan;
        }
        String normalizedMessage = normalize(message);
        if (isDiningAdjustment(normalizedMessage)) {
            return adjustDining(previousPlan);
        }
        if (isHotelAdjustment(normalizedMessage)) {
            return adjustHotel(previousPlan);
        }
        if (isTransportAdjustment(normalizedMessage)) {
            return adjustTransport(previousPlan, normalizedMessage);
        }
        Integer targetDay = targetDay(normalizedMessage);
        if (targetDay != null) {
            return adjustDay(previousPlan, targetDay, normalizedMessage);
        }
        return appendTodo(previousPlan, "已记录新的局部调整诉求，建议二次确认后再替换对应行程项。");
    }

    /**
     * 细化上一轮完整旅行计划。
     *
     * @param plan 上一轮计划
     * @return 细化后的计划
     */
    private TravelPlan detailPlan(TravelPlan plan) {
        List<DailyPlan> detailedPlans = plan.dailyPlans().stream()
                .map(this::detailDailyPlan)
                .toList();
        return copyPlan(
                plan,
                detailedPlans,
                plan.transportSuggestions(),
                plan.hotelSuggestions(),
                append(plan.todoList(), "已将上一轮行程细化为可执行时间段，具体交通、门票和营业时间仍需二次确认。")
        );
    }

    /**
     * 细化单日行程。
     *
     * @param dailyPlan 上一轮每日计划
     * @return 细化后的每日计划
     */
    private DailyPlan detailDailyPlan(DailyPlan dailyPlan) {
        List<String> notes = append(dailyPlan.notes(), "本日已补充时间段、交通方式、预计耗时和二次确认提示。");
        return new DailyPlan(
                dailyPlan.day(),
                dailyPlan.city(),
                detailSegment("上午", "09:00-11:30", dailyPlan.city(), dailyPlan.morning()),
                detailSegment("下午", "14:00-17:00", dailyPlan.city(), dailyPlan.afternoon()),
                detailSegment("晚上", "18:30-20:30", dailyPlan.city(), dailyPlan.evening()),
                dailyPlan.fatigueLevel(),
                notes
        );
    }

    /**
     * 细化时间段描述。
     *
     * @param label 时间段
     * @param timeRange 默认时间范围
     * @param city 城市
     * @param original 原始安排
     * @return 细化后的时间段安排
     */
    private String detailSegment(String label, String timeRange, String city, String original) {
        String target = hasText(original) ? original : city + label + "候选活动";
        if (target.contains("预计") && target.contains("二次确认") && target.matches(".*\\d{1,2}:\\d{2}.*")) {
            return target;
        }
        return timeRange + " " + target + "；建议地铁/打车或步行组合前往，预计" + duration(label)
                + "；门票、营业时间、排队和路况需二次确认。";
    }

    /**
     * 获取上一轮完整推荐计划。
     *
     * @param sessionContext 会话上下文
     * @return 上一轮推荐计划，不存在时返回 null
     */
    private TravelPlan previousPlan(TravelSessionContext sessionContext) {
        if (sessionContext == null) {
            return null;
        }
        PreviousTravelPlanContext previousPlanContext = sessionContext.previousPlanContext();
        return previousPlanContext == null ? null : previousPlanContext.recommendedPlan();
    }

    /**
     * 调整指定天的行程节奏。
     *
     * @param plan 上一轮计划
     * @param targetDay 目标天数
     * @param message 用户输入
     * @return 调整后的计划
     */
    private TravelPlan adjustDay(TravelPlan plan, int targetDay, String message) {
        List<DailyPlan> dailyPlans = new ArrayList<>();
        for (DailyPlan dailyPlan : plan.dailyPlans()) {
            if (dailyPlan.day() == targetDay) {
                dailyPlans.add(relaxDay(dailyPlan, message));
            } else {
                dailyPlans.add(dailyPlan);
            }
        }
        return copyPlan(
                plan,
                dailyPlans,
                plan.transportSuggestions(),
                plan.hotelSuggestions(),
                append(plan.todoList(), "已优先局部调整第" + targetDay + "天，其他天行程保持上一轮计划。")
        );
    }

    /**
     * 降低单日行程强度。
     *
     * @param dailyPlan 原始每日计划
     * @param message 用户输入
     * @return 调整后的每日计划
     */
    private DailyPlan relaxDay(DailyPlan dailyPlan, String message) {
        String reason = containsAny(message, "轻松", "少走", "不累", "慢一点", "休息")
                ? "降低强度，减少排队和跨区域移动"
                : "按本轮要求局部调整";
        List<String> notes = append(dailyPlan.notes(), "本日已" + reason + "；具体门票、营业时间和路况需二次确认。");
        return new DailyPlan(
                dailyPlan.day(),
                dailyPlan.city(),
                "09:30-11:00 " + dailyPlan.city() + "低强度核心地点游览，优先选择地铁/打车直达，预计1.5小时",
                "14:00-16:00 安排一处室内或近距离候选点，减少连续步行，预计2小时，需二次确认开放状态",
                "18:00-20:00 就近用餐和休息，不再安排远距离夜游，预计2小时",
                FatigueLevel.LOW,
                notes
        );
    }

    /**
     * 调整餐饮相关安排。
     *
     * @param plan 上一轮计划
     * @return 调整后的计划
     */
    private TravelPlan adjustDining(TravelPlan plan) {
        List<DailyPlan> dailyPlans = plan.dailyPlans().stream()
                .map(dailyPlan -> new DailyPlan(
                        dailyPlan.day(),
                        dailyPlan.city(),
                        dailyPlan.morning(),
                        dailyPlan.afternoon(),
                        "18:00-20:00 换为" + dailyPlan.city() + "本地口碑餐厅或小吃街候选，优先选择离住宿/当日最后景点近的位置，需二次确认营业时间和排队情况",
                        dailyPlan.fatigueLevel(),
                        append(dailyPlan.notes(), "餐饮已按本轮要求局部替换，未改动上午和下午主行程。")
                ))
                .toList();
        return copyPlan(plan, dailyPlans, plan.transportSuggestions(), plan.hotelSuggestions(),
                append(plan.todoList(), "餐厅候选需要出发前二次确认营业时间、预约规则和排队情况。"));
    }

    /**
     * 调整住宿相关建议。
     *
     * @param plan 上一轮计划
     * @return 调整后的计划
     */
    private TravelPlan adjustHotel(TravelPlan plan) {
        List<String> hotelSuggestions = append(
                plan.hotelSuggestions(),
                "住宿局部调整：优先选择靠近当日高频活动区域或轨道交通站点的商圈，保留原行程路线，具体酒店价格和房态需二次确认。"
        );
        return copyPlan(plan, plan.dailyPlans(), plan.transportSuggestions(), hotelSuggestions,
                append(plan.todoList(), "重新核对酒店商圈、晚到交通和退改规则。"));
    }

    /**
     * 调整交通相关建议。
     *
     * @param plan 上一轮计划
     * @param message 用户输入
     * @return 调整后的计划
     */
    private TravelPlan adjustTransport(TravelPlan plan, String message) {
        String suggestion = containsAny(message, "少", "减少", "别太折腾", "不折腾")
                ? "交通局部调整：减少跨城和反复换乘，优先保留同城连续游览，必要跨城班次需二次确认。"
                : "交通局部调整：按本轮要求替换交通偏好，班次、票价和时长需二次确认。";
        return copyPlan(plan, plan.dailyPlans(), append(plan.transportSuggestions(), suggestion), plan.hotelSuggestions(),
                append(plan.todoList(), "重新确认大交通班次、城市内通勤时长和节假日拥堵风险。"));
    }

    /**
     * 追加待办事项。
     *
     * @param plan 上一轮计划
     * @param todo 待办事项
     * @return 调整后的计划
     */
    private TravelPlan appendTodo(TravelPlan plan, String todo) {
        return copyPlan(plan, plan.dailyPlans(), plan.transportSuggestions(), plan.hotelSuggestions(), append(plan.todoList(), todo));
    }

    /**
     * 复制旅行计划并替换局部字段。
     *
     * @param plan 原始计划
     * @param dailyPlans 每日计划列表
     * @param transportSuggestions 交通建议列表
     * @param hotelSuggestions 酒店建议列表
     * @param todoList 待办事项列表
     * @return 新旅行计划
     */
    private TravelPlan copyPlan(
            TravelPlan plan,
            List<DailyPlan> dailyPlans,
            List<String> transportSuggestions,
            List<String> hotelSuggestions,
            List<String> todoList
    ) {
        return new TravelPlan(
                plan.title(),
                plan.summary(),
                plan.route(),
                dailyPlans,
                transportSuggestions,
                hotelSuggestions,
                Map.copyOf(plan.budgetEstimate()),
                plan.risks(),
                todoList
        );
    }

    /**
     * 判断是否为餐饮调整。
     *
     * @param message 用户输入
     * @return 命中餐饮调整时返回 true
     */
    private boolean isDiningAdjustment(String message) {
        return containsAny(message, "餐厅", "吃", "美食", "餐饮", "小吃", "晚餐", "午餐");
    }

    /**
     * 判断是否为酒店调整。
     *
     * @param message 用户输入
     * @return 命中酒店调整时返回 true
     */
    private boolean isHotelAdjustment(String message) {
        return containsAny(message, "酒店", "住宿", "住哪里", "住哪", "民宿", "商圈");
    }

    /**
     * 判断是否为交通调整。
     *
     * @param message 用户输入
     * @return 命中交通调整时返回 true
     */
    private boolean isTransportAdjustment(String message) {
        return containsAny(message, "交通", "高铁", "飞机", "自驾", "打车", "地铁", "跨城", "换乘");
    }

    /**
     * 解析用户指定的目标天数。
     *
     * @param message 用户输入
     * @return 目标天数，未命中时返回 null
     */
    private Integer targetDay(String message) {
        for (int day = 1; day <= 9; day++) {
            if (message.contains("第" + day + "天") || message.contains("第" + day + "日")) {
                return day;
            }
        }
        String[] chineseNumbers = {"一", "二", "三", "四", "五", "六", "七", "八", "九"};
        for (int index = 0; index < chineseNumbers.length; index++) {
            String value = chineseNumbers[index];
            if (message.contains("第" + value + "天") || message.contains("第" + value + "日")) {
                return index + 1;
            }
        }
        return null;
    }

    /**
     * 追加文本到列表末尾。
     *
     * @param values 原列表
     * @param value 追加文本
     * @return 新列表
     */
    private List<String> append(List<String> values, String value) {
        List<String> result = new ArrayList<>(values == null ? List.of() : values);
        result.add(value);
        return result;
    }

    /**
     * 判断文本是否包含有效内容。
     *
     * @param value 待判断文本
     * @return 包含有效内容时返回 true
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 获取时间段预计耗时。
     *
     * @param label 时间段
     * @return 预计耗时文本
     */
    private String duration(String label) {
        return "晚上".equals(label) ? "2小时" : "2.5小时";
    }

    /**
     * 判断文本是否包含任意关键词。
     *
     * @param message 文本
     * @param keywords 关键词数组
     * @return 命中任意关键词时返回 true
     */
    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 规整用户输入。
     *
     * @param message 用户输入
     * @return 规整后的文本
     */
    private String normalize(String message) {
        return message == null ? "" : message.trim();
    }
}
