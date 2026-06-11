-- 阅读统计深化功能迁移脚本

-- 1. 用户阅读目标设置表
CREATE TABLE IF NOT EXISTS reading_goal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    daily_goal_minutes INT NOT NULL DEFAULT 30 COMMENT '每日阅读目标时长(分钟)',
    weekly_goal_minutes INT NOT NULL DEFAULT 210 COMMENT '每周阅读目标时长(分钟)',
    goal_type TINYINT DEFAULT 1 COMMENT '1-每日目标 2-每周目标 3-两者都有',
    remind_enabled TINYINT DEFAULT 0 COMMENT '是否开启阅读提醒 0-关闭 1-开启',
    remind_time VARCHAR(5) DEFAULT '' COMMENT '提醒时间(HH:mm)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='用户阅读目标设置表';

-- 2. 阅读报告表
CREATE TABLE IF NOT EXISTS reading_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    report_type VARCHAR(20) NOT NULL COMMENT '报告类型 weekly/monthly/yearly/custom',
    period_start DATE NOT NULL COMMENT '报告周期开始日期',
    period_end DATE NOT NULL COMMENT '报告周期结束日期',
    total_duration BIGINT DEFAULT 0 COMMENT '总阅读时长(秒)',
    book_count INT DEFAULT 0 COMMENT '读完书籍数',
    annotation_count INT DEFAULT 0 COMMENT '批注数',
    max_streak_days INT DEFAULT 0 COMMENT '最长连续阅读天数',
    reading_days INT DEFAULT 0 COMMENT '阅读天数',
    report_data TEXT COMMENT '报告详细数据(JSON)',
    status TINYINT DEFAULT 1 COMMENT '0-生成中 1-已完成 2-失败',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='阅读报告表';

-- 3. 给user表添加连续阅读天数字段
ALTER TABLE user ADD COLUMN IF NOT EXISTS current_streak_days INT DEFAULT 0 COMMENT '当前连续阅读天数' AFTER status;
ALTER TABLE user ADD COLUMN IF NOT EXISTS max_streak_days INT DEFAULT 0 COMMENT '最长连续阅读天数' AFTER current_streak_days;
ALTER TABLE user ADD COLUMN IF NOT EXISTS last_read_date DATE DEFAULT NULL COMMENT '最后阅读日期' AFTER max_streak_days;

-- 4. 管理端阅读行为分析视图/统计
-- 初始化默认阅读目标
INSERT INTO reading_goal (user_id, daily_goal_minutes, weekly_goal_minutes, goal_type)
SELECT id, 30, 210, 1 FROM user
ON DUPLICATE KEY UPDATE daily_goal_minutes = VALUES(daily_goal_minutes);
