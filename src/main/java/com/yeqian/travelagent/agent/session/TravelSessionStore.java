package com.yeqian.travelagent.agent.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import com.yeqian.travelagent.domain.model.ClarificationQuestion;
import com.yeqian.travelagent.domain.model.PreviousTravelPlanContext;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelSessionContext;
import com.yeqian.travelagent.infrastructure.config.TravelAgentProperties;
import com.yeqian.travelagent.infrastructure.persistence.entity.TravelSessionEntity;
import com.yeqian.travelagent.infrastructure.persistence.mapper.TravelSessionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 旅行规划会话仓库。
 *
 * <p>使用数据库保存多轮追问上下文，支持应用重启后恢复未过期会话。</p>
 */
@Component
public class TravelSessionStore {

    private static final String STATUS_CLARIFYING = "CLARIFYING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final TypeReference<List<ClarificationQuestion>> QUESTION_LIST_TYPE = new TypeReference<>() {
    };

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private TravelSessionMapper travelSessionMapper;

    @Resource
    private TravelAgentProperties travelAgentProperties;

    /**
     * 根据会话编号查询上下文。
     *
     * @param sessionId 会话编号
     * @return 会话上下文，不存在时返回 null
     */
    public TravelSessionContext findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return travelSessionMapper.findBySessionId(sessionId)
                .filter(this::isRecoverable)
                .map(this::toContext)
                .orElse(null);
    }

    /**
     * 创建或更新澄清态会话上下文。
     *
     * @param sessionId 请求携带的会话编号，可为空
     * @param intent 当前已解析出的部分旅行意图
     * @param questions 当前结构化澄清问题
     * @return 保存后的会话上下文
     */
    public TravelSessionContext saveClarifying(String sessionId, TravelIntent intent, List<ClarificationQuestion> questions) {
        TravelSessionContext existingContext = findBySessionId(sessionId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(sessionTtlHours());
        if (existingContext == null) {
            String normalizedSessionId = newClarifyingSessionId(sessionId);
            TravelSessionContext createdContext = new TravelSessionContext(
                    normalizedSessionId,
                    intent,
                    questions,
                    null,
                    now,
                    now,
                    STATUS_CLARIFYING
            );
            travelSessionMapper.insert(toEntity(createdContext, expiresAt));
            return createdContext;
        }

        TravelSessionContext updatedContext = existingContext.withClarifyingIntent(intent, questions);
        travelSessionMapper.updateClarifying(toEntity(updatedContext, expiresAt));
        return updatedContext;
    }

    /**
     * 生成新追问会话编号。
     *
     * @param requestedSessionId 请求携带的会话编号
     * @return 可安全插入的新会话编号
     */
    private String newClarifyingSessionId(String requestedSessionId) {
        if (!hasText(requestedSessionId)) {
            return newSessionId();
        }
        return travelSessionMapper.findBySessionId(requestedSessionId).isPresent() ? newSessionId() : requestedSessionId;
    }

    /**
     * 将会话标记为已完成。
     *
     * @param sessionId 会话编号
     * @param intent 完整旅行意图
     */
    public void markCompleted(String sessionId, TravelIntent intent) {
        if (!hasText(sessionId)) {
            return;
        }
        TravelSessionContext existingContext = findBySessionId(sessionId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(sessionTtlHours());
        if (existingContext != null) {
            travelSessionMapper.markCompleted(existingContext.sessionId(), toJson(intent), null, expiresAt, now);
            return;
        }
        TravelSessionContext createdContext = new TravelSessionContext(
                sessionId,
                intent,
                List.of(),
                null,
                now,
                now,
                STATUS_COMPLETED
        );
        travelSessionMapper.insert(toEntity(createdContext, expiresAt));
    }

    /**
     * 将会话标记为已完成，并保存上一轮完整计划上下文。
     *
     * @param sessionId 会话编号
     * @param response 已完成并落库后的旅行规划响应
     */
    public void markCompleted(String sessionId, TravelPlanResponse response) {
        if (!hasText(sessionId) || response == null) {
            return;
        }
        PreviousTravelPlanContext previousPlanContext = new PreviousTravelPlanContext(
                response.planId(),
                response.intent(),
                response.recommendedPlan(),
                response.reminders(),
                response.risks()
        );
        TravelIntent intent = response.intent();
        TravelSessionContext existingContext = findBySessionId(sessionId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(sessionTtlHours());
        if (existingContext != null) {
            travelSessionMapper.markCompleted(existingContext.sessionId(), toJson(intent), toJson(previousPlanContext), expiresAt, now);
            return;
        }
        TravelSessionContext createdContext = new TravelSessionContext(
                sessionId,
                intent,
                List.of(),
                previousPlanContext,
                now,
                now,
                STATUS_COMPLETED
        );
        travelSessionMapper.insert(toEntity(createdContext, expiresAt));
    }

    /**
     * 判断会话是否可恢复。
     *
     * @param entity 会话持久化实体
     * @return 未过期且处于追问状态时返回 true
     */
    private boolean isRecoverable(TravelSessionEntity entity) {
        return (STATUS_CLARIFYING.equals(entity.getStatus()) || STATUS_COMPLETED.equals(entity.getStatus()))
                && entity.getExpiresAt() != null
                && entity.getExpiresAt().isAfter(LocalDateTime.now());
    }

    /**
     * 将持久化实体转换为会话上下文。
     *
     * @param entity 会话持久化实体
     * @return 会话上下文
     */
    private TravelSessionContext toContext(TravelSessionEntity entity) {
        return new TravelSessionContext(
                entity.getSessionId(),
                fromJson(entity.getPartialIntentJson(), TravelIntent.class),
                fromQuestionJson(entity.getLastQuestionsJson()),
                fromJson(entity.getLastResponseJson(), PreviousTravelPlanContext.class),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getStatus()
        );
    }

    /**
     * 将会话上下文转换为持久化实体。
     *
     * @param context 会话上下文
     * @param expiresAt 会话过期时间
     * @return 会话持久化实体
     */
    private TravelSessionEntity toEntity(TravelSessionContext context, LocalDateTime expiresAt) {
        TravelSessionEntity entity = new TravelSessionEntity();
        entity.setSessionId(context.sessionId());
        entity.setStatus(context.status());
        entity.setPartialIntentJson(toJson(context.partialIntent()));
        entity.setLastQuestionsJson(toJson(context.lastQuestions()));
        entity.setLastResponseJson(toJson(context.previousPlanContext()));
        entity.setExpiresAt(expiresAt);
        entity.setCreatedAt(context.createdAt());
        entity.setUpdatedAt(context.updatedAt());
        return entity;
    }

    /**
     * 获取会话过期小时数。
     *
     * @return 会话过期小时数
     */
    private int sessionTtlHours() {
        if (travelAgentProperties == null || travelAgentProperties.getPlanning() == null) {
            return 24;
        }
        return Math.max(1, travelAgentProperties.getPlanning().getSessionTtlHours());
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
            throw new IllegalStateException("旅行会话序列化失败", exception);
        }
    }

    /**
     * 将 JSON 反序列化为目标对象。
     *
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @param <T> 目标泛型
     * @return 目标对象
     */
    private <T> T fromJson(String json, Class<T> clazz) {
        if (!hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("旅行会话反序列化失败", exception);
        }
    }

    /**
     * 将问题 JSON 反序列化为结构化追问列表。
     *
     * @param json 问题 JSON
     * @return 结构化追问列表
     */
    private List<ClarificationQuestion> fromQuestionJson(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, QUESTION_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("旅行会话追问问题反序列化失败", exception);
        }
    }

    /**
     * 生成新的会话编号。
     *
     * @return 会话编号
     */
    private String newSessionId() {
        return "session_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 判断文本是否包含有效内容。
     *
     * @param value 待判断文本
     * @return 包含有效内容时返回 true
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
