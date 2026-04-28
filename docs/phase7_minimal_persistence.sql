CREATE TABLE IF NOT EXISTS travel_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id VARCHAR(64) NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_travel_plan_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS travel_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id VARCHAR(64) NOT NULL,
    task_index INT NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    query_text VARCHAR(1000) NULL,
    city VARCHAR(128) NULL,
    priority INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_name VARCHAR(128) NULL,
    error_message VARCHAR(1000) NULL,
    task_json LONGTEXT NULL,
    tool_result_json LONGTEXT NULL,
    created_at DATETIME NOT NULL,
    KEY idx_travel_task_plan_id (plan_id),
    CONSTRAINT fk_travel_task_plan_id FOREIGN KEY (plan_id) REFERENCES travel_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS travel_evidence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id VARCHAR(64) NOT NULL,
    evidence_index INT NOT NULL,
    evidence_type VARCHAR(32) NOT NULL,
    city VARCHAR(128) NULL,
    title VARCHAR(255) NULL,
    summary TEXT NULL,
    confidence DECIMAL(5, 4) NOT NULL,
    source_name VARCHAR(128) NULL,
    source_url VARCHAR(1000) NULL,
    key_facts_json LONGTEXT NULL,
    evidence_json LONGTEXT NULL,
    fetched_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    KEY idx_travel_evidence_plan_id (plan_id),
    CONSTRAINT fk_travel_evidence_plan_id FOREIGN KEY (plan_id) REFERENCES travel_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS travel_reminder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id VARCHAR(64) NOT NULL,
    reminder_index INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    reminder_type VARCHAR(32) NOT NULL,
    remind_rule VARCHAR(255) NULL,
    description VARCHAR(1000) NULL,
    reminder_json LONGTEXT NULL,
    created_at DATETIME NOT NULL,
    KEY idx_travel_reminder_plan_id (plan_id),
    CONSTRAINT fk_travel_reminder_plan_id FOREIGN KEY (plan_id) REFERENCES travel_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
