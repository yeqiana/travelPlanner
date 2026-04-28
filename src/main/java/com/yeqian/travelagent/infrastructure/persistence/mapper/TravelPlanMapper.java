package com.yeqian.travelagent.infrastructure.persistence.mapper;

import com.yeqian.travelagent.infrastructure.persistence.entity.TravelPlanEntity;
import jakarta.annotation.Resource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 旅行计划 Mapper。
 *
 * <p>负责 travel_plan 表的新增和按计划编号查询。</p>
 */
@Repository
public class TravelPlanMapper {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 插入旅行计划主记录。
     *
     * @param entity 旅行计划实体
     */
    public void insert(TravelPlanEntity entity) {
        jdbcTemplate.update(
                """
                        INSERT INTO travel_plan (plan_id, response_json, created_at, updated_at)
                        VALUES (?, ?, ?, ?)
                        """,
                entity.getPlanId(),
                entity.getResponseJson(),
                toTimestamp(entity.getCreatedAt()),
                toTimestamp(entity.getUpdatedAt())
        );
    }

    /**
     * 按计划编号查询旅行计划主记录。
     *
     * @param planId 计划编号
     * @return 旅行计划实体，不存在时为空
     */
    public Optional<TravelPlanEntity> findByPlanId(String planId) {
        try {
            TravelPlanEntity entity = jdbcTemplate.queryForObject(
                    """
                            SELECT plan_id, response_json, created_at, updated_at
                            FROM travel_plan
                            WHERE plan_id = ?
                            """,
                    (rs, rowNum) -> {
                        TravelPlanEntity plan = new TravelPlanEntity();
                        plan.setPlanId(rs.getString("plan_id"));
                        plan.setResponseJson(rs.getString("response_json"));
                        plan.setCreatedAt(toOffsetDateTime(rs.getTimestamp("created_at")));
                        plan.setUpdatedAt(toOffsetDateTime(rs.getTimestamp("updated_at")));
                        return plan;
                    },
                    planId
            );
            return Optional.ofNullable(entity);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
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

    /**
     * 转换数据库时间戳为偏移时间。
     *
     * @param value 数据库时间戳
     * @return 偏移时间
     */
    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(OffsetDateTime.now().getOffset());
    }
}
