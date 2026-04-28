package com.yeqian.travelagent.agent.orchestrator;

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
import com.yeqian.travelagent.domain.model.ImageBrief;
import com.yeqian.travelagent.domain.model.MissingInfoCheckResult;
import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelReminder;
import com.yeqian.travelagent.domain.model.TravelTask;
import com.yeqian.travelagent.tool.ToolExecutor;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 旅行 Agent 编排器。
 *
 * <p>负责串联需求解析、缺失信息判断和查询任务拆解流程。</p>
 */
@Component
public class TravelAgentOrchestrator {

    @Resource
    private TravelIntentParser travelIntentParser;

    @Resource
    private MissingInfoChecker missingInfoChecker;

    @Resource
    private TravelTaskPlanner travelTaskPlanner;

    @Resource
    private ToolExecutor toolExecutor;

    @Resource
    private EvidenceNormalizer evidenceNormalizer;

    @Resource
    private CandidatePlanGenerator candidatePlanGenerator;

    @Resource
    private TravelScorer travelScorer;

    @Resource
    private ItineraryPlanner itineraryPlanner;

    @Resource
    private ReminderGenerator reminderGenerator;

    @Resource
    private ImageBriefGenerator imageBriefGenerator;

    /**
     * 执行旅行规划编排流程。
     *
     * @param request 旅行计划请求
     * @return 旅行计划响应
     */
    public TravelPlanResponse plan(TravelPlanRequest request) {
        TravelIntent intent = travelIntentParser.parse(request.message());
        MissingInfoCheckResult checkResult = missingInfoChecker.check(intent);
        if (checkResult.needClarification()) {
            return TravelPlanResponse.needClarification(checkResult.clarificationQuestions(), intent);
        }
        List<TravelTask> tasks = travelTaskPlanner.plan(intent);
        List<ToolResult> toolResults = toolExecutor.execute(tasks);
        List<TravelEvidence> evidences = evidenceNormalizer.normalize(toolResults, tasks);
        List<TravelCandidatePlan> candidatePlans = candidatePlanGenerator.generate(intent, evidences);
        List<ScoredTravelPlan> scoredPlans = travelScorer.score(candidatePlans, intent, evidences);
        TravelPlan recommendedPlan = itineraryPlanner.generate(intent, scoredPlans, evidences);
        List<TravelReminder> reminders = reminderGenerator.generate(intent, recommendedPlan);
        ImageBrief imageBrief = imageBriefGenerator.generate(recommendedPlan);
        return TravelPlanResponse.completed(intent, tasks, toolResults, evidences, candidatePlans, scoredPlans, recommendedPlan, reminders, imageBrief);
    }
}
