package com.yeqian.travelagent.domain.model;

import com.yeqian.travelagent.domain.enums.TravelTaskType;

/**
 * 旅行查询任务。
 *
 * <p>描述一次待查询的信息任务，包含任务类型、查询语句、关联城市和优先级。</p>
 *
 * @param taskType 任务类型
 * @param query 查询语句
 * @param city 关联城市，无法确定时为空
 * @param priority 优先级，数值越小越优先
 */
public record TravelTask(
        TravelTaskType taskType,
        String query,
        String city,
        int priority
) {
}
