package com.yeqian.travelagent.agent.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.agent.checker.MissingInfoChecker;
import com.yeqian.travelagent.agent.generator.ImageBriefGenerator;
import com.yeqian.travelagent.agent.generator.ReminderGenerator;
import com.yeqian.travelagent.agent.normalizer.EvidenceNormalizer;
import com.yeqian.travelagent.agent.parser.TravelIntentParser;
import com.yeqian.travelagent.agent.planner.CandidatePlanGenerator;
import com.yeqian.travelagent.agent.planner.ItineraryPlanner;
import com.yeqian.travelagent.agent.planner.TravelTaskPlanner;
import com.yeqian.travelagent.agent.scorer.TravelScorer;
import com.yeqian.travelagent.application.dto.TravelPlanRequest;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import com.yeqian.travelagent.infrastructure.ai.JsonExtractor;
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

import static org.assertj.core.api.Assertions.assertThat;

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
    }

    /**
     * 构造测试用编排器。
     *
     * @return 注入测试依赖后的编排器
     */
    private TravelAgentOrchestrator buildOrchestrator() {
        TravelAgentOrchestrator orchestrator = new TravelAgentOrchestrator();
        ReflectionTestUtils.setField(orchestrator, "travelIntentParser", travelIntentParser());
        ReflectionTestUtils.setField(orchestrator, "missingInfoChecker", new MissingInfoChecker());
        ReflectionTestUtils.setField(orchestrator, "travelTaskPlanner", new TravelTaskPlanner());
        ReflectionTestUtils.setField(orchestrator, "toolExecutor", toolExecutor());
        ReflectionTestUtils.setField(orchestrator, "evidenceNormalizer", new EvidenceNormalizer());
        ReflectionTestUtils.setField(orchestrator, "candidatePlanGenerator", new CandidatePlanGenerator());
        ReflectionTestUtils.setField(orchestrator, "travelScorer", new TravelScorer());
        ReflectionTestUtils.setField(orchestrator, "itineraryPlanner", new ItineraryPlanner());
        ReflectionTestUtils.setField(orchestrator, "reminderGenerator", new ReminderGenerator());
        ReflectionTestUtils.setField(orchestrator, "imageBriefGenerator", new ImageBriefGenerator());
        return orchestrator;
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
