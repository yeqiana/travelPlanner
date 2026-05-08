package com.yeqian.travelagent.agent.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.agent.adjuster.TravelPlanLocalAdjuster;
import com.yeqian.travelagent.agent.checker.MissingInfoChecker;
import com.yeqian.travelagent.agent.generator.ImageBriefGenerator;
import com.yeqian.travelagent.agent.generator.ReminderGenerator;
import com.yeqian.travelagent.agent.intent.TravelDialogIntentRecognizer;
import com.yeqian.travelagent.agent.normalizer.EvidenceNormalizer;
import com.yeqian.travelagent.agent.parser.TravelIntentParser;
import com.yeqian.travelagent.agent.planner.CandidatePlanGenerator;
import com.yeqian.travelagent.agent.planner.ItineraryPlanner;
import com.yeqian.travelagent.agent.planner.TravelTaskPlanner;
import com.yeqian.travelagent.agent.scorer.TravelScorer;
import com.yeqian.travelagent.agent.session.TravelSessionStore;
import com.yeqian.travelagent.agent.suggestion.ContextualSuggestionGenerator;
import com.yeqian.travelagent.application.dto.TravelPlanRequest;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import com.yeqian.travelagent.infrastructure.ai.JsonExtractor;
import com.yeqian.travelagent.infrastructure.config.TravelAgentProperties;
import com.yeqian.travelagent.infrastructure.persistence.entity.TravelSessionEntity;
import com.yeqian.travelagent.infrastructure.persistence.mapper.TravelSessionMapper;
import com.yeqian.travelagent.tool.MockAttractionInfoTool;
import com.yeqian.travelagent.tool.MockHotelSearchTool;
import com.yeqian.travelagent.tool.MockRouteTool;
import com.yeqian.travelagent.tool.MockTransportSearchTool;
import com.yeqian.travelagent.tool.MockWeatherTool;
import com.yeqian.travelagent.tool.MockWebSearchTool;
import com.yeqian.travelagent.tool.ToolExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 旅行 Agent 编排器测试。
 */
class TravelAgentOrchestratorTest {

    /**
     * 验证完整输入可以返回包含行程、提醒、一图流和风险提示的完整计划。
     */
    @Test
    void shouldReturnCompletePlanForCompleteInput() {
        TravelAgentOrchestrator orchestrator = buildOrchestrator();

        TravelPlanResponse response = orchestrator.plan(new TravelPlanRequest(
                "五一从西安出发去杭州和上海玩4天，两个人，预算5000，不想太累",
                "test-session"
        ));

        assertThat(response.needClarification()).isFalse();
        assertThat(response.recommendedPlan()).isNotNull();
        assertThat(response.recommendedPlan().dailyPlans()).isNotEmpty();
        assertThat(response.reminders()).isNotEmpty();
        assertThat(response.imageBrief()).isNotNull();
        assertThat(response.imageBrief().sections()).isNotEmpty();
        assertThat(response.recommendedPlan().risks()).isNotEmpty();
        assertThat(response.contextualSuggestions()).isNotEmpty();
    }

    /**
     * 验证缺少出发地时返回会话编号，并且第二轮携带会话编号可以合并上下文继续生成完整计划。
     */
    @Test
    void shouldMergeIntentBySessionIdForMultiTurnClarification() {
        TravelAgentOrchestrator orchestrator = buildOrchestrator();

        TravelPlanResponse firstResponse = orchestrator.plan(new TravelPlanRequest(
                "五一想出去玩4天，两个人，预算3000，不想太累",
                null
        ));

        assertThat(firstResponse.needClarification()).isTrue();
        assertThat(firstResponse.sessionId()).isNotBlank();
        assertThat(firstResponse.clarificationQuestions()).contains("你是从哪个城市出发？");
        assertThat(firstResponse.structuredClarificationQuestions())
                .extracting("field")
                .contains("departureCity");

        TravelPlanResponse secondResponse = orchestrator.plan(new TravelPlanRequest(
                "我从西安出发，想去杭州上海周边",
                firstResponse.sessionId()
        ));

        assertThat(secondResponse.needClarification()).isFalse();
        assertThat(secondResponse.sessionId()).isEqualTo(firstResponse.sessionId());
        assertThat(secondResponse.intent().departureCity()).isEqualTo("西安");
        assertThat(secondResponse.intent().days()).isEqualTo(4);
        assertThat(secondResponse.intent().peopleCount()).isEqualTo(2);
        assertThat(secondResponse.intent().destinationPreferences()).contains("杭州");
        assertThat(secondResponse.recommendedPlan()).isNotNull();
    }

