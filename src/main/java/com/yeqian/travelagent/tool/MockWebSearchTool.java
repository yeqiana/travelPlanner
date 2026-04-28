package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * mock 网页搜索工具。
 *
 * <p>用于在不接入真实搜索 API 的情况下返回通用旅行搜索摘要。</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class MockWebSearchTool implements TravelTool {

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
     * 执行 mock 网页搜索。
     *
     * @param task 旅行查询任务
     * @return 工具调用结果
     */
    @Override
    public ToolResult execute(TravelTask task) {
        return new ToolResult(task.taskType(), source(), true,
                "mock 搜索摘要：" + task.query() + "。实时政策、门票和营业信息需二次确认。",
                null,
                OffsetDateTime.now());
    }

    /**
     * 获取工具来源名称。
     *
     * @return 工具来源名称
     */
    @Override
    public String source() {
        return "MockWebSearchTool";
    }
}
