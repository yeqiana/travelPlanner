package com.yeqian.travelagent.interfaces.controller;

import com.yeqian.travelagent.application.dto.TravelPlanRequest;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import com.yeqian.travelagent.application.service.TravelPlanningApplicationService;
import com.yeqian.travelagent.common.result.Result;
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
    public Result<TravelPlanResponse> getPlan(@PathVariable String planId) {
        return Result.success(travelPlanningApplicationService.findByPlanId(planId));
    }
}
