USE travel_planner_dev;

ALTER TABLE travel_session
    ADD COLUMN last_response_json LONGTEXT NULL COMMENT '上一轮完整旅行规划响应上下文JSON' AFTER last_questions_json;
