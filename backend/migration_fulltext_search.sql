-- 全文搜索功能迁移脚本

-- 书籍页面文本表：存储PDF每一页的文本内容，用于全文搜索
CREATE TABLE IF NOT EXISTS book_page_text (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    book_title VARCHAR(255) DEFAULT '' COMMENT '书籍标题（冗余）',
    page_num INT NOT NULL COMMENT '页码',
    page_text LONGTEXT COMMENT '页面文本内容',
    word_count INT DEFAULT 0 COMMENT '字数统计',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_book_page (book_id, page_num),
    FULLTEXT KEY ft_page_text (page_text) WITH PARSER ngram COMMENT '全文索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍页面文本表';

-- 索引任务表：跟踪书籍文本提取和索引状态
CREATE TABLE IF NOT EXISTS book_index_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    book_title VARCHAR(255) DEFAULT '' COMMENT '书籍标题',
    total_pages INT DEFAULT 0 COMMENT '总页数',
    indexed_pages INT DEFAULT 0 COMMENT '已索引页数',
    status TINYINT DEFAULT 0 COMMENT '0-待处理 1-处理中 2-已完成 3-失败',
    error_message VARCHAR(1000) DEFAULT '' COMMENT '错误信息',
    priority INT DEFAULT 1 COMMENT '优先级(数字越大优先级越高)',
    started_at DATETIME DEFAULT NULL COMMENT '开始处理时间',
    finished_at DATETIME DEFAULT NULL COMMENT '完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_status (status),
    INDEX idx_status_priority (status, priority),
    UNIQUE KEY uk_book_id (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍索引任务表';

-- 新增全文搜索权限
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(120, 'search_mgmt', '全文搜索管理', 1, NULL, '/search-index', 13),
(121, 'search:view', '查看索引状态', 2, 120, '/api/admin/search/index-status', 1),
(122, 'search:rebuild', '重建索引', 2, 120, '/api/admin/search/rebuild', 2),
(123, 'search:alert_view', '查看索引失败告警', 2, 120, '/api/admin/search/alerts', 3)
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 超级管理员：全文搜索管理权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE id >= 120
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 运营：全文搜索管理权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 120), (2, 121), (2, 122), (2, 123)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 只读审计：查看索引状态和告警权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(3, 120), (3, 121), (3, 123)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
