package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import com.yeqian.travelagent.infrastructure.client.SearchApiClient;
import jakarta.annotation.Resource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * 真实 Web 搜索工具。
 *
 * <p>优先调用真实搜索 API；调用失败时降级到 mock 搜索工具，避免中断完整规划流程。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSearchTool implements TravelTool {

    @Resource
    private SearchApiClient searchApiClient;

    @Resource
    private MockWebSearchTool mockWebSearchTool;

    /**
     * 判断是否支持通用网页搜索任务。
     *
     * @param taskType 任务类型
     * @return 支持通用网页搜索任务返回 true，否则返回 false
     */
    @Override
    public boolean supports(TravelTaskType taskType) {
        return TravelTaskType.GENERAL_WEB == taskType;
    }

    /**
     * 执行真实网页搜索，失败时返回 mock 降级结果。
     *
     * @param task 旅行查询任务
     * @return 工具调用结果
     */
    @Override
    public ToolResult execute(TravelTask task) {
        try {
            String query = task.query() == null || task.query().isBlank() ? "旅行信息" : task.query();
            String rawContent = searchApiClient.search(query);
            return new ToolResult(task.taskType(), source(), true, rawContent, null, OffsetDateTime.now());
        } catch (Exception exception) {
            ToolResult fallback = mockWebSearchTool.execute(task);
            return new ToolResult(
                    task.taskType(),
                    source() + " -> " + fallback.source(),
                    true,
                    fallback.rawContent(),
                    "真实搜索 API 调用失败，已降级到 mock：" + exception.getMessage(),
                    OffsetDateTime.now()
            );
        }
    }

    /**
     * 获取工具来源名称。
     *
     * @return 工具来源名称
     */
    @Override
    public String source() {
        return "WebSearchTool";
    }
}
