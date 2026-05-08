package com.yeqian.travelagent.agent.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import com.yeqian.travelagent.domain.enums.FatigueLevel;
import com.yeqian.travelagent.domain.model.ClarificationQuestion;
import com.yeqian.travelagent.domain.model.DailyPlan;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelSessionContext;
import com.yeqian.travelagent.infrastructure.config.TravelAgentProperties;
import com.yeqian.travelagent.infrastructure.persistence.entity.TravelSessionEntity;
import com.yeqian.travelagent.infrastructure.persistence.mapper.TravelSessionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 旅行会话仓库测试。
 */
class TravelSessionStoreTest {

    /**
     * 验证新追问会话会写入数据库并带上过期时间。
     */
    @Test
    void shouldCreateClarifyingSessionWithExpiresAt() {
        TravelSessionMapper mapper = mock(TravelSessionMapper.class);
        TravelSessionStore store = store(mapper, 24);
        TravelIntent intent = intent("杭州");
        List<ClarificationQuestion> questions = List.of(question("departureCity"));

        TravelSessionContext context = store.saveClarifying(null, intent, questions);

        ArgumentCaptor<TravelSessionEntity> captor = ArgumentCaptor.forClass(TravelSessionEntity.class);
        verify(mapper).insert(captor.capture());
        TravelSessionEntity entity = captor.getValue();
        assertThat(context.sessionId()).startsWith("session_");
        assertThat(entity.getSessionId()).isEqualTo(context.sessionId());
        assertThat(entity.getStatus()).isEqualTo("CLARIFYING");
        assertThat(entity.getPartialIntentJson()).contains("杭州");
        assertThat(entity.getLastQuestionsJson()).contains("departureCity");
        assertThat(entity.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
    }

    /**
     * 验证应用重启后可以从数据库恢复未过期的追问上下文。
     */
    @Test
    void shouldRecoverUnexpiredClarifyingSessionFromDatabase() throws Exception {
        TravelSessionMapper mapper = mock(TravelSessionMapper.class);
        TravelSessionEntity entity = entity("session-demo", "CLARIFYING", LocalDateTime.now().plusHours(1));
        when(mapper.findBySessionId("session-demo")).thenReturn(Optional.of(entity));

        TravelSessionContext context = store(mapper, 24).findBySessionId("session-demo");

        assertThat(context).isNotNull();
        assertThat(context.sessionId()).isEqualTo("session-demo");
        assertThat(context.partialIntent().destinationPreferences()).contains("杭州");
        assertThat(context.lastQuestions()).extracting("field").contains("departureCity");
    }

    /**
     * 验证过期会话不会被恢复。
     */
    @Test
    void shouldIgnoreExpiredSession() throws Exception {
        TravelSessionMapper mapper = mock(TravelSessionMapper.class);
        TravelSessionEntity entity = entity("session-expired", "CLARIFYING", LocalDateTime.now().minusMinutes(1));
        when(mapper.findBySessionId("session-expired")).thenReturn(Optional.of(entity));

        TravelSessionContext context = store(mapper, 24).findBySessionId("session-expired");

        assertThat(context).isNull();
    }

    /**
     * 验证已完成会话可以被恢复，用于后续细化和调整计划。
     */
    @Test
    void shouldRecoverCompletedSessionForFollowUpAdjustment() throws Exception {
        TravelSessionMapper mapper = mock(TravelSessionMapper.class);
        TravelSessionEntity entity = entity("session-completed", "COMPLETED", LocalDateTime.now().plusHours(1));
        when(mapper.findBySessionId("session-completed")).thenReturn(Optional.of(entity));
        TravelSessionStore store = store(mapper, 24);

        TravelSessionContext context = store.findBySessionId("session-completed");

        assertThat(context).isNotNull();
        assertThat(context.status()).isEqualTo("COMPLETED");
        store.markCompleted("session-completed", intent("杭州"));

        verify(mapper).markCompleted(any(), any(), any(), any(), any());
    }

    /**
     * 验证完成态会话会保存上一轮完整计划上下文。
     *
     * @throws Exception JSON 序列化异常
     */
    @Test
    void shouldSavePreviousPlanContextWhenCompletedResponseExists() throws Exception {
        TravelSessionMapper mapper = mock(TravelSessionMapper.class);
        TravelSessionEntity entity = entity("session-completed", "COMPLETED", LocalDateTime.now().plusHours(1));
        when(mapper.findBySessionId("session-completed")).thenReturn(Optional.of(entity));
        TravelPlanResponse response = TravelPlanResponse.completed(
                "session-completed",
                intent("杭州"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                plan(),
                List.of(),
                null
        ).withPlanId("plan-1");
        ArgumentCaptor<String> responseJsonCaptor = ArgumentCaptor.forClass(String.class);

        store(mapper, 24).markCompleted("session-completed", response);

        verify(mapper).markCompleted(any(), any(), responseJsonCaptor.capture(), any(), any());
        assertThat(responseJsonCaptor.getValue()).contains("plan-1", "recommendedPlan", "杭州");
    }

    /**
     * 验证已有追问会话再次保存时会更新数据库记录。
     */
    @Test
    void shouldUpdateExistingClarifyingSession() throws Exception {
        TravelSessionMapper mapper = mock(TravelSessionMapper.class);
        TravelSessionEntity entity = entity("session-demo", "CLARIFYING", LocalDateTime.now().plusHours(1));
        when(mapper.findBySessionId("session-demo")).thenReturn(Optional.of(entity));

        TravelSessionContext context = store(mapper, 24).saveClarifying("session-demo", intent("上海"), List.of(question("budget")));

        verify(mapper).updateClarifying(any(TravelSessionEntity.class));
        assertThat(context.sessionId()).isEqualTo("session-demo");
        assertThat(context.partialIntent().destinationPreferences()).contains("上海");
    }

    /**
     * 构造测试用会话仓库。
     *
     * @param mapper 会话 Mapper
     * @param ttlHours 会话过期小时数
     * @return 注入测试依赖后的会话仓库
     */
    private TravelSessionStore store(TravelSessionMapper mapper, int ttlHours) {
        TravelAgentProperties properties = new TravelAgentProperties();
        properties.getPlanning().setSessionTtlHours(ttlHours);
        TravelSessionStore store = new TravelSessionStore();
        ReflectionTestUtils.setField(store, "objectMapper", new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(store, "travelSessionMapper", mapper);
        ReflectionTestUtils.setField(store, "travelAgentProperties", properties);
        return store;
    }

    /**
     * 构造测试用会话实体。
     *
     * @param sessionId 会话编号
     * @param status 会话状态
     * @param expiresAt 过期时间
     * @return 会话实体
     * @throws Exception JSON 序列化异常
     */
    private TravelSessionEntity entity(String sessionId, String status, LocalDateTime expiresAt) throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        TravelSessionEntity entity = new TravelSessionEntity();
        entity.setSessionId(sessionId);
        entity.setStatus(status);
        entity.setPartialIntentJson(mapper.writeValueAsString(intent("杭州")));
        entity.setLastQuestionsJson(mapper.writeValueAsString(List.of(question("departureCity"))));
        entity.setExpiresAt(expiresAt);
        entity.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        entity.setUpdatedAt(LocalDateTime.now().minusMinutes(1));
        return entity;
    }

    /**
     * 构造测试用旅行意图。
     *
     * @param destination 目的地
     * @return 旅行意图
     */
    private TravelIntent intent(String destination) {
        return new TravelIntent("西安", "五一", 4, 2, BigDecimal.valueOf(3000), List.of(destination), List.of("不想太累"), null, null, List.of());
    }

    /**
     * 构造测试用结构化追问。
     *
     * @param field 字段名
     * @return 结构化追问
     */
    private ClarificationQuestion question(String field) {
        return new ClarificationQuestion(field, "请补充" + field, "示例", true);
    }

    /**
     * 构造测试用旅行计划。
     *
     * @return 旅行计划
     */
    private TravelPlan plan() {
        return new TravelPlan(
                "杭州2日游",
                "测试计划",
                List.of("西安", "杭州"),
                List.of(new DailyPlan(1, "杭州", "09:00 西湖", "14:00 灵隐寺", "18:00 湖滨晚餐", FatigueLevel.MEDIUM, List.of())),
                List.of("高铁往返"),
                List.of("住西湖附近"),
                Map.of("total", 3000),
                List.of("节假日人流需确认"),
                List.of("确认门票")
        );
    }
}
