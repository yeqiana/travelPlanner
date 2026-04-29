package com.yeqian.travelagent.infrastructure.persistence.entity;

import java.time.LocalDateTime;

/**
 * 旅行会话持久化实体。
 *
 * <p>用于承载 travel_session 表中的多轮追问上下文和过期时间。</p>
 */
public class TravelSessionEntity {

    private String sessionId;

    private String status;

    private String partialIntentJson;

    private String lastQuestionsJson;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 获取会话编号。
     *
     * @return 会话编号
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话编号。
     *
     * @param sessionId 会话编号
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取会话状态。
     *
     * @return 会话状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置会话状态。
     *
     * @param status 会话状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取部分意图 JSON。
     *
     * @return 部分意图 JSON
     */
    public String getPartialIntentJson() {
        return partialIntentJson;
    }

    /**
     * 设置部分意图 JSON。
     *
     * @param partialIntentJson 部分意图 JSON
     */
    public void setPartialIntentJson(String partialIntentJson) {
        this.partialIntentJson = partialIntentJson;
    }

    /**
     * 获取上一轮追问问题 JSON。
     *
     * @return 上一轮追问问题 JSON
     */
    public String getLastQuestionsJson() {
        return lastQuestionsJson;
    }

    /**
     * 设置上一轮追问问题 JSON。
     *
     * @param lastQuestionsJson 上一轮追问问题 JSON
     */
    public void setLastQuestionsJson(String lastQuestionsJson) {
        this.lastQuestionsJson = lastQuestionsJson;
    }

    /**
     * 获取会话过期时间。
     *
     * @return 会话过期时间
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * 设置会话过期时间。
     *
     * @param expiresAt 会话过期时间
     */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
