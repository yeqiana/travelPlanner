package com.yeqian.travelagent.infrastructure.persistence.mapper;

import com.yeqian.travelagent.infrastructure.persistence.entity.TravelEvidenceEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 旅行证据 Mapper。
 *
 * <p>负责 travel_evidence 表的批量新增。</p>
 */
@Repository
public class TravelEvidenceMapper {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 批量插入旅行证据记录。
     *
     * @param entities 旅行证据实体列表
     */
    public void batchInsert(List<TravelEvidenceEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO travel_evidence (
                            plan_id, evidence_index, evidence_type, city, title, summary,
                            confidence, source_name, source_url, key_facts_json,
                            evidence_json, fetched_at, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                entities,
                entities.size(),
                (ps, entity) -> {
                    ps.setString(1, entity.getPlanId());
                    ps.setInt(2, entity.getEvidenceIndex());
                    ps.setString(3, entity.getEvidenceType());
                    ps.setString(4, entity.getCity());
                    ps.setString(5, entity.getTitle());
                    ps.setString(6, entity.getSummary());
                    ps.setDouble(7, entity.getConfidence());
                    ps.setString(8, entity.getSourceName());
                    ps.setString(9, entity.getSourceUrl());
                    ps.setString(10, entity.getKeyFactsJson());
                    ps.setString(11, entity.getEvidenceJson());
                    ps.setTimestamp(12, toTimestamp(entity.getFetchedAt()));
                    ps.setTimestamp(13, toTimestamp(entity.getCreatedAt()));
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
