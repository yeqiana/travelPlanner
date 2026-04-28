package com.yeqian.travelagent.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * 旅行任务持久化实体。
 *
 * <p>用于承载 travel_task 表中的查询任务记录，并保留工具执行状态。</p>
 */
public class TravelTaskEntity {

    private String planId;

    private Integer taskIndex;

    private String taskType;

    private String queryText;

    private String city;

    private Integer priority;

    private String status;

    private String sourceName;

    private String errorMessage;

    private String taskJson;

    private String toolResultJson;

    private OffsetDateTime createdAt;

    /**
     * 获取计划编号。
     *
     * @return 计划编号
     */
    public String getPlanId() {
        return planId;
    }

    /**
     * 设置计划编号。
     *
     * @param planId 计划编号
     */
    public void setPlanId(String planId) {
        this.planId = planId;
    }

    /**
     * 获取任务序号。
     *
     * @return 任务序号
     */
    public Integer getTaskIndex() {
        return taskIndex;
    }

    /**
     * 设置任务序号。
     *
     * @param taskIndex 任务序号
     */
    public void setTaskIndex(Integer taskIndex) {
        this.taskIndex = taskIndex;
    }

    /**
     * 获取任务类型。
     *
     * @return 任务类型
     */
    public String getTaskType() {
        return taskType;
    }

    /**
     * 设置任务类型。
     *
     * @param taskType 任务类型
     */
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    /**
     * 获取查询文本。
     *
     * @return 查询文本
     */
    public String getQueryText() {
        return queryText;
    }

    /**
     * 设置查询文本。
     *
     * @param queryText 查询文本
     */
    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    /**
     * 获取城市。
     *
     * @return 城市
     */
    public String getCity() {
        return city;
    }

    /**
     * 设置城市。
     *
     * @param city 城市
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * 获取优先级。
     *
     * @return 优先级
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * 设置优先级。
     *
     * @param priority 优先级
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * 获取任务状态。
     *
     * @return 任务状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置任务状态。
     *
     * @param status 任务状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取工具来源名称。
     *
     * @return 工具来源名称
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * 设置工具来源名称。
     *
     * @param sourceName 工具来源名称
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * 获取错误信息。
     *
     * @return 错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置错误信息。
     *
     * @param errorMessage 错误信息
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 获取任务 JSON。
     *
     * @return 任务 JSON
     */
    public String getTaskJson() {
        return taskJson;
    }

    /**
     * 设置任务 JSON。
     *
     * @param taskJson 任务 JSON
     */
    public void setTaskJson(String taskJson) {
        this.taskJson = taskJson;
    }

    /**
     * 获取工具结果 JSON。
     *
     * @return 工具结果 JSON
     */
    public String getToolResultJson() {
        return toolResultJson;
    }

    /**
     * 设置工具结果 JSON。
     *
     * @param toolResultJson 工具结果 JSON
     */
    public void setToolResultJson(String toolResultJson) {
        this.toolResultJson = toolResultJson;
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
}
