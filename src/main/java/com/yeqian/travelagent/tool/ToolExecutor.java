package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
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

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

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
            log.info("工具执行跳过：无查询任务");
            return List.of();
        }
        log.info("工具批量执行开始：taskCount={}", tasks.size());
        return tasks.stream().map(this::executeOne).toList();
    }

    /**
     * 执行单个查询任务。
     *
     * @param task 查询任务
     * @return 工具调用结果
     */
    private ToolResult executeOne(TravelTask task) {
        List<TravelTool> orderedTools = travelTools.stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .toList();
        Optional<TravelTool> toolOptional = orderedTools.stream()
                .filter(tool -> tool.supports(task.taskType()))
                .findFirst();
        if (toolOptional.isEmpty()) {
            log.warn("工具匹配失败：taskType={}, city={}, query={}", task.taskType(), task.city(), task.query());
            return failure(task, "ToolExecutor", "未找到匹配的旅行工具：" + task.taskType());
        }

        TravelTool tool = toolOptional.get();
        long startMillis = System.currentTimeMillis();
        try {
            log.info("工具执行开始：source={}, taskType={}, city={}, query={}", tool.source(), task.taskType(), task.city(), task.query());
            ToolResult result = tool.execute(task);
            log.info("工具执行结束：source={}, taskType={}, city={}, success={}, costMillis={}, error={}",
                    tool.source(),
                    task.taskType(),
                    task.city(),
                    result.success(),
                    System.currentTimeMillis() - startMillis,
                    result.errorMessage());
            return result;
        } catch (Exception exception) {
            log.warn("工具执行异常：source={}, taskType={}, city={}, costMillis={}, error={}",
                    tool.source(),
                    task.taskType(),
                    task.city(),
                    System.currentTimeMillis() - startMillis,
                    exception.getMessage(),
                    exception);
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
