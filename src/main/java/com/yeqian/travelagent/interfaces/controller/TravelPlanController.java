package com.yeqian.travelagent.interfaces.controller;

import com.yeqian.travelagent.application.dto.TravelPlanRequest;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import com.yeqian.travelagent.application.service.TravelPlanningApplicationService;
import com.yeqian.travelagent.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 旅行计划控制器。
 *
 * <p>提供旅行计划生成入口；第一阶段只开放 POST 接口，不实现前端和历史查询。</p>
 */
@RestController
@RequestMapping("/api/travel/plans")
@Tag(name = "旅行计划", description = "提供旅行计划生成、多轮补充和历史计划查询接口。")
public class TravelPlanController {

    @Resource
    private TravelPlanningApplicationService travelPlanningApplicationService;

    /**
     * 创建旅行计划。
     *
     * @param request 旅行计划请求
     * @return 统一旅行计划响应
     */
    @PostMapping
    @Operation(summary = "创建旅行计划", description = "根据用户自然语言旅行需求生成旅行计划；信息不足时返回澄清问题和会话编号。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功或需要补充信息",
                    content = @Content(
                            schema = @Schema(implementation = Result.class),
                            examples = {
                                    @ExampleObject(name = "完整计划响应", value = """
                                            {
                                              "code": 0,
                                              "message": "success",
                                              "data": {
                                                "planId": "0b0fd5a2-3a25-43b6-9bfb-2cf3c6b79d11",
                                                "sessionId": "session_demo",
                                                "needClarification": false,
                                                "clarificationQuestions": [],
                                                "structuredClarificationQuestions": [],
                                                "intent": {
                                                  "departureCity": "西安",
                                                  "dateText": "五一",
                                                  "days": 4,
                                                  "peopleCount": 2,
                                                  "budget": 5000,
                                                  "destinationPreferences": ["杭州", "上海周边"],
                                                  "travelStyles": ["不想太累", "节假日"]
                                                },
                                                "evidences": [
                                                  {
                                                    "evidenceType": "WEATHER",
                                                    "city": "杭州",
                                                    "keyFacts": {
                                                      "sourceStatus": "SUCCESS",
                                                      "weatherRisk": "LOW",
                                                      "needSecondConfirm": false
                                                    }
                                                  }
                                                ],
                                                "recommendedPlan": {
                                                  "title": "杭州 + 上海周边4天旅行计划",
                                                  "route": ["西安", "杭州", "上海周边", "西安"],
                                                  "dailyPlans": []
                                                },
                                                "imageBrief": {
                                                  "title": "杭州 + 上海周边4天旅行计划",
                                                  "sections": []
                                                },
                                                "risks": ["节假日人流和票务风险需提前确认"]
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "追问信息响应", value = """
                                            {
                                              "code": 0,
                                              "message": "success",
                                              "data": {
                                                "planId": null,
                                                "sessionId": "session_6f4f0b2e",
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
                                                  "days": 4,
                                                  "peopleCount": 2,
                                                  "destinationPreferences": ["杭州"]
                                                }
                                              }
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "请求参数不合法",
                    content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "500", description = "系统暂时无法生成旅行计划",
                    content = @Content(schema = @Schema(implementation = Result.class)))
    })
    public Result<TravelPlanResponse> createPlan(@Valid @RequestBody TravelPlanRequest request) {
        return Result.success(travelPlanningApplicationService.plan(request));
    }

    /**
     * 查询历史旅行计划。
     *
     * @param planId 计划编号
     * @return 统一历史旅行计划响应
     */
    @GetMapping("/{planId}")
    @Operation(summary = "查询历史旅行计划", description = "根据计划编号查询已保存的旅行计划。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "404", description = "计划不存在",
                    content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "500", description = "系统暂时无法查询旅行计划",
                    content = @Content(schema = @Schema(implementation = Result.class)))
    })
    public Result<TravelPlanResponse> getPlan(
            @Parameter(description = "计划编号", required = true, example = "plan-20260428-001")
            @PathVariable String planId
    ) {
        return Result.success(travelPlanningApplicationService.findByPlanId(planId));
    }
}
