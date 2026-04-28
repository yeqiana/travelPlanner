package com.yeqian.travelagent.infrastructure.persistence.mapper;

import com.yeqian.travelagent.infrastructure.persistence.entity.TravelReminderEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 旅行提醒 Mapper。
 *
 * <p>负责 travel_reminder 表的批量新增。</p>
 */
@Repository
public class TravelReminderMapper {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 批量插入旅行提醒记录。
     *
     * @param entities 旅行提醒实体列表
     */
    public void batchInsert(List<TravelReminderEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO travel_reminder (
                            plan_id, reminder_index, title, reminder_type,
                            remind_rule, description, reminder_json, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                entities,
                entities.size(),
                (ps, entity) -> {
                    ps.setString(1, entity.getPlanId());
                    ps.setInt(2, entity.getReminderIndex());
                    ps.setString(3, entity.getTitle());
                    ps.setString(4, entity.getReminderType());
                    ps.setString(5, entity.getRemindRule());
                    ps.setString(6, entity.getDescription());
                    ps.setString(7, entity.getReminderJson());
                    ps.setTimestamp(8, toTimestamp(entity.getCreatedAt()));
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
