package com.yeqian.travelagent.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelReminder;
import com.yeqian.travelagent.domain.model.TravelTask;
import com.yeqian.travelagent.infrastructure.persistence.entity.TravelEvidenceEntity;
import com.yeqian.travelagent.infrastructure.persistence.entity.TravelPlanEntity;
import com.yeqian.travelagent.infrastructure.persistence.entity.TravelReminderEntity;
import com.yeqian.travelagent.infrastructure.persistence.entity.TravelTaskEntity;
import com.yeqian.travelagent.infrastructure.persistence.mapper.TravelEvidenceMapper;
import com.yeqian.travelagent.infrastructure.persistence.mapper.TravelPlanMapper;
import com.yeqian.travelagent.infrastructure.persistence.mapper.TravelReminderMapper;
import com.yeqian.travelagent.infrastructure.persistence.mapper.TravelTaskMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * 旅行计划持久化服务。
 *
 * <p>负责保存完整旅行计划结果，并按 planId 读取历史计划。</p>
 */
@Service
public class TravelPlanPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TravelPlanPersistenceService.class);

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private TravelPlanMapper travelPlanMapper;

    @Resource
    private TravelTaskMapper travelTaskMapper;

    @Resource
    private TravelEvidenceMapper travelEvidenceMapper;

    @Resource
    private TravelReminderMapper travelReminderMapper;

    /**
     * 保存生成成功的完整旅行计划结果。
     *
     * @param response 旅行计划响应
     * @return 已写入全局唯一 planId 的旅行计划响应
     */
    @Transactional
    public TravelPlanResponse saveCompletedPlan(TravelPlanResponse response) {
        String planId = UUID.randomUUID().toString();
        TravelPlanResponse savedResponse = response.withPlanId(planId);
        OffsetDateTime now = OffsetDateTime.now();
        log.info("旅行计划持久化开始：planId={}, tasks={}, evidences={}, reminders={}",
                planId,
                savedResponse.tasks().size(),
                savedResponse.evidences().size(),
                savedResponse.reminders().size());

        TravelPlanEntity planEntity = new TravelPlanEntity();
        planEntity.setPlanId(planId);
        planEntity.setResponseJson(toJson(savedResponse));
        planEntity.setCreatedAt(now);
        planEntity.setUpdatedAt(now);

        travelPlanMapper.insert(planEntity);
        travelTaskMapper.batchInsert(taskEntities(savedResponse, now));
        travelEvidenceMapper.batchInsert(evidenceEntities(savedResponse, now));
        travelReminderMapper.batchInsert(reminderEntities(savedResponse, now));
        log.info("旅行计划持久化完成：planId={}", planId);
        return savedResponse;
    }

    /**
     * 按计划编号查询历史旅行计划。
     *
     * @param planId 计划编号
     * @return 保存时的旅行计划响应
     */
    public TravelPlanResponse findByPlanId(String planId) {
        log.info("查询历史旅行计划：planId={}", planId);
        TravelPlanEntity entity = travelPlanMapper.findByPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("旅行计划不存在：" + planId));
        return fromJson(entity.getResponseJson(), TravelPlanResponse.class);
    }

    /**
     * 构建任务持久化实体列表。
     *
     * @param response 旅行计划响应
     * @param now 当前时间
     * @return 任务持久化实体列表
     */
    private List<TravelTaskEntity> taskEntities(TravelPlanResponse response, OffsetDateTime now) {
        List<TravelTask> tasks = response.tasks();
        List<ToolResult> toolResults = response.toolResults();
        return IntStream.range(0, tasks.size())
                .mapToObj(index -> taskEntity(response.planId(), index, tasks.get(index), toolResultAt(toolResults, index), now))
                .toList();
    }

    /**
     * 构建单条任务持久化实体。
     *
     * @param planId 计划编号
     * @param index 任务序号
     * @param task 旅行任务
     * @param toolResult 工具执行结果
     * @param now 当前时间
     * @return 任务持久化实体
     */
    private TravelTaskEntity taskEntity(String planId, int index, TravelTask task, ToolResult toolResult, OffsetDateTime now) {
        TravelTaskEntity entity = new TravelTaskEntity();
        entity.setPlanId(planId);
        entity.setTaskIndex(index);
        entity.setTaskType(task.taskType().name());
        entity.setQueryText(task.query());
        entity.setCity(task.city());
        entity.setPriority(task.priority());
        entity.setStatus(taskStatus(toolResult));
        entity.setSourceName(toolResult == null ? null : toolResult.source());
        entity.setErrorMessage(toolResult == null ? null : toolResult.errorMessage());
        entity.setTaskJson(toJson(task));
        entity.setToolResultJson(toJson(toolResult));
        entity.setCreatedAt(now);
        return entity;
    }

    /**
     * 构建证据持久化实体列表。
     *
     * @param response 旅行计划响应
     * @param now 当前时间
     * @return 证据持久化实体列表
     */
    private List<TravelEvidenceEntity> evidenceEntities(TravelPlanResponse response, OffsetDateTime now) {
        List<TravelEvidence> evidences = response.evidences();
        return IntStream.range(0, evidences.size())
                .mapToObj(index -> evidenceEntity(response.planId(), index, evidences.get(index), now))
                .toList();
    }

    /**
     * 构建单条证据持久化实体。
     *
     * @param planId 计划编号
     * @param index 证据序号
     * @param evidence 旅行证据
     * @param now 当前时间
     * @return 证据持久化实体
     */
    private TravelEvidenceEntity evidenceEntity(String planId, int index, TravelEvidence evidence, OffsetDateTime now) {
        TravelEvidenceEntity entity = new TravelEvidenceEntity();
        entity.setPlanId(planId);
        entity.setEvidenceIndex(index);
        entity.setEvidenceType(evidence.evidenceType().name());
        entity.setCity(evidence.city());
        entity.setTitle(evidence.title());
        entity.setSummary(evidence.summary());
        entity.setConfidence(evidence.confidence());
        entity.setSourceName(evidence.sourceName());
        entity.setSourceUrl(evidence.sourceUrl());
        entity.setKeyFactsJson(toJson(evidence.keyFacts()));
        entity.setEvidenceJson(toJson(evidence));
        entity.setFetchedAt(evidence.fetchedAt());
        entity.setCreatedAt(now);
        return entity;
    }

    /**
     * 构建提醒持久化实体列表。
     *
     * @param response 旅行计划响应
     * @param now 当前时间
     * @return 提醒持久化实体列表
     */
    private List<TravelReminderEntity> reminderEntities(TravelPlanResponse response, OffsetDateTime now) {
        List<TravelReminder> reminders = response.reminders();
        return IntStream.range(0, reminders.size())
                .mapToObj(index -> reminderEntity(response.planId(), index, reminders.get(index), now))
                .toList();
    }

    /**
     * 构建单条提醒持久化实体。
     *
     * @param planId 计划编号
     * @param index 提醒序号
     * @param reminder 旅行提醒
     * @param now 当前时间
     * @return 提醒持久化实体
     */
    private TravelReminderEntity reminderEntity(String planId, int index, TravelReminder reminder, OffsetDateTime now) {
        TravelReminderEntity entity = new TravelReminderEntity();
        entity.setPlanId(planId);
        entity.setReminderIndex(index);
        entity.setTitle(reminder.title());
        entity.setReminderType(reminder.type().name());
        entity.setRemindRule(reminder.remindRule());
        entity.setDescription(reminder.description());
        entity.setReminderJson(toJson(reminder));
        entity.setCreatedAt(now);
        return entity;
    }

    /**
     * 按下标获取工具结果。
     *
     * @param toolResults 工具结果列表
     * @param index 下标
     * @return 工具结果，不存在时为空
     */
    private ToolResult toolResultAt(List<ToolResult> toolResults, int index) {
        if (toolResults == null || index >= toolResults.size()) {
            return null;
        }
        return toolResults.get(index);
    }

    /**
     * 获取任务执行状态。
     *
     * @param toolResult 工具执行结果
     * @return 任务执行状态
     */
    private String taskStatus(ToolResult toolResult) {
        if (toolResult == null) {
            return "PENDING";
        }
        return toolResult.success() ? "SUCCESS" : "FAILED";
    }

    /**
     * 将对象序列化为 JSON。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("旅行计划序列化失败", exception);
        }
    }

    /**
     * 将 JSON 反序列化为对象。
     *
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @param <T> 目标泛型
     * @return 反序列化后的对象
     */
    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("旅行计划反序列化失败", exception);
        }
    }
}
