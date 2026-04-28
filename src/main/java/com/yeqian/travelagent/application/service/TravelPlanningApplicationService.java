package com.yeqian.travelagent.application.service;

import com.yeqian.travelagent.agent.orchestrator.TravelAgentOrchestrator;
import com.yeqian.travelagent.application.dto.TravelPlanRequest;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 旅行规划应用服务。
 *
 * <p>作为接口层和 Agent 编排层之间的薄应用服务，保持职责清晰。</p>
 */
@Service
public class TravelPlanningApplicationService {

    @Resource
    private TravelAgentOrchestrator travelAgentOrchestrator;

    @Resource
    private TravelPlanPersistenceService travelPlanPersistenceService;

    /**
     * 创建旅行计划。
     *
     * @param request 旅行计划请求
     * @return 旅行计划响应
     */
    public TravelPlanResponse plan(TravelPlanRequest request) {
        TravelPlanResponse response = travelAgentOrchestrator.plan(request);
        if (response.needClarification()) {
            return response;
        }
        return travelPlanPersistenceService.saveCompletedPlan(response);
    }

    /**
     * 查询历史旅行计划。
     *
     * @param planId 计划编号
     * @return 历史旅行计划响应
     */
    public TravelPlanResponse findByPlanId(String planId) {
        return travelPlanPersistenceService.findByPlanId(planId);
    }
}
