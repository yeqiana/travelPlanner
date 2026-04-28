package com.yeqian.travelagent.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * 旅行提醒持久化实体。
 *
 * <p>用于承载 travel_reminder 表中的提醒事项。</p>
 */
public class TravelReminderEntity {

    private String planId;

    private Integer reminderIndex;

    private String title;

    private String reminderType;

    private String remindRule;

    private String description;

    private String reminderJson;

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
     * 获取提醒序号。
     *
     * @return 提醒序号
     */
    public Integer getReminderIndex() {
        return reminderIndex;
    }

    /**
     * 设置提醒序号。
     *
     * @param reminderIndex 提醒序号
     */
    public void setReminderIndex(Integer reminderIndex) {
        this.reminderIndex = reminderIndex;
    }

    /**
     * 获取提醒标题。
     *
     * @return 提醒标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置提醒标题。
     *
     * @param title 提醒标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取提醒类型。
     *
     * @return 提醒类型
     */
    public String getReminderType() {
        return reminderType;
    }

    /**
     * 设置提醒类型。
     *
     * @param reminderType 提醒类型
     */
    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    /**
     * 获取提醒规则。
     *
     * @return 提醒规则
     */
    public String getRemindRule() {
        return remindRule;
    }

    /**
     * 设置提醒规则。
     *
     * @param remindRule 提醒规则
     */
    public void setRemindRule(String remindRule) {
        this.remindRule = remindRule;
    }

    /**
     * 获取提醒说明。
     *
     * @return 提醒说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置提醒说明。
     *
     * @param description 提醒说明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取提醒 JSON。
     *
     * @return 提醒 JSON
     */
    public String getReminderJson() {
        return reminderJson;
    }

    /**
     * 设置提醒 JSON。
     *
     * @param reminderJson 提醒 JSON
     */
    public void setReminderJson(String reminderJson) {
        this.reminderJson = reminderJson;
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
