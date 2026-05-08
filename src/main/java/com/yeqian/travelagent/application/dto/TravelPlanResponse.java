package com.yeqian.travelagent.application.dto;

import com.yeqian.travelagent.domain.model.ClarificationQuestion;
import com.yeqian.travelagent.domain.model.ImageBrief;
import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelReminder;
import com.yeqian.travelagent.domain.model.TravelTask;
import com.yeqian.travelagent.domain.enums.TravelDialogIntent;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 旅行计划响应 DTO。
 *
 * <p>承载旅行意图、澄清问题、查询任务、证据、候选方案、评分结果和最终计划。</p>
 *
 * @param planId 计划编号
 * @param sessionId 会话编号
 * @param needClarification 是否需要补充信息
 * @param clarificationQuestions 兼容旧接口的澄清问题列表
 * @param structuredClarificationQuestions 结构化澄清问题列表
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
 * @param dialogIntent 本轮多轮对话意图
 * @param contextualSuggestions 上下文快捷提示列表
 * @param createdAt 响应创建时间
 */
@Schema(description = "旅行计划响应", example = """
        {
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
            "travelStyles": ["不想太累", "节假日"],
            "transportPreference": null,
            "hotelBudgetPerNight": null,
            "avoidPlaces": []
          },
          "tasks": [],
          "toolResults": [],
          "evidences": [
            {
              "evidenceType": "WEATHER",
              "city": "杭州",
              "title": "杭州 天气证据",
              "summary": "杭州天气晴转多云，穿衣建议：按当季轻便衣物准备，出发前复查天气。",
              "keyFacts": {
                "sourceStatus": "SUCCESS",
                "needSecondConfirm": false,
                "confidence": 0.75,
                "weatherRisk": "LOW"
              },
              "confidence": 0.75,
              "sourceName": "MockWeatherTool",
              "sourceUrl": null,
              "fetchedAt": "2026-04-29T10:00:00+08:00"
            }
          ],
          "candidatePlans": [],
          "scoredPlans": [],
          "recommendedPlan": {
            "title": "杭州 + 上海周边4天旅行计划",
            "summary": "低疲劳节假日路线",
            "route": ["西安", "杭州", "上海周边", "西安"],
            "dailyPlans": []
          },
          "score": null,
          "reminders": [],
          "imageBrief": {
            "title": "杭州 + 上海周边4天旅行计划",
            "subtitle": "低疲劳节假日路线",
            "sections": []
          },
          "risks": ["节假日人流和票务风险需提前确认"],
          "createdAt": "2026-04-29T10:00:00+08:00"
        }
        """)
