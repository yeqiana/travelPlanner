CREATE DATABASE IF NOT EXISTS travel_planner_dev DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE travel_planner_dev;

CREATE TABLE IF NOT EXISTS travel_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    session_id VARCHAR(64) NOT NULL COMMENT '多轮追问会话编号',
    status VARCHAR(32) NOT NULL COMMENT '会话状态：CLARIFYING 或 COMPLETED',
    partial_intent_json LONGTEXT NULL COMMENT '已解析出的部分旅行意图JSON',
    last_questions_json LONGTEXT NULL COMMENT '上一轮结构化追问问题JSON',
    expires_at DATETIME NOT NULL COMMENT '会话过期时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_travel_session_session_id (session_id),
    KEY idx_travel_session_status_expires_at (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旅行规划多轮追问会话表';

CREATE TABLE IF NOT EXISTS travel_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    plan_id VARCHAR(64) NOT NULL COMMENT '旅行规划业务编号',
    response_json LONGTEXT NOT NULL COMMENT '完整旅行规划响应JSON',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_travel_plan_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旅行规划主表';

CREATE TABLE IF NOT EXISTS travel_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    plan_id VARCHAR(64) NOT NULL COMMENT '旅行规划业务编号',
    task_index INT NOT NULL COMMENT '任务在规划中的顺序',
    task_type VARCHAR(32) NOT NULL COMMENT '任务类型',
    query_text VARCHAR(1000) NULL COMMENT '任务查询文本',
    city VARCHAR(128) NULL COMMENT '任务关联城市',
    priority INT NOT NULL COMMENT '任务优先级',
    status VARCHAR(32) NOT NULL COMMENT '任务状态',
    source_name VARCHAR(128) NULL COMMENT '数据来源名称',
    error_message VARCHAR(1000) NULL COMMENT '任务执行错误信息',
    task_json LONGTEXT NULL COMMENT '任务原始JSON',
    tool_result_json LONGTEXT NULL COMMENT '工具执行结果JSON',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_travel_task_plan_id (plan_id),
    CONSTRAINT fk_travel_task_plan_id FOREIGN KEY (plan_id) REFERENCES travel_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旅行规划任务表';

CREATE TABLE IF NOT EXISTS travel_evidence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    plan_id VARCHAR(64) NOT NULL COMMENT '旅行规划业务编号',
    evidence_index INT NOT NULL COMMENT '证据在规划中的顺序',
    evidence_type VARCHAR(32) NOT NULL COMMENT '证据类型',
    city VARCHAR(128) NULL COMMENT '证据关联城市',
    title VARCHAR(255) NULL COMMENT '证据标题',
    summary TEXT NULL COMMENT '证据摘要',
    confidence DECIMAL(5, 4) NOT NULL COMMENT '证据置信度',
    source_name VARCHAR(128) NULL COMMENT '数据来源名称',
    source_url VARCHAR(1000) NULL COMMENT '数据来源链接',
    key_facts_json LONGTEXT NULL COMMENT '关键事实JSON',
    evidence_json LONGTEXT NULL COMMENT '证据原始JSON',
    fetched_at DATETIME NULL COMMENT '证据抓取时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_travel_evidence_plan_id (plan_id),
    CONSTRAINT fk_travel_evidence_plan_id FOREIGN KEY (plan_id) REFERENCES travel_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旅行规划证据表';

CREATE TABLE IF NOT EXISTS travel_reminder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    plan_id VARCHAR(64) NOT NULL COMMENT '旅行规划业务编号',
    reminder_index INT NOT NULL COMMENT '提醒在规划中的顺序',
    title VARCHAR(255) NOT NULL COMMENT '提醒标题',
    reminder_type VARCHAR(32) NOT NULL COMMENT '提醒类型',
    remind_rule VARCHAR(255) NULL COMMENT '提醒规则',
    description VARCHAR(1000) NULL COMMENT '提醒描述',
    reminder_json LONGTEXT NULL COMMENT '提醒原始JSON',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_travel_reminder_plan_id (plan_id),
    CONSTRAINT fk_travel_reminder_plan_id FOREIGN KEY (plan_id) REFERENCES travel_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旅行规划提醒表';
