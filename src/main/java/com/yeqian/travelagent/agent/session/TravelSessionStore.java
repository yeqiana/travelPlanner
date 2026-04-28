package com.yeqian.travelagent.agent.session;

import com.yeqian.travelagent.domain.model.ClarificationQuestion;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelSessionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 旅行规划会话仓库。
 *
 * <p>Batch 1 使用内存保存多轮追问上下文，后续如需多实例或持久化可替换为数据库/缓存实现。</p>
 */
@Component
public class TravelSessionStore {

    private final Map<String, TravelSessionContext> sessions = new ConcurrentHashMap<>();

    /**
     * 根据会话编号查询上下文。
     *
     * @param sessionId 会话编号
     * @return 会话上下文，不存在时返回 null
     */
    public TravelSessionContext findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return sessions.get(sessionId);
    }

    /**
     * 创建或更新澄清态会话上下文。
     *
     * @param sessionId 请求携带的会话编号，可为空
     * @param intent 当前已解析出的部分旅行意图
     * @param questions 当前结构化澄清问题
     * @return 保存后的会话上下文
     */
    public TravelSessionContext saveClarifying(String sessionId, TravelIntent intent, List<ClarificationQuestion> questions) {
        TravelSessionContext existingContext = findBySessionId(sessionId);
        if (existingContext == null) {
            String normalizedSessionId = hasText(sessionId) ? sessionId : newSessionId();
            TravelSessionContext createdContext = new TravelSessionContext(
                    normalizedSessionId,
                    intent,
                    questions,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    "CLARIFYING"
            );
            sessions.put(normalizedSessionId, createdContext);
            return createdContext;
        }

        TravelSessionContext updatedContext = existingContext.withClarifyingIntent(intent, questions);
        sessions.put(updatedContext.sessionId(), updatedContext);
        return updatedContext;
    }

    /**
     * 将会话标记为已完成。
     *
     * @param sessionId 会话编号
     * @param intent 完整旅行意图
     */
    public void markCompleted(String sessionId, TravelIntent intent) {
        TravelSessionContext existingContext = findBySessionId(sessionId);
        if (existingContext != null) {
            sessions.put(existingContext.sessionId(), existingContext.withCompletedIntent(intent));
        }
    }

    /**
     * 生成新的会话编号。
     *
     * @return 会话编号
     */
    private String newSessionId() {
        return "session_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 判断文本是否包含有效内容。
     *
     * @param value 待判断文本
     * @return 包含有效内容时返回 true
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
