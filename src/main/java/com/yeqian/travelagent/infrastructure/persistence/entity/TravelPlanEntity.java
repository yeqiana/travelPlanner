package com.yeqian.travelagent.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * 旅行计划持久化实体。
 *
 * <p>用于承载 travel_plan 表中的主记录，保存计划编号和完整响应 JSON。</p>
 */
public class TravelPlanEntity {

    private String planId;

    private String responseJson;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    /**
     * 获取计划编号。
     *
     * @return 全局唯一计划编号
     */
    public String getPlanId() {
        return planId;
    }

    /**
     * 设置计划编号。
     *
     * @param planId 全局唯一计划编号
     */
    public void setPlanId(String planId) {
        this.planId = planId;
    }

    /**
     * 获取完整响应 JSON。
     *
     * @return 旅行计划响应 JSON
     */
    public String getResponseJson() {
        return responseJson;
    }

    /**
     * 设置完整响应 JSON。
     *
     * @param responseJson 旅行计划响应 JSON
     */
    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
