package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 工具执行器。
 *
 * <p>根据任务类型分发到对应工具；单个工具失败不应中断整个旅行规划流程。</p>
 */
@Component
public class ToolExecutor {

    @Resource
    private List<TravelTool> travelTools;

    /**
     * 批量执行旅行查询任务。
     *
     * @param tasks 查询任务列表
     * @return 工具调用结果列表
     */
    public List<ToolResult> execute(List<TravelTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream().map(this::executeOne).toList();
    }

    /**
     * 执行单个查询任务。
     *
     * @param task 查询任务
     * @return 工具调用结果
     */
    private ToolResult executeOne(TravelTask task) {
        Optional<TravelTool> toolOptional = travelTools.stream()
                .filter(tool -> tool.supports(task.taskType()))
                .findFirst();
        if (toolOptional.isEmpty()) {
            return failure(task, "ToolExecutor", "未找到匹配的旅行工具：" + task.taskType());
        }

        TravelTool tool = toolOptional.get();
        try {
            return tool.execute(task);
        } catch (Exception exception) {
            return failure(task, tool.source(), exception.getMessage());
        }
    }

    /**
     * 构造失败工具结果。
     *
     * @param task 查询任务
     * @param source 工具来源
     * @param errorMessage 错误信息
     * @return 失败工具结果
     */
    private ToolResult failure(TravelTask task, String source, String errorMessage) {
        return new ToolResult(task.taskType(), source, false, "", errorMessage, OffsetDateTime.now());
    }
}