    /**
     * 验证完成态会话中的后续细化请求会复用上一轮旅行意图。
     */
    @Test
    void shouldDetailPreviousPlanByCompletedSession() {
        TravelAgentOrchestrator orchestrator = buildOrchestrator();

        TravelPlanResponse firstResponse = orchestrator.plan(new TravelPlanRequest(
                "五一从西安出发去杭州玩3天，两个人，预算3000，不想太累",
                "completed-session"
        ));

        TravelPlanResponse secondResponse = orchestrator.plan(new TravelPlanRequest(
                "没有详细的旅游计划啊",
                firstResponse.sessionId()
        ));

        assertThat(secondResponse.needClarification()).isFalse();
        assertThat(secondResponse.dialogIntent().name()).isEqualTo("DETAIL_PLAN");
        assertThat(secondResponse.intent().departureCity()).isEqualTo("西安");
        assertThat(secondResponse.intent().destinationPreferences()).contains("杭州");
        assertThat(secondResponse.recommendedPlan().dailyPlans().get(0).morning()).contains("08:30", "需二次确认");
    }

    /**
     * 验证局部调整类请求会被识别为调整计划。
     */
    @Test
    void shouldRecognizeAdjustPlanForFollowUpChange() {
        TravelAgentOrchestrator orchestrator = buildOrchestrator();
        TravelPlanResponse firstResponse = orchestrator.plan(new TravelPlanRequest(
                "五一从西安出发去杭州玩3天，两个人，预算3000，不想太累",
                "adjust-session"
        ));

        TravelPlanResponse secondResponse = orchestrator.plan(new TravelPlanRequest(
                "第二天轻松一点",
                firstResponse.sessionId()
        ));

        assertThat(secondResponse.dialogIntent().name()).isEqualTo("ADJUST_PLAN");
        assertThat(secondResponse.intent().days()).isEqualTo(3);
        assertThat(secondResponse.contextualSuggestions()).isNotEmpty();
    }

    /**
     * 构造测试用编排器。
     *
     * @return 注入测试依赖后的编排器
     */
    private TravelAgentOrchestrator buildOrchestrator() {
        TravelAgentOrchestrator orchestrator = new TravelAgentOrchestrator();
        ReflectionTestUtils.setField(orchestrator, "travelIntentParser", travelIntentParser());
        ReflectionTestUtils.setField(orchestrator, "travelDialogIntentRecognizer", new TravelDialogIntentRecognizer());
        ReflectionTestUtils.setField(orchestrator, "missingInfoChecker", new MissingInfoChecker());
        ReflectionTestUtils.setField(orchestrator, "travelTaskPlanner", new TravelTaskPlanner());
        ReflectionTestUtils.setField(orchestrator, "toolExecutor", toolExecutor());
        ReflectionTestUtils.setField(orchestrator, "evidenceNormalizer", new EvidenceNormalizer());
        ReflectionTestUtils.setField(orchestrator, "candidatePlanGenerator", new CandidatePlanGenerator());
        ReflectionTestUtils.setField(orchestrator, "travelScorer", new TravelScorer());
        ReflectionTestUtils.setField(orchestrator, "itineraryPlanner", new ItineraryPlanner());
        ReflectionTestUtils.setField(orchestrator, "travelPlanLocalAdjuster", new TravelPlanLocalAdjuster());
        ReflectionTestUtils.setField(orchestrator, "reminderGenerator", new ReminderGenerator());
        ReflectionTestUtils.setField(orchestrator, "imageBriefGenerator", new ImageBriefGenerator());
        ReflectionTestUtils.setField(orchestrator, "contextualSuggestionGenerator", new ContextualSuggestionGenerator());
        ReflectionTestUtils.setField(orchestrator, "travelSessionStore", travelSessionStore());
        return orchestrator;
    }

