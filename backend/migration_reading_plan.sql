SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS reading_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    target_days INT NOT NULL DEFAULT 30 COMMENT '目标天数',
    daily_min_duration INT NOT NULL DEFAULT 600 COMMENT '每日最低阅读时长(秒)',
    reminder_time VARCHAR(5) DEFAULT '' COMMENT '提醒时间(HH:mm)',
    read_pages INT NOT NULL DEFAULT 0 COMMENT '已读页数',
    total_pages INT NOT NULL DEFAULT 0 COMMENT '总页数',
    streak_days INT NOT NULL DEFAULT 0 COMMENT '连续打卡天数',
    max_streak_days INT NOT NULL DEFAULT 0 COMMENT '最长连续打卡天数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-已放弃 1-进行中 2-已完成',
    start_date DATE NOT NULL COMMENT '计划开始日期',
    end_date DATE DEFAULT NULL COMMENT '计划结束日期(预计或实际)',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_status (status),
    INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB COMMENT='阅读计划表';

CREATE TABLE IF NOT EXISTS reading_plan_checkin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL COMMENT '计划ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    checkin_date DATE NOT NULL COMMENT '打卡日期',
    duration INT NOT NULL DEFAULT 0 COMMENT '当日阅读时长(秒)',
    pages_read INT NOT NULL DEFAULT 0 COMMENT '当日阅读页数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plan_date (plan_id, checkin_date),
    INDEX idx_plan_id (plan_id),
    INDEX idx_user_date (user_id, checkin_date)
) ENGINE=InnoDB COMMENT='阅读计划打卡记录表';

CREATE TABLE IF NOT EXISTS reading_plan_badge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    plan_id BIGINT DEFAULT NULL COMMENT '关联计划ID(NULL为全局徽章)',
    badge_type VARCHAR(50) NOT NULL COMMENT '徽章类型(streak_3/streak_7/streak_14/streak_30/streak_100/plan_complete)',
    badge_name VARCHAR(100) NOT NULL COMMENT '徽章名称',
    badge_icon VARCHAR(200) DEFAULT '' COMMENT '徽章图标',
    earned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_badge (user_id, badge_type, plan_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='阅读计划徽章表';

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(110, 'reading_plan_mgmt', '阅读计划管理', 1, NULL, '/reading-plans', 12),
(111, 'reading_plan:view', '查看阅读计划统计', 2, 110, '/api/admin/reading-plans/stats', 1)
ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE id >= 110
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 110), (2, 111)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO role_permission (role_id, permission_id) VALUES
(3, 110), (3, 111)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
