package com.yeqian.travelagent.application.dto;

import com.yeqian.travelagent.domain.model.ImageBrief;
import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelReminder;
import com.yeqian.travelagent.domain.model.TravelTask;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 旅行计划响应 DTO。
 *
 * <p>承载旅行意图、澄清问题、查询任务、证据、候选方案、评分结果和最终计划。</p>
 *
 * @param planId 计划编号
 * @param needClarification 是否需要补充信息
 * @param clarificationQuestions 澄清问题列表
 * @param intent 旅行意图
 * @param tasks 查询任务列表
 * @param toolResults 工具调用结果列表
 * @param evidences 查询证据列表
 * @param candidatePlans 候选行程列表
 * @param scoredPlans 已评分候选方案列表
 * @param recommendedPlan 推荐旅行计划
 * @param score 当前最高分方案
 * @param reminders 旅行提醒列表
 * @param imageBrief 一图流文案
 * @param risks 风险提示列表
 * @param createdAt 响应创建时间
 */
public record TravelPlanResponse(
        String planId,
        boolean needClarification,
        List<String> clarificationQuestions,
        TravelIntent intent,
        List<TravelTask> tasks,
        List<ToolResult> toolResults,
        List<TravelEvidence> evidences,
        List<TravelCandidatePlan> candidatePlans,
        List<ScoredTravelPlan> scoredPlans,
        TravelPlan recommendedPlan,
        ScoredTravelPlan score,
        List<TravelReminder> reminders,
        ImageBrief imageBrief,
        List<String> risks,
        OffsetDateTime createdAt
) {

    /**
     * 创建旅行计划响应并规整空集合字段。
     */
    public TravelPlanResponse {
        clarificationQuestions = clarificationQuestions == null ? List.of() : List.copyOf(clarificationQuestions);
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        toolResults = toolResults == null ? List.of() : List.copyOf(toolResults);
        evidences = evidences == null ? List.of() : List.copyOf(evidences);
        candidatePlans = candidatePlans == null ? List.of() : List.copyOf(candidatePlans);
        scoredPlans = scoredPlans == null ? List.of() : List.copyOf(scoredPlans);
        reminders = reminders == null ? List.of() : List.copyOf(reminders);
        risks = risks == null ? List.of() : List.copyOf(risks);
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }

    /**
     * 构造需要补充信息的响应。
     *
     * @param questions 澄清问题列表
     * @param intent 已解析的旅行意图
     * @return 旅行计划响应
     */
    public static TravelPlanResponse needClarification(List<String> questions, TravelIntent intent) {
        return new TravelPlanResponse(null, true, questions, intent, List.of(), List.of(), List.of(), List.of(), List.of(), null, null, List.of(), null, List.of(), OffsetDateTime.now());
    }

    /**
     * 构造已解析但尚未拆解任务的响应。
     *
     * @param intent 旅行意图
     * @return 旅行计划响应
     */
    public static TravelPlanResponse parsed(TravelIntent intent) {
        return new TravelPlanResponse(null, false, List.of(), intent, List.of(), List.of(), List.of(), List.of(), List.of(), null, null, List.of(), null, List.of(), OffsetDateTime.now());
    }

    /**
     * 构造已完成任务拆解的响应。
     *
     * @param intent 旅行意图
     * @param tasks 查询任务列表
     * @return 旅行计划响应
     */
    public static TravelPlanResponse planned(TravelIntent intent, List<TravelTask> tasks) {
        return new TravelPlanResponse(null, false, List.of(), intent, tasks, List.of(), List.of(), List.of(), List.of(), null, null, List.of(), null, List.of(), OffsetDateTime.now());
    }

    /**
     * 构造已完成工具执行的响应。
     *
     * @param intent 旅行意图
     * @param tasks 查询任务列表
     * @param toolResults 工具调用结果列表
     * @return 旅行计划响应
     */
    public static TravelPlanResponse toolsExecuted(TravelIntent intent, List<TravelTask> tasks, List<ToolResult> toolResults) {
        return new TravelPlanResponse(null, false, List.of(), intent, tasks, toolResults, List.of(), List.of(), List.of(), null, null, List.of(), null, List.of(), OffsetDateTime.now());
    }

    /**
     * 构造已完成证据归一化、候选方案生成和评分的响应。
     *
     * @param intent 旅行意图
     * @param tasks 查询任务列表
     * @param toolResults 工具调用结果列表
     * @param evidences 查询证据列表
     * @param candidatePlans 候选行程列表
     * @param scoredPlans 已评分候选方案列表
     * @return 旅行计划响应
     */
    public static TravelPlanResponse scored(
            TravelIntent intent,
            List<TravelTask> tasks,
            List<ToolResult> toolResults,
            List<TravelEvidence> evidences,
            List<TravelCandidatePlan> candidatePlans,
            List<ScoredTravelPlan> scoredPlans
    ) {
        ScoredTravelPlan bestPlan = firstScoredPlan(scoredPlans);
        return new TravelPlanResponse(null, false, List.of(), intent, tasks, toolResults, evidences, candidatePlans, scoredPlans, null, bestPlan, List.of(), null, List.of(), OffsetDateTime.now());
    }

    /**
     * 构造最小闭环完整旅行计划响应。
     *
     * @param intent 旅行意图
     * @param tasks 查询任务列表
     * @param toolResults 工具调用结果列表
     * @param evidences 查询证据列表
     * @param candidatePlans 候选行程列表
     * @param scoredPlans 已评分候选方案列表
     * @param recommendedPlan 推荐旅行计划
     * @param reminders 旅行提醒列表
     * @param imageBrief 一图流文案
     * @return 旅行计划响应
     */
    public static TravelPlanResponse completed(
            TravelIntent intent,
            List<TravelTask> tasks,
            List<ToolResult> toolResults,
            List<TravelEvidence> evidences,
            List<TravelCandidatePlan> candidatePlans,
            List<ScoredTravelPlan> scoredPlans,
            TravelPlan recommendedPlan,
            List<TravelReminder> reminders,
            ImageBrief imageBrief
    ) {
        return new TravelPlanResponse(
                null,
                false,
                List.of(),
                intent,
                tasks,
                toolResults,
                evidences,
                candidatePlans,
                scoredPlans,
                recommendedPlan,
                firstScoredPlan(scoredPlans),
                reminders,
                imageBrief,
                recommendedPlan == null ? List.of() : recommendedPlan.risks(),
                OffsetDateTime.now()
        );
    }

    /**
     * 获取最高分方案。
     *
     * @param scoredPlans 已评分候选方案列表
     * @return 最高分方案，不存在时返回 null
     */
    /**
     * 复制当前响应并写入计划编号。
     *
     * @param planId 全局唯一计划编号
     * @return 带计划编号的旅行计划响应
     */
    public TravelPlanResponse withPlanId(String planId) {
        return new TravelPlanResponse(
                planId,
                needClarification,
                clarificationQuestions,
                intent,
                tasks,
                toolResults,
                evidences,
                candidatePlans,
                scoredPlans,
                recommendedPlan,
                score,
                reminders,
                imageBrief,
                risks,
                createdAt
        );
    }

    private static ScoredTravelPlan firstScoredPlan(List<ScoredTravelPlan> scoredPlans) {
        return scoredPlans == null || scoredPlans.isEmpty() ? null : scoredPlans.get(0);
    }
}
