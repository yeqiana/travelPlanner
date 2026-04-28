package com.yeqian.travelagent.agent.normalizer;

import com.yeqian.travelagent.domain.enums.EvidenceType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelTask;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 证据归一化器。
 *
 * <p>把工具调用结果转换为统一证据结构，并保留来源、时间和二次确认提示。</p>
 */
@Component
public class EvidenceNormalizer {

    /**
     * 批量归一化工具结果。
     *
     * @param toolResults 工具调用结果列表
     * @return 旅行证据列表
     */
    public List<TravelEvidence> normalize(List<ToolResult> toolResults) {
        return normalize(toolResults, List.of());
    }

    /**
     * 按查询任务批量归一化工具结果。
     *
     * @param toolResults 工具调用结果列表
     * @param tasks 查询任务列表
     * @return 旅行证据列表
     */
    public List<TravelEvidence> normalize(List<ToolResult> toolResults, List<TravelTask> tasks) {
        if (toolResults == null || toolResults.isEmpty()) {
            return List.of();
        }
        return IntStream.range(0, toolResults.size())
                .mapToObj(index -> normalizeOne(toolResults.get(index), taskAt(tasks, index)))
                .toList();
    }

    /**
     * 归一化单个工具结果。
     *
     * @param result 工具调用结果
     * @param task 查询任务
     * @return 旅行证据
     */
    private TravelEvidence normalizeOne(ToolResult result, TravelTask task) {
        EvidenceType evidenceType = EvidenceType.valueOf(result.taskType().name());
        boolean needSecondConfirm = !result.success() || result.rawContent().contains("二次确认");
        String city = task == null ? null : task.city();
        return new TravelEvidence(
                evidenceType,
                city,
                buildTitle(evidenceType, city),
                result.success() ? result.rawContent() : result.errorMessage(),
                Map.of(
                        "needSecondConfirm", needSecondConfirm,
                        "success", result.success(),
                        "query", task == null ? "" : task.query()
                ),
                result.success() ? 0.75 : 0.2,
                result.source(),
                null,
                result.fetchedAt()
        );
    }

    /**
     * 按下标获取查询任务。
     *
     * @param tasks 查询任务列表
     * @param index 下标
     * @return 查询任务，缺失时返回 null
     */
    private TravelTask taskAt(List<TravelTask> tasks, int index) {
        if (tasks == null || index >= tasks.size()) {
            return null;
        }
        return tasks.get(index);
    }

    /**
     * 生成证据标题。
     *
     * @param evidenceType 证据类型
     * @param city 关联城市
     * @return 证据标题
     */
    private String buildTitle(EvidenceType evidenceType, String city) {
        String prefix = city == null || city.isBlank() ? "" : city + " ";
        return switch (evidenceType) {
            case WEATHER -> prefix + "天气证据";
            case TRANSPORT -> prefix + "交通证据";
            case HOTEL -> prefix + "住宿证据";
            case ATTRACTION -> prefix + "景点证据";
            case ROUTE -> prefix + "路线证据";
            case GENERAL_WEB -> prefix + "通用网页证据";
        };
    }
}
