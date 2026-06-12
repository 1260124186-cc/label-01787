-- 阅读赛季/挑战赛 迁移脚本
-- 执行前请备份数据库

-- 1. 赛季配置表
CREATE TABLE IF NOT EXISTS reading_season (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL COMMENT '赛季标题',
    subtitle VARCHAR(200) DEFAULT '' COMMENT '赛季副标题',
    cover_image VARCHAR(500) DEFAULT '' COMMENT '封面图',
    description TEXT COMMENT '赛季描述',
    season_type TINYINT DEFAULT 1 COMMENT '1-挑战赛 2-阅读马拉松 3-读书月',
    status TINYINT DEFAULT 0 COMMENT '0-草稿 1-报名中 2-进行中 3-已结束 4-已取消',
    start_date DATE NOT NULL COMMENT '赛季开始日期',
    end_date DATE NOT NULL COMMENT '赛季结束日期',
    signup_start DATE DEFAULT NULL COMMENT '报名开始日期',
    signup_end DATE DEFAULT NULL COMMENT '报名截止日期',
    duration_days INT NOT NULL COMMENT '赛季天数',
    daily_min_duration INT DEFAULT 600 COMMENT '每日最低阅读时长(秒)',
    daily_max_duration INT DEFAULT 28800 COMMENT '每日最高有效阅读时长(秒)，防刷',
    max_participants INT DEFAULT 0 COMMENT '最大参与人数，0-不限',
    points_reward INT DEFAULT 0 COMMENT '完成赛季奖励积分',
    badge_icon VARCHAR(100) DEFAULT '' COMMENT '赛季徽章图标标识',
    badge_name VARCHAR(100) DEFAULT '' COMMENT '赛季徽章名称',
    rules TEXT COMMENT '赛季规则说明',
    prize_config TEXT COMMENT '奖品配置JSON',
    cheat_threshold_speed INT DEFAULT 7200 COMMENT '作弊检测：每小时最高阅读秒数',
    cheat_threshold_streak INT DEFAULT 168 COMMENT '作弊检测：连续阅读最长小时数',
    cheat_auto_flag TINYINT DEFAULT 1 COMMENT '是否自动标记可疑记录',
    published_at DATETIME DEFAULT NULL COMMENT '发布时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date)
) ENGINE=InnoDB COMMENT='阅读赛季配置表';

-- 2. 赛季参与者表
CREATE TABLE IF NOT EXISTS season_participant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    season_id BIGINT NOT NULL COMMENT '赛季ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status TINYINT DEFAULT 1 COMMENT '0-已退出 1-进行中 2-已完成 3-未完成 4-作弊取消资格',
    signup_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    qualified_days INT DEFAULT 0 COMMENT '达标天数',
    total_duration BIGINT DEFAULT 0 COMMENT '总阅读时长(秒)',
    total_books INT DEFAULT 0 COMMENT '阅读书籍数',
    streak_days INT DEFAULT 0 COMMENT '连续达标天数',
    max_streak_days INT DEFAULT 0 COMMENT '最大连续达标天数',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    points_awarded INT DEFAULT 0 COMMENT '已发放积分数',
    badge_awarded TINYINT DEFAULT 0 COMMENT '是否已颁发徽章 0-否 1-是',
    prize_claimed TINYINT DEFAULT 0 COMMENT '奖品是否已领取 0-否 1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_season_user (season_id, user_id),
    INDEX idx_season_id (season_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_season_streak (season_id, streak_days DESC)
) ENGINE=InnoDB COMMENT='赛季参与者表';

-- 3. 赛季每日记录表
CREATE TABLE IF NOT EXISTS season_daily_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    season_id BIGINT NOT NULL COMMENT '赛季ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    duration INT DEFAULT 0 COMMENT '当日阅读时长(秒)',
    book_count INT DEFAULT 0 COMMENT '当日阅读书籍数',
    is_qualified TINYINT DEFAULT 0 COMMENT '是否达标 0-否 1-是',
    is_flagged TINYINT DEFAULT 0 COMMENT '是否被标记可疑 0-否 1-是',
    flag_reason VARCHAR(500) DEFAULT '' COMMENT '标记原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_season_user_date (season_id, user_id, record_date),
    INDEX idx_season_date (season_id, record_date),
    INDEX idx_user_date (user_id, record_date),
    INDEX idx_flagged (is_flagged)
) ENGINE=InnoDB COMMENT='赛季每日记录表';

-- 4. 赛季徽章表
CREATE TABLE IF NOT EXISTS season_badge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    season_id BIGINT NOT NULL COMMENT '赛季ID',
    badge_type VARCHAR(50) NOT NULL COMMENT '徽章类型',
    badge_name VARCHAR(100) NOT NULL COMMENT '徽章名称',
    badge_icon VARCHAR(100) NOT NULL COMMENT '徽章图标',
    earned_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    INDEX idx_user_id (user_id),
    INDEX idx_season_id (season_id),
    UNIQUE KEY uk_user_season_type (user_id, season_id, badge_type)
) ENGINE=InnoDB COMMENT='赛季徽章表';

-- 5. 作弊检测记录表
CREATE TABLE IF NOT EXISTS season_cheat_detection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    season_id BIGINT NOT NULL COMMENT '赛季ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    detection_date DATE NOT NULL COMMENT '检测日期',
    detection_type TINYINT NOT NULL COMMENT '1-阅读速度异常 2-连续时长异常 3-数据突变 4-设备异常',
    detection_detail TEXT COMMENT '检测详情JSON',
    severity TINYINT DEFAULT 1 COMMENT '1-轻微 2-中等 3-严重',
    status TINYINT DEFAULT 0 COMMENT '0-待审核 1-确认作弊 2-误报 3-已处理',
    handled_by BIGINT DEFAULT NULL COMMENT '处理人ID',
    handle_result VARCHAR(500) DEFAULT '' COMMENT '处理结果',
    handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_season_id (season_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_detection_date (detection_date)
) ENGINE=InnoDB COMMENT='赛季作弊检测记录表';

-- 6. 赛季管理权限
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(140, 'season_mgmt', '赛季管理', 1, NULL, '/seasons', 15),
(141, 'season:view', '查看赛季', 2, 140, '/api/admin/seasons', 1),
(142, 'season:create', '创建赛季', 2, 140, '/api/admin/seasons', 2),
(143, 'season:update', '编辑赛季', 2, 140, '/api/admin/seasons', 3),
(144, 'season:delete', '删除赛季', 2, 140, '/api/admin/seasons', 4),
(145, 'season:prize', '奖品发放', 2, 140, '/api/admin/seasons/prize', 5),
(146, 'season:cheat', '作弊审核', 2, 140, '/api/admin/seasons/cheat', 6)
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 超级管理员添加所有赛季权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(1, 140), (1, 141), (1, 142), (1, 143), (1, 144), (1, 145), (1, 146)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 运营角色添加查看、创建、编辑、奖品发放权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 140), (2, 141), (2, 142), (2, 143), (2, 145), (2, 146)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 只读审计角色添加查看权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(3, 140), (3, 141)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
