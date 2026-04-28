package com.yeqian.travelagent.agent.planner;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 旅行查询任务拆解器。
 *
 * <p>根据已解析的旅行意图，使用规则拆解天气、交通、酒店、景点、路线和通用网页查询任务。</p>
 */
@Component
public class TravelTaskPlanner {

    private static final int MAX_TASK_COUNT = 8;

    /**
     * 根据旅行意图生成查询任务。
     *
     * @param intent 旅行意图
     * @return 查询任务列表，最多返回 8 个任务
     */
    public List<TravelTask> plan(TravelIntent intent) {
        List<String> destinationCities = destinationCities(intent);
        String destinationText = destinationCities.isEmpty() ? "热门旅行目的地" : String.join(" ", destinationCities);
        String primaryCity = destinationCities.isEmpty() ? null : destinationCities.get(0);

        List<TravelTask> tasks = new ArrayList<>();
        if (destinationCities.isEmpty()) {
            addTask(tasks, TravelTaskType.GENERAL_WEB, "适合" + daysText(intent) + "旅行的目的地推荐", null);
        }

        addTask(tasks, TravelTaskType.TRANSPORT, departureText(intent) + "到" + destinationText + "交通方式 高铁 飞机 时间 成本", primaryCity);
        for (String city : destinationCities) {
            addTask(tasks, TravelTaskType.WEATHER, city + " " + dateText(intent) + "天气 穿衣建议", city);
        }
        addTask(tasks, TravelTaskType.HOTEL, destinationText + " " + dateText(intent) + "酒店 区域 价格 交通便利", primaryCity);
        for (String city : destinationCities) {
            addTask(tasks, TravelTaskType.ATTRACTION, city + "热门景点 预约 开放时间 游玩时长", city);
        }
        addTask(tasks, TravelTaskType.ROUTE, departureText(intent) + "到" + destinationText + " " + daysText(intent) + "路线可行性 城市顺序", primaryCity);

        return List.copyOf(tasks);
    }

    /**
     * 提取目的地城市列表。
     *
     * @param intent 旅行意图
     * @return 去重后的目的地城市列表
     */
    private List<String> destinationCities(TravelIntent intent) {
        return intent.destinationPreferences().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    /**
     * 添加查询任务，并控制任务总数。
     *
     * @param tasks 当前任务列表
     * @param type 任务类型
     * @param query 查询语句
     * @param city 关联城市
     */
    private void addTask(List<TravelTask> tasks, TravelTaskType type, String query, String city) {
        if (tasks.size() >= MAX_TASK_COUNT) {
            return;
        }
        tasks.add(new TravelTask(type, query, city, tasks.size() + 1));
    }

    /**
     * 生成出发地查询文本。
     *
     * @param intent 旅行意图
     * @return 出发地文本
     */
    private String departureText(TravelIntent intent) {
        return isBlank(intent.departureCity()) ? "出发地" : intent.departureCity();
    }

    /**
     * 生成日期查询文本。
     *
     * @param intent 旅行意图
     * @return 日期文本
     */
    private String dateText(TravelIntent intent) {
        return isBlank(intent.dateText()) ? "出行日期" : intent.dateText();
    }

    /**
     * 生成天数查询文本。
     *
     * @param intent 旅行意图
     * @return 天数文本
     */
    private String daysText(TravelIntent intent) {
        return intent.days() == null || intent.days() <= 0 ? "多天" : intent.days() + "天";
    }

    /**
     * 判断字符串是否为空。
     *
     * @param value 待判断字符串
     * @return 为空时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
