-- 番茄钟专注模块迁移脚本
-- focus_session: 独立专注会话表，记录每次番茄钟专注
-- sys_config 新增配置项：专注相关可配置规则

CREATE TABLE IF NOT EXISTS focus_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT DEFAULT NULL COMMENT '关联书籍ID（可选）',
    duration INT NOT NULL COMMENT '计划专注时长(秒)',
    actual_duration INT DEFAULT 0 COMMENT '实际专注时长(秒)',
    status TINYINT DEFAULT 0 COMMENT '0-进行中 1-已完成 2-已放弃',
    started_at DATETIME NOT NULL COMMENT '开始时间',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    pomodoro_index INT DEFAULT 1 COMMENT '当日第几个番茄钟',
    tag VARCHAR(50) DEFAULT '' COMMENT '专注标签（阅读/学习/工作等）',
    note VARCHAR(500) DEFAULT '' COMMENT '专注备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_status (status),
    INDEX idx_started_at (started_at),
    INDEX idx_user_date (user_id, DATE(started_at))
) ENGINE=InnoDB COMMENT='专注会话表';

-- 番茄钟与阅读目标联动配置项（插入 sys_config）
INSERT INTO sys_config (config_key, config_value, config_type, description, category, is_editable)
VALUES
    ('focus.pomodoro.default_minutes', '25', 'number', '默认番茄钟时长(分钟)', 'focus', '1'),
    ('focus.pomodoro.short_break', '5', 'number', '短休息时长(分钟)', 'focus', '1'),
    ('focus.pomodoro.long_break', '15', 'number', '长休息时长(分钟)', 'focus', '1'),
    ('focus.pomodoro.long_break_interval', '4', 'number', '每多少个番茄后长休息', 'focus', '1'),
    ('focus.goal.pomodoro_count_half', '4', 'number', '完成多少个番茄算当日目标一半', 'focus', '1'),
    ('focus.goal.enabled', 'true', 'boolean', '是否启用番茄钟与阅读目标联动', 'focus', '1'),
    ('focus.goal.ratio', '0.5', 'number', '番茄钟时长占阅读目标进度的比例', 'focus', '1'),
    ('focus.min_valid_duration', '60', 'number', '最小有效专注时长(秒)，低于此不计入', 'focus', '1')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    config_type = VALUES(config_type),
    description = VALUES(description),
    category = VALUES(category),
    is_editable = VALUES(is_editable);
