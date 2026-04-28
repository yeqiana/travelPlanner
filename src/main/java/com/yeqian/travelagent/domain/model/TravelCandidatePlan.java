package com.yeqian.travelagent.domain.model;

import java.util.List;

/**
 * 旅行候选方案。
 *
 * <p>表示一条可选路线及其推荐理由，供规则评分选择最优方案。</p>
 *
 * @param name 方案名称
 * @param route 路线城市列表
 * @param reason 推荐理由
 */
public record TravelCandidatePlan(
        String name,
        List<String> route,
        String reason
) {
    /**
     * 创建旅行候选方案并规整空集合字段。
     */
    public TravelCandidatePlan {
        route = route == null ? List.of() : List.copyOf(route);
    }
}
