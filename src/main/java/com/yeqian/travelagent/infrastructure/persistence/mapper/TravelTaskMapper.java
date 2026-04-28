package com.yeqian.travelagent.infrastructure.persistence.mapper;

import com.yeqian.travelagent.infrastructure.persistence.entity.TravelTaskEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 旅行任务 Mapper。
 *
 * <p>负责 travel_task 表的批量新增。</p>
 */
@Repository
public class TravelTaskMapper {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 批量插入旅行任务记录。
     *
     * @param entities 旅行任务实体列表
     */
    public void batchInsert(List<TravelTaskEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO travel_task (
                            plan_id, task_index, task_type, query_text, city, priority,
                            status, source_name, error_message, task_json, tool_result_json, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                entities,
                entities.size(),
                (ps, entity) -> {
                    ps.setString(1, entity.getPlanId());
                    ps.setInt(2, entity.getTaskIndex());
                    ps.setString(3, entity.getTaskType());
                    ps.setString(4, entity.getQueryText());
                    ps.setString(5, entity.getCity());
                    ps.setInt(6, entity.getPriority());
                    ps.setString(7, entity.getStatus());
                    ps.setString(8, entity.getSourceName());
                    ps.setString(9, entity.getErrorMessage());
                    ps.setString(10, entity.getTaskJson());
                    ps.setString(11, entity.getToolResultJson());
                    ps.setTimestamp(12, toTimestamp(entity.getCreatedAt()));
                }
        );
    }

    /**
     * 转换时间为数据库时间戳。
     *
     * @param value 偏移时间
     * @return 数据库时间戳
     */
    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }
}
