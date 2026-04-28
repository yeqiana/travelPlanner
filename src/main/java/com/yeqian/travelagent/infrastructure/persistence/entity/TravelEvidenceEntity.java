package com.yeqian.travelagent.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

/**
 * 旅行证据持久化实体。
 *
 * <p>用于承载 travel_evidence 表中的证据记录，保留来源和关键事实 JSON。</p>
 */
public class TravelEvidenceEntity {

    private String planId;

    private Integer evidenceIndex;

    private String evidenceType;

    private String city;

    private String title;

    private String summary;

    private Double confidence;

    private String sourceName;

    private String sourceUrl;

    private String keyFactsJson;

    private String evidenceJson;

    private OffsetDateTime fetchedAt;

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
     * 获取证据序号。
     *
     * @return 证据序号
     */
    public Integer getEvidenceIndex() {
        return evidenceIndex;
    }

    /**
     * 设置证据序号。
     *
     * @param evidenceIndex 证据序号
     */
    public void setEvidenceIndex(Integer evidenceIndex) {
        this.evidenceIndex = evidenceIndex;
    }

    /**
     * 获取证据类型。
     *
     * @return 证据类型
     */
    public String getEvidenceType() {
        return evidenceType;
    }

    /**
     * 设置证据类型。
     *
     * @param evidenceType 证据类型
     */
    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
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
     * 获取标题。
     *
     * @return 标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置标题。
     *
     * @param title 标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取摘要。
     *
     * @return 摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 设置摘要。
     *
     * @param summary 摘要
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取可信度。
     *
     * @return 可信度
     */
    public Double getConfidence() {
        return confidence;
    }

    /**
     * 设置可信度。
     *
     * @param confidence 可信度
     */
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    /**
     * 获取来源名称。
     *
     * @return 来源名称
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * 设置来源名称。
     *
     * @param sourceName 来源名称
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * 获取来源链接。
     *
     * @return 来源链接
     */
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     * 设置来源链接。
     *
     * @param sourceUrl 来源链接
     */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    /**
     * 获取关键事实 JSON。
     *
     * @return 关键事实 JSON
     */
    public String getKeyFactsJson() {
        return keyFactsJson;
    }

    /**
     * 设置关键事实 JSON。
     *
     * @param keyFactsJson 关键事实 JSON
     */
    public void setKeyFactsJson(String keyFactsJson) {
        this.keyFactsJson = keyFactsJson;
    }

    /**
     * 获取证据 JSON。
     *
     * @return 证据 JSON
     */
    public String getEvidenceJson() {
        return evidenceJson;
    }

    /**
     * 设置证据 JSON。
     *
     * @param evidenceJson 证据 JSON
     */
    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    /**
     * 获取抓取时间。
     *
     * @return 抓取时间
     */
    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }

    /**
     * 设置抓取时间。
     *
     * @param fetchedAt 抓取时间
     */
    public void setFetchedAt(OffsetDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
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
