package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * mock 景点信息工具。
 *
 * <p>用于在不接入真实景区 API 的情况下返回景点预约和开放时间提示。</p>
 */
@Component
public class MockAttractionInfoTool implements TravelTool {

    /**
     * 判断是否支持景点任务。
     *
     * @param taskType 任务类型
     * @return 支持景点任务返回 true，否则返回 false
     */
    @Override
    public boolean supports(TravelTaskType taskType) {
        return TravelTaskType.ATTRACTION == taskType;
    }

    /**
     * 执行 mock 景点信息查询。
     *
     * @param task 旅行查询任务
     * @return 工具调用结果
     */
    @Override
    public ToolResult execute(TravelTask task) {
        String city = task.city() == null ? "目的地" : task.city();
        return new ToolResult(task.taskType(), source(), true,
                city + " mock 景点提示：热门景点节假日通常需要提前预约，门票、名额和开放时间需二次确认。",
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
        return "MockAttractionInfoTool";
    }
}
