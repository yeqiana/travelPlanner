package com.yeqian.travelagent.domain.model;

import com.yeqian.travelagent.domain.enums.FatigueLevel;

import java.util.List;

/**
 * 每日行程。
 *
 * <p>描述某一天的城市、上午、下午、晚上安排及注意事项。</p>
 *
 * @param day 第几天
 * @param city 当天所在城市
 * @param morning 上午安排
 * @param afternoon 下午安排
 * @param evening 晚上安排
 * @param fatigueLevel 疲劳等级
 * @param notes 注意事项
 */
public record DailyPlan(
        int day,
        String city,
        String morning,
        String afternoon,
        String evening,
        FatigueLevel fatigueLevel,
        List<String> notes
) {
    /**
     * 创建每日行程并规整空集合字段。
     */
    public DailyPlan {
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
