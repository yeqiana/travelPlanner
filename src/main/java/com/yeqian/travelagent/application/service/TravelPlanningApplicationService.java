package com.yeqian.travelagent.application.service;

import com.yeqian.travelagent.agent.orchestrator.TravelAgentOrchestrator;
import com.yeqian.travelagent.agent.session.TravelSessionStore;
import com.yeqian.travelagent.application.dto.TravelPlanRequest;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 旅行规划应用服务。
 *
 * <p>作为接口层和 Agent 编排层之间的薄应用服务，保持职责清晰。</p>
 */
@Service
public class TravelPlanningApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TravelPlanningApplicationService.class);

    @Resource
    private TravelAgentOrchestrator travelAgentOrchestrator;

    @Resource
    private TravelPlanPersistenceService travelPlanPersistenceService;

    @Resource
    private TravelSessionStore travelSessionStore;

    /**
     * 创建旅行计划。
     *
     * @param request 旅行计划请求
     * @return 旅行计划响应
     */
    public TravelPlanResponse plan(TravelPlanRequest request) {
        String traceId = ensureTraceId();
        long startMillis = System.currentTimeMillis();
        try {
            log.info("旅行计划请求开始：traceId={}, sessionId={}, message={}", traceId, request.sessionId(), abbreviate(request.message()));
            TravelPlanResponse response = travelAgentOrchestrator.plan(request);
            log.info("旅行计划编排完成：traceId={}, needClarification={}, sessionId={}, tasks={}, evidences={}, reminders={}",
                    traceId,
                    response.needClarification(),
                    response.sessionId(),
                    response.tasks().size(),
                    response.evidences().size(),
                    response.reminders().size());
            // 是否需要用户补充旅行信息
            if (response.needClarification()) {
                log.info("旅行计划请求结束：traceId={}, result=NEED_CLARIFICATION, costMillis={}", traceId, System.currentTimeMillis() - startMillis);
                return response;
            }
            TravelPlanResponse savedResponse = travelPlanPersistenceService.saveCompletedPlan(response);
            travelSessionStore.markCompleted(savedResponse.sessionId(), savedResponse);
            log.info("旅行计划请求结束：traceId={}, result=COMPLETED, planId={}, costMillis={}",
                    traceId,
                    savedResponse.planId(),
                    System.currentTimeMillis() - startMillis);
            return savedResponse;
        } catch (RuntimeException exception) {
            log.error("旅行计划请求失败：traceId={}, sessionId={}, costMillis={}, error={}",
                    traceId,
                    request.sessionId(),
                    System.currentTimeMillis() - startMillis,
                    exception.getMessage(),
                    exception);
            throw exception;
        } finally {
            MDC.remove("traceId");
        }
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

    /**
     * 确保当前请求存在链路编号。
     *
     * @return 链路编号
     */
    private String ensureTraceId() {
        String currentTraceId = MDC.get("traceId");
        if (currentTraceId != null && !currentTraceId.isBlank()) {
            return currentTraceId;
        }
        String traceId = "travel-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        return traceId;
    }

    /**
     * 缩短日志中的用户输入文本。
     *
     * @param value 原始文本
     * @return 缩短后的文本
     */
    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }
}
