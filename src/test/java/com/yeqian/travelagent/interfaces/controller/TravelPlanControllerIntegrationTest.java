package com.yeqian.travelagent.interfaces.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.application.dto.TravelPlanRequest;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import com.yeqian.travelagent.application.service.TravelPlanningApplicationService;
import com.yeqian.travelagent.domain.enums.FatigueLevel;
import com.yeqian.travelagent.domain.enums.ReminderType;
import com.yeqian.travelagent.domain.model.DailyPlan;
import com.yeqian.travelagent.domain.model.ImageBrief;
import com.yeqian.travelagent.domain.model.ImageBriefSection;
import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelReminder;
import com.yeqian.travelagent.domain.model.TravelScore;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 旅行计划控制器集成测试。
 */
class TravelPlanControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    /**
     * 验证完整输入可以通过接口生成完整旅行计划。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldCreateCompletePlan() throws Exception {
        TravelPlanResponse response = completeResponse().withPlanId("plan-test-001");
        TravelPlanningApplicationService service = mock(TravelPlanningApplicationService.class);
        when(service.plan(any(TravelPlanRequest.class))).thenReturn(response);

        mockMvc(service).perform(post("/api/travel/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"五一从西安出发去杭州玩3天，两个人，预算3000\",\"sessionId\":\"s1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.planId").value("plan-test-001"))
                .andExpect(jsonPath("$.data.needClarification").value(false))
                .andExpect(jsonPath("$.data.recommendedPlan.dailyPlans[0].city").value("杭州"))
                .andExpect(jsonPath("$.data.score.score.totalScore").value(88))
                .andExpect(jsonPath("$.data.reminders[0].type").value("TICKET"))
                .andExpect(jsonPath("$.data.imageBrief.title").value("杭州3天旅行计划"))
                .andExpect(jsonPath("$.data.risks[0]").value("节假日人流风险"));
    }

    /**
     * 验证完整计划响应契约快照保持稳定。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldMatchCompletePlanResponseSnapshot() throws Exception {
        TravelPlanResponse response = completeResponse().withPlanId("plan-test-001");
        TravelPlanningApplicationService service = mock(TravelPlanningApplicationService.class);
        when(service.plan(any(TravelPlanRequest.class))).thenReturn(response);

        MvcResult result = mockMvc(service).perform(post("/api/travel/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"五一从西安出发去杭州玩3天，两个人，预算3000\",\"sessionId\":\"s1\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JSONAssert.assertEquals("""
                {
                  "code": 0,
                  "message": "success",
                  "data": {
                    "planId": "plan-test-001",
                    "needClarification": false,
                    "intent": {
                      "departureCity": "西安",
                      "dateText": "五一",
                      "days": 3,
                      "peopleCount": 2,
                      "budget": 3000,
                      "destinationPreferences": ["杭州"],
                      "travelStyles": ["不想太累"]
                    },
                    "recommendedPlan": {
                      "title": "杭州3天旅行计划",
                      "route": ["西安", "杭州", "西安"]
                    },
                    "score": {
                      "score": {
                        "totalScore": 88
                      }
                    },
                    "reminders": [
                      {
                        "title": "确认车票",
                        "type": "TICKET"
                      }
                    ],
                    "imageBrief": {
                      "title": "杭州3天旅行计划"
                    },
                    "risks": ["节假日人流风险"]
                  }
                }
                """, responseBody(result), false);
    }

    /**
     * 验证创建后可以通过计划编号查询历史计划。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldFindCreatedPlanByPlanId() throws Exception {
        TravelPlanResponse response = completeResponse().withPlanId("plan-test-001");
        TravelPlanningApplicationService service = mock(TravelPlanningApplicationService.class);
        when(service.findByPlanId("plan-test-001")).thenReturn(response);

        mockMvc(service).perform(get("/api/travel/plans/plan-test-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.planId").value("plan-test-001"))
                .andExpect(jsonPath("$.data.recommendedPlan.title").value("杭州3天旅行计划"));
    }

    /**
     * 验证历史计划查询响应契约快照保持稳定。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldMatchFindPlanResponseSnapshot() throws Exception {
        TravelPlanResponse response = completeResponse().withPlanId("plan-test-001");
        TravelPlanningApplicationService service = mock(TravelPlanningApplicationService.class);
        when(service.findByPlanId("plan-test-001")).thenReturn(response);

        MvcResult result = mockMvc(service).perform(get("/api/travel/plans/plan-test-001"))
                .andExpect(status().isOk())
                .andReturn();

        JSONAssert.assertEquals("""
                {
                  "code": 0,
                  "message": "success",
                  "data": {
                    "planId": "plan-test-001",
                    "needClarification": false,
                    "recommendedPlan": {
                      "title": "杭州3天旅行计划"
                    },
                    "imageBrief": {
                      "title": "杭州3天旅行计划"
                    }
                  }
                }
                """, responseBody(result), false);
    }

    /**
     * 验证缺少出发地时接口返回追问信息。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnClarificationQuestions() throws Exception {
        TravelIntent intent = new TravelIntent(null, "五一", 3, 1, null, List.of("杭州"), List.of(), null, null, List.of());
        TravelPlanningApplicationService service = mock(TravelPlanningApplicationService.class);
        when(service.plan(any(TravelPlanRequest.class)))
                .thenReturn(TravelPlanResponse.needClarification(List.of("你是从哪个城市出发？"), intent));

        mockMvc(service).perform(post("/api/travel/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"五一去杭州玩3天\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needClarification").value(true))
                .andExpect(jsonPath("$.data.clarificationQuestions[0]").value("你是从哪个城市出发？"));
    }

    /**
     * 验证追问响应契约快照保持稳定。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldMatchClarificationResponseSnapshot() throws Exception {
        TravelIntent intent = new TravelIntent(null, "五一", 3, 1, null, List.of("杭州"), List.of(), null, null, List.of());
        TravelPlanningApplicationService service = mock(TravelPlanningApplicationService.class);
        when(service.plan(any(TravelPlanRequest.class))).thenReturn(TravelPlanResponse.needClarification(
                "session-test-001",
                List.of("你是从哪个城市出发？"),
                List.of(new com.yeqian.travelagent.domain.model.ClarificationQuestion("departureCity", "你是从哪个城市出发？", "例如：西安", true)),
                intent
        ));

        MvcResult result = mockMvc(service).perform(post("/api/travel/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"五一去杭州玩3天\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JSONAssert.assertEquals("""
                {
                  "code": 0,
                  "message": "success",
                  "data": {
                    "planId": null,
                    "sessionId": "session-test-001",
                    "needClarification": true,
                    "clarificationQuestions": ["你是从哪个城市出发？"],
                    "structuredClarificationQuestions": [
                      {
                        "field": "departureCity",
                        "question": "你是从哪个城市出发？",
                        "example": "例如：西安",
                        "required": true
                      }
                    ],
                    "intent": {
                      "departureCity": null,
                      "dateText": "五一",
                      "days": 3,
                      "peopleCount": 1,
                      "destinationPreferences": ["杭州"]
                    }
                  }
                }
                """, responseBody(result), false);
    }

    /**
     * 构造注入 mock 服务的 MockMvc。
     *
     * @param service 应用服务
     * @return MockMvc 实例
     */
    private MockMvc mockMvc(TravelPlanningApplicationService service) {
        TravelPlanController controller = new TravelPlanController();
        ReflectionTestUtils.setField(controller, "travelPlanningApplicationService", service);
        return MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 读取接口响应正文。
     *
     * @param result MockMvc 执行结果
     * @return JSON 响应正文
     * @throws Exception JSON 解析异常
     */
    private String responseBody(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return objectMapper.writeValueAsString(jsonNode);
    }

    /**
     * 构造完整计划响应。
     *
     * @return 完整计划响应
     */
    private TravelPlanResponse completeResponse() {
        TravelIntent intent = new TravelIntent("西安", "五一", 3, 2, BigDecimal.valueOf(3000), List.of("杭州"), List.of("不想太累"), null, null, List.of());
        DailyPlan dailyPlan = new DailyPlan(1, "杭州", "抵达杭州", "西湖轻量游", "早点休息", FatigueLevel.LOW, List.of("确认开放时间"));
        TravelPlan plan = new TravelPlan("杭州3天旅行计划", "摘要", List.of("西安", "杭州", "西安"), List.of(dailyPlan), List.of("高铁优先"), List.of("地铁附近"), Map.of("total", 2600), List.of("节假日人流风险"), List.of("确认车票"));
        TravelCandidatePlan candidate = new TravelCandidatePlan("杭州轻量游", List.of("西安", "杭州", "西安"), "低疲劳");
        TravelScore score = new TravelScore(88, 90, 86, 84, 88, 80, 78, "推荐");
        ScoredTravelPlan scoredPlan = new ScoredTravelPlan(candidate, score);
        List<TravelReminder> reminders = List.of(new TravelReminder("确认车票", ReminderType.TICKET, "出发前5天", "检查车票"));
        ImageBrief imageBrief = new ImageBrief("杭州3天旅行计划", "副标题", List.of(new ImageBriefSection("路线", "西安 -> 杭州 -> 西安")));
        return TravelPlanResponse.completed(intent, List.of(), List.of(), List.of(), List.of(candidate), List.of(scoredPlan), plan, reminders, imageBrief);
    }
}