    /**
     * 构造测试用持久化会话仓库。
     *
     * @return 注入内存 mapper 的会话仓库
     */
    private TravelSessionStore travelSessionStore() {
        Map<String, TravelSessionEntity> sessions = new ConcurrentHashMap<>();
        TravelSessionMapper mapper = mock(TravelSessionMapper.class);
        when(mapper.findBySessionId(any())).thenAnswer(invocation -> Optional.ofNullable(sessions.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            TravelSessionEntity entity = invocation.getArgument(0);
            sessions.put(entity.getSessionId(), entity);
            return null;
        }).when(mapper).insert(any(TravelSessionEntity.class));
        doAnswer(invocation -> {
            TravelSessionEntity entity = invocation.getArgument(0);
            sessions.put(entity.getSessionId(), entity);
            return null;
        }).when(mapper).updateClarifying(any(TravelSessionEntity.class));
        doAnswer(invocation -> {
            String sessionId = invocation.getArgument(0);
            TravelSessionEntity entity = sessions.get(sessionId);
            if (entity != null) {
                entity.setStatus("COMPLETED");
                entity.setPartialIntentJson(invocation.getArgument(1));
                entity.setLastQuestionsJson("[]");
            }
            return null;
        }).when(mapper).markCompleted(any(), any(), any(), any(), any());

        TravelAgentProperties properties = new TravelAgentProperties();
        TravelSessionStore store = new TravelSessionStore();
        ReflectionTestUtils.setField(store, "objectMapper", new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(store, "travelSessionMapper", mapper);
        ReflectionTestUtils.setField(store, "travelAgentProperties", properties);
        return store;
    }

    /**
     * 构造测试用旅行意图解析器。
     *
     * @return 注入规则解析依赖后的解析器
     */
    private TravelIntentParser travelIntentParser() {
        TravelIntentParser parser = new TravelIntentParser();
        ReflectionTestUtils.setField(parser, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(parser, "jsonExtractor", new JsonExtractor());
        ReflectionTestUtils.setField(parser, "chatClientBuilderProvider", emptyChatClientBuilderProvider());
        return parser;
    }

    /**
     * 构造空模型客户端提供器，让解析器走规则兜底。
     *
     * @return 空模型客户端提供器
     */
    private ObjectProvider<ChatClient.Builder> emptyChatClientBuilderProvider() {
        return new ObjectProvider<>() {
            /**
             * 获取模型客户端构造器。
             *
             * @param args 参数列表
             * @return 始终返回 null
             */
            @Override
            public ChatClient.Builder getObject(Object... args) {
                return null;
            }

            /**
             * 获取模型客户端构造器。
             *
             * @return 始终返回 null
             */
            @Override
            public ChatClient.Builder getObject() {
                return null;
            }
        };
    }

    /**
     * 构造测试用工具执行器。
     *
     * @return 注入 mock 工具后的工具执行器
     */
    private ToolExecutor toolExecutor() {
        ToolExecutor executor = new ToolExecutor();
        ReflectionTestUtils.setField(executor, "travelTools", List.of(
                new MockTransportSearchTool(),
                new MockWeatherTool(),
                new MockHotelSearchTool(),
                new MockAttractionInfoTool(),
                new MockRouteTool(),
                new MockWebSearchTool()
        ));
        return executor;
    }
}
