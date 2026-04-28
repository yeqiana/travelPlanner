package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * mock 交通查询工具。
 *
 * <p>用于在不接入真实票务 API 的情况下返回城市间交通建议。</p>
 */
@Component
public class MockTransportSearchTool implements TravelTool {

    /**
     * 判断是否支持交通任务。
     *
     * @param taskType 任务类型
     * @return 支持交通任务返回 true，否则返回 false
     */
    @Override
    public boolean supports(TravelTaskType taskType) {
        return TravelTaskType.TRANSPORT == taskType;
    }

    /**
     * 执行 mock 交通查询。
     *
     * @param task 旅行查询任务
     * @return 工具调用结果
     */
    @Override
    public ToolResult execute(TravelTask task) {
        return new ToolResult(task.taskType(), source(), true,
                "mock 交通建议：" + task.query() + "。优先比较高铁、动车和低价航班，具体车次、余票和价格需二次确认。",
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
        return "MockTransportSearchTool";
    }
}
