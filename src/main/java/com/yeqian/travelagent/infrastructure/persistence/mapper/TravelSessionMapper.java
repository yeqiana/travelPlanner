package com.yeqian.travelagent.infrastructure.persistence.mapper;

import com.yeqian.travelagent.infrastructure.persistence.entity.TravelSessionEntity;
import jakarta.annotation.Resource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 旅行会话 Mapper。
 *
 * <p>负责 travel_session 表的新增、更新、状态变更和按会话编号查询。</p>
 */
@Repository
public class TravelSessionMapper {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 插入旅行会话记录。
     *
     * @param entity 旅行会话实体
     */
    public void insert(TravelSessionEntity entity) {
        jdbcTemplate.update(
                """
                        INSERT INTO travel_session
                        (session_id, status, partial_intent_json, last_questions_json, expires_at, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                entity.getSessionId(),
                entity.getStatus(),
                entity.getPartialIntentJson(),
                entity.getLastQuestionsJson(),
                toTimestamp(entity.getExpiresAt()),
                toTimestamp(entity.getCreatedAt()),
                toTimestamp(entity.getUpdatedAt())
        );
    }

    /**
     * 更新旅行会话追问上下文。
     *
     * @param entity 旅行会话实体
     */
    public void updateClarifying(TravelSessionEntity entity) {
        jdbcTemplate.update(
                """
                        UPDATE travel_session
                        SET status = ?, partial_intent_json = ?, last_questions_json = ?, expires_at = ?, updated_at = ?
                        WHERE session_id = ?
                        """,
                entity.getStatus(),
                entity.getPartialIntentJson(),
                entity.getLastQuestionsJson(),
                toTimestamp(entity.getExpiresAt()),
                toTimestamp(entity.getUpdatedAt()),
                entity.getSessionId()
        );
    }

    /**
     * 标记旅行会话为已完成。
     *
     * @param sessionId 会话编号
     * @param partialIntentJson 完成时的意图 JSON
     * @param updatedAt 更新时间
     */
    public void markCompleted(String sessionId, String partialIntentJson, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                """
                        UPDATE travel_session
                        SET status = 'COMPLETED', partial_intent_json = ?, last_questions_json = '[]', updated_at = ?
                        WHERE session_id = ?
                        """,
                partialIntentJson,
                toTimestamp(updatedAt),
                sessionId
        );
    }

    /**
     * 按会话编号查询旅行会话。
     *
     * @param sessionId 会话编号
     * @return 旅行会话实体，不存在时为空
     */
    public Optional<TravelSessionEntity> findBySessionId(String sessionId) {
        try {
            TravelSessionEntity entity = jdbcTemplate.queryForObject(
                    """
                            SELECT session_id, status, partial_intent_json, last_questions_json,
                                   expires_at, created_at, updated_at
                            FROM travel_session
                            WHERE session_id = ?
                            """,
                    (rs, rowNum) -> {
                        TravelSessionEntity session = new TravelSessionEntity();
                        session.setSessionId(rs.getString("session_id"));
                        session.setStatus(rs.getString("status"));
                        session.setPartialIntentJson(rs.getString("partial_intent_json"));
                        session.setLastQuestionsJson(rs.getString("last_questions_json"));
                        session.setExpiresAt(toLocalDateTime(rs.getTimestamp("expires_at")));
                        session.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                        session.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
                        return session;
                    },
                    sessionId
            );
            return Optional.ofNullable(entity);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    /**
     * 转换本地时间为数据库时间戳。
     *
     * @param value 本地时间
     * @return 数据库时间戳
     */
    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    /**
     * 转换数据库时间戳为本地时间。
     *
     * @param value 数据库时间戳
     * @return 本地时间
     */
    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
