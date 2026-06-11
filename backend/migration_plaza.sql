-- 书摘广场相关表

-- 公开书摘表
CREATE TABLE IF NOT EXISTS public_excerpt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '发布用户ID',
    annotation_id BIGINT DEFAULT NULL COMMENT '关联的批注ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    book_title VARCHAR(200) NOT NULL COMMENT '书名',
    book_author VARCHAR(100) DEFAULT '' COMMENT '作者',
    excerpt_text TEXT NOT NULL COMMENT '书摘原文',
    comment_text TEXT COMMENT '评语/感想',
    likes INT DEFAULT 0 COMMENT '点赞数',
    favorites INT DEFAULT 0 COMMENT '收藏数',
    views INT DEFAULT 0 COMMENT '浏览数',
    status TINYINT DEFAULT 1 COMMENT '0-已撤回 1-正常 2-已下架',
    audit_status TINYINT DEFAULT 0 COMMENT '0-待审核 1-审核通过 2-审核不通过',
    report_count INT DEFAULT 0 COMMENT '举报次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status, audit_status),
    INDEX idx_created_at (created_at),
    INDEX idx_likes (likes)
) ENGINE=InnoDB COMMENT='公开书摘表';

-- 书摘点赞表
CREATE TABLE IF NOT EXISTS excerpt_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    excerpt_id BIGINT NOT NULL COMMENT '书摘ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_excerpt_user (excerpt_id, user_id),
    INDEX idx_excerpt_id (excerpt_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='书摘点赞表';

-- 书摘收藏表
CREATE TABLE IF NOT EXISTS excerpt_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    excerpt_id BIGINT NOT NULL COMMENT '书摘ID',
    user_id BIGINT NOT NULL COMMENT '收藏用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_excerpt_user (excerpt_id, user_id),
    INDEX idx_excerpt_id (excerpt_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='书摘收藏表';

-- 书摘举报表
CREATE TABLE IF NOT EXISTS excerpt_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    excerpt_id BIGINT NOT NULL COMMENT '书摘ID',
    reporter_id BIGINT NOT NULL COMMENT '举报人ID',
    reason VARCHAR(200) NOT NULL COMMENT '举报原因',
    detail TEXT COMMENT '详细描述',
    status TINYINT DEFAULT 0 COMMENT '0-待处理 1-已处理 2-已驳回',
    handler_id BIGINT DEFAULT NULL COMMENT '处理人ID',
    handle_result TEXT COMMENT '处理结果',
    handled_at DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_excerpt_id (excerpt_id),
    INDEX idx_reporter_id (reporter_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='书摘举报表';

-- 新增广场管理权限
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(110, 'plaza_mgmt', '书摘广场', 1, NULL, '/plaza', 12),
(111, 'plaza:view', '查看书摘', 2, 110, '/api/admin/plaza/excerpts', 1),
(112, 'plaza:audit', '审核书摘', 2, 110, '/api/admin/plaza/excerpts/*/audit', 2),
(113, 'plaza:remove', '下架书摘', 2, 110, '/api/admin/plaza/excerpts/*/remove', 3),
(114, 'plaza:report_view', '查看举报', 2, 110, '/api/admin/plaza/reports', 4),
(115, 'plaza:report_handle', '处理举报', 2, 110, '/api/admin/plaza/reports/*/handle', 5)
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 超级管理员：广场管理权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE id >= 110
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 运营：广场管理权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 110), (2, 111), (2, 112), (2, 113), (2, 114), (2, 115)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 只读审计：查看广场权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(3, 110), (3, 111), (3, 114)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
