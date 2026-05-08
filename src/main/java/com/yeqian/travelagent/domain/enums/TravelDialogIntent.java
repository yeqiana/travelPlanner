package com.yeqian.travelagent.domain.enums;

/**
 * 旅行对话意图枚举。
 *
 * <p>用于标识用户在多轮旅行规划中的操作意图，帮助系统区分细化、重写、调整和补充信息。</p>
 */
public enum TravelDialogIntent {
    /**
     * 首次创建旅行计划。
     */
    CREATE_PLAN,

    /**
     * 细化当前旅行计划。
     */
    DETAIL_PLAN,

    /**
     * 重新生成旅行计划。
     */
    REGENERATE_PLAN,

    /**
     * 调整已有旅行计划。
     */
    ADJUST_PLAN,

    /**
     * 补充新的约束条件。
     */
    ADD_CONSTRAINT,

    /**
     * 回答系统追问信息。
     */
    ANSWER_CLARIFICATION
}
