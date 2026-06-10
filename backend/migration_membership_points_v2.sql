-- 积分与会员体系 迁移脚本 - V2
-- 执行前请备份数据库

-- 1. 用户会员状态表新增 AI 使用计数字段
ALTER TABLE user_membership
    ADD COLUMN ai_used_today INT DEFAULT 0 COMMENT '今日AI使用次数' AFTER extra_storage,
    ADD COLUMN ai_usage_date VARCHAR(10) DEFAULT '' COMMENT 'AI使用计数日期(yyyy-MM-dd)' AFTER ai_used_today;

-- 2. 订单表新增存储包大小字段
ALTER TABLE `order`
    ADD COLUMN storage_gb INT DEFAULT NULL COMMENT '存储包大小(GB，order_type=2时有效)' AFTER wx_transaction_id;

-- 3. AI摘要记录表
CREATE TABLE IF NOT EXISTS ai_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    book_title VARCHAR(255) DEFAULT '' COMMENT '书籍标题',
    summary TEXT COMMENT 'AI生成的摘要',
    key_points TEXT COMMENT '核心要点',
    status TINYINT DEFAULT 2 COMMENT '0-失败 1-成功 2-生成中',
    error_msg VARCHAR(500) DEFAULT '' COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='AI摘要记录表';

-- 4. PDF转图任务表
CREATE TABLE IF NOT EXISTS convert_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    book_title VARCHAR(255) DEFAULT '' COMMENT '书籍标题',
    total_pages INT DEFAULT 0 COMMENT '总页数',
    converted_pages INT DEFAULT 0 COMMENT '已转换页数',
    priority INT DEFAULT 1 COMMENT '优先级(数字越大优先级越高，VIP为10，免费用户为1)',
    status TINYINT DEFAULT 0 COMMENT '0-等待中 1-处理中 2-已完成 3-失败',
    error_msg VARCHAR(500) DEFAULT '' COMMENT '错误信息',
    started_at DATETIME DEFAULT NULL COMMENT '开始处理时间',
    finished_at DATETIME DEFAULT NULL COMMENT '完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_status_priority (status, priority)
) ENGINE=InnoDB COMMENT='PDF转图任务表';

-- 5. 新增转图任务和AI摘要管理权限
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(99, 'convert:view', '查看转图任务', 2, 90, '/api/admin/convert/tasks', 9),
(100, 'ai_summary:view', '查看AI摘要', 2, 90, '/api/admin/ai/summaries', 10)
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 超级管理员和运营添加权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(1, 99), (1, 100),
(2, 99), (2, 100)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 只读审计添加查看权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(3, 99), (3, 100)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