public record TravelPlanResponse(
        @Schema(description = "计划编号。需要补充信息或尚未落库时可能为空", example = "plan-20260428-001", nullable = true)
        String planId,
        @Schema(description = "会话编号。用于多轮补充旅行需求", example = "session-20260428-001", nullable = true)
        String sessionId,
        @Schema(description = "是否需要用户补充旅行信息", example = "false")
        boolean needClarification,
        @Schema(description = "兼容旧接口的澄清问题文本列表")
        List<String> clarificationQuestions,
        @Schema(description = "结构化澄清问题列表")
        List<ClarificationQuestion> structuredClarificationQuestions,
        @Schema(description = "解析后的旅行意图")
        TravelIntent intent,
        @Schema(description = "工具查询任务列表")
        List<TravelTask> tasks,
        @Schema(description = "工具调用结果列表")
        List<ToolResult> toolResults,
        @Schema(description = "归一化后的查询证据列表")
        List<TravelEvidence> evidences,
        @Schema(description = "候选旅行方案列表")
        List<TravelCandidatePlan> candidatePlans,
        @Schema(description = "已评分的候选旅行方案列表")
        List<ScoredTravelPlan> scoredPlans,
        @Schema(description = "推荐旅行计划")
        TravelPlan recommendedPlan,
        @Schema(description = "当前最高分候选方案")
        ScoredTravelPlan score,
        @Schema(description = "旅行提醒列表")
        List<TravelReminder> reminders,
        @Schema(description = "一图流文案")
        ImageBrief imageBrief,
        @Schema(description = "风险提示列表")
        List<String> risks,
        @Schema(description = "本轮多轮对话意图", example = "DETAIL_PLAN", nullable = true)
        TravelDialogIntent dialogIntent,
        @Schema(description = "上下文快捷提示列表")
        List<String> contextualSuggestions,
        @Schema(description = "响应创建时间")
        OffsetDateTime createdAt
) {

    /**
     * 创建旅行计划响应并规整空集合字段。
     */
    public TravelPlanResponse {
        clarificationQuestions = clarificationQuestions == null ? List.of() : List.copyOf(clarificationQuestions);
        structuredClarificationQuestions = structuredClarificationQuestions == null ? List.of() : List.copyOf(structuredClarificationQuestions);
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        toolResults = toolResults == null ? List.of() : List.copyOf(toolResults);
        evidences = evidences == null ? List.of() : List.copyOf(evidences);
        candidatePlans = candidatePlans == null ? List.of() : List.copyOf(candidatePlans);
        scoredPlans = scoredPlans == null ? List.of() : List.copyOf(scoredPlans);
        reminders = reminders == null ? List.of() : List.copyOf(reminders);
        risks = risks == null ? List.of() : List.copyOf(risks);
        contextualSuggestions = contextualSuggestions == null ? List.of() : List.copyOf(contextualSuggestions);
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
        return needClarification(null, questions, List.of(), intent);
    }

    /**
     * 构造需要补充信息的响应。
     *
     * @param sessionId 会话编号
     * @param questions 澄清问题列表
     * @param structuredQuestions 结构化澄清问题列表
     * @param intent 已解析的旅行意图
     * @return 旅行计划响应
     */
    public static TravelPlanResponse needClarification(
            String sessionId,
            List<String> questions,
            List<ClarificationQuestion> structuredQuestions,
            TravelIntent intent
    ) {
        return new TravelPlanResponse(null, sessionId, true, questions, structuredQuestions, intent, List.of(), List.of(), List.of(), List.of(), List.of(), null, null, List.of(), null, List.of(), null, List.of(), OffsetDateTime.now());
    }

    /**
     * 构造已解析但尚未拆解任务的响应。
     *
     * @param intent 旅行意图
     * @return 旅行计划响应
     */
    public static TravelPlanResponse parsed(TravelIntent intent) {
        return new TravelPlanResponse(null, null, false, List.of(), List.of(), intent, List.of(), List.of(), List.of(), List.of(), List.of(), null, null, List.of(), null, List.of(), null, List.of(), OffsetDateTime.now());
    }

    /**
     * 构造已完成任务拆解的响应。
     *
     * @param intent 旅行意图
     * @param tasks 查询任务列表
     * @return 旅行计划响应
     */
    public static TravelPlanResponse planned(TravelIntent intent, List<TravelTask> tasks) {
        return new TravelPlanResponse(null, null, false, List.of(), List.of(), intent, tasks, List.of(), List.of(), List.of(), List.of(), null, null, List.of(), null, List.of(), null, List.of(), OffsetDateTime.now());
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
        return new TravelPlanResponse(null, null, false, List.of(), List.of(), intent, tasks, toolResults, List.of(), List.of(), List.of(), null, null, List.of(), null, List.of(), null, List.of(), OffsetDateTime.now());
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
        return new TravelPlanResponse(null, null, false, List.of(), List.of(), intent, tasks, toolResults, evidences, candidatePlans, scoredPlans, null, bestPlan, List.of(), null, List.of(), null, List.of(), OffsetDateTime.now());
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
        return completed(null, intent, tasks, toolResults, evidences, candidatePlans, scoredPlans, recommendedPlan, reminders, imageBrief);
    }

    /**
     * 构造带会话编号的完整旅行计划响应。
     *
     * @param sessionId 会话编号
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
            String sessionId,
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
                sessionId,
                false,
                List.of(),
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
                null,
                List.of(),
                OffsetDateTime.now()
        );
    }

    /**
     * 复制当前响应并写入多轮意图和快捷提示。
     *
     * @param dialogIntent 本轮多轮对话意图
     * @param contextualSuggestions 上下文快捷提示列表
     * @return 带交互辅助信息的旅行计划响应
     */
    public TravelPlanResponse withInteraction(TravelDialogIntent dialogIntent, List<String> contextualSuggestions) {
        return new TravelPlanResponse(
                planId,
                sessionId,
                needClarification,
                clarificationQuestions,
                structuredClarificationQuestions,
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
                dialogIntent,
                contextualSuggestions,
                createdAt
        );
    }

    /**
     * 复制当前响应并写入计划编号。
     *
     * @param planId 全局唯一计划编号
     * @return 带计划编号的旅行计划响应
     */
    public TravelPlanResponse withPlanId(String planId) {
        return new TravelPlanResponse(
                planId,
                sessionId,
                needClarification,
                clarificationQuestions,
                structuredClarificationQuestions,
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
                dialogIntent,
                contextualSuggestions,
                createdAt
        );
    }

    /**
     * 获取最高分方案。
     *
     * @param scoredPlans 已评分候选方案列表
     * @return 最高分方案，不存在时返回 null
     */
    private static ScoredTravelPlan firstScoredPlan(List<ScoredTravelPlan> scoredPlans) {
        return scoredPlans == null || scoredPlans.isEmpty() ? null : scoredPlans.get(0);
    }
}
