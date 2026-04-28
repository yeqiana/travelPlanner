package com.yeqian.travelagent.interfaces.controller;

import com.yeqian.travelagent.application.dto.TravelPlanRequest;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import com.yeqian.travelagent.application.service.TravelPlanningApplicationService;
import com.yeqian.travelagent.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
                    content = @Content(schema = @Schema(implementation = Result.class))),
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
