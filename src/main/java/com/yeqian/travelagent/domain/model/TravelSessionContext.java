package com.yeqian.travelagent.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 旅行规划会话上下文。
 *
 * <p>用于保存多轮追问过程中已经解析出的部分意图和上一轮澄清问题。</p>
 *
 * @param sessionId 会话编号
 * @param partialIntent 已解析出的部分旅行意图
 * @param lastQuestions 上一轮结构化澄清问题
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 * @param status 会话状态
 */
public record TravelSessionContext(
        String sessionId,
        TravelIntent partialIntent,
        List<ClarificationQuestion> lastQuestions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String status
) {
    /**
     * 创建旅行规划会话上下文并规整空集合和时间字段。
     */
    public TravelSessionContext {
        lastQuestions = lastQuestions == null ? List.of() : List.copyOf(lastQuestions);
        createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        status = status == null || status.isBlank() ? "CLARIFYING" : status;
    }

    /**
     * 基于当前上下文创建更新后的澄清态上下文。
     *
     * @param intent 最新合并后的部分旅行意图
     * @param questions 最新结构化澄清问题
     * @return 更新后的会话上下文
     */
    public TravelSessionContext withClarifyingIntent(TravelIntent intent, List<ClarificationQuestion> questions) {
        return new TravelSessionContext(sessionId, intent, questions, createdAt, LocalDateTime.now(), "CLARIFYING");
    }

    /**
     * 基于当前上下文创建已完成态上下文。
     *
     * @param intent 最新合并后的完整旅行意图
     * @return 更新后的会话上下文
     */
    public TravelSessionContext withCompletedIntent(TravelIntent intent) {
        return new TravelSessionContext(sessionId, intent, List.of(), createdAt, LocalDateTime.now(), "COMPLETED");
    }
}
