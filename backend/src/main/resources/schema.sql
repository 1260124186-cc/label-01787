-- 小安的书店 数据库初始化脚本
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS xiaoan_bookstore DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xiaoan_bookstore;

-- 管理员用户表
CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) DEFAULT '' COMMENT '昵称',
    role_id BIGINT DEFAULT NULL COMMENT '角色ID',
    status TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='管理员用户表';

-- 小程序用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    openid VARCHAR(100) NOT NULL UNIQUE COMMENT '微信openid',
    nickname VARCHAR(50) DEFAULT '' COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT '' COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='小程序用户表';

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='分类表';

-- 书籍表
CREATE TABLE IF NOT EXISTS book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) NOT NULL COMMENT '书名',
    author VARCHAR(100) DEFAULT '' COMMENT '作者',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    page_count INT DEFAULT 0 COMMENT '总页数',
    category_id BIGINT DEFAULT NULL COMMENT '分类ID',
    last_page INT DEFAULT 0 COMMENT '上次阅读页码',
    copyright_declared TINYINT DEFAULT 0 COMMENT '0-未声明 1-已声明版权',
    status TINYINT DEFAULT 1 COMMENT '0-已删除 1-正常 2-已下架',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB COMMENT='书籍表';

-- 批注/笔记表
CREATE TABLE IF NOT EXISTS annotation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    page_num INT NOT NULL COMMENT '页码',
    selected_text TEXT COMMENT '选中文本',
    content TEXT NOT NULL COMMENT '批注内容',
    type TINYINT NOT NULL COMMENT '1-评语 2-笔记',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_book (user_id, book_id)
) ENGINE=InnoDB COMMENT='批注笔记表';

-- 阅读记录表
CREATE TABLE IF NOT EXISTS reading_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    duration INT DEFAULT 0 COMMENT '阅读时长(秒)',
    last_page INT DEFAULT 0 COMMENT '阅读到的页码',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_user_book (user_id, book_id)
) ENGINE=InnoDB COMMENT='阅读记录表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    user_type TINYINT DEFAULT NULL COMMENT '1-管理员 2-小程序用户',
    action VARCHAR(100) NOT NULL COMMENT '操作类型',
    target VARCHAR(200) DEFAULT '' COMMENT '操作对象',
    detail TEXT COMMENT '详情',
    ip VARCHAR(50) DEFAULT '' COMMENT 'IP地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='操作日志表';

-- 角色表
CREATE TABLE IF NOT EXISTS role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(200) DEFAULT '' COMMENT '描述',
    status TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='角色表';

-- 权限表（菜单+接口级）
CREATE TABLE IF NOT EXISTS permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    name VARCHAR(100) NOT NULL COMMENT '权限名称',
    type TINYINT NOT NULL COMMENT '1-菜单 2-接口',
    parent_id BIGINT DEFAULT NULL COMMENT '父权限ID',
    path VARCHAR(200) DEFAULT '' COMMENT '菜单路径或接口路径',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id),
    INDEX idx_code (code)
) ENGINE=InnoDB COMMENT='权限表';

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB COMMENT='角色权限关联表';

-- 文件下载日志表
CREATE TABLE IF NOT EXISTS file_download_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL COMMENT '下载人ID',
    user_type TINYINT DEFAULT NULL COMMENT '1-管理员 2-小程序用户',
    file_token VARCHAR(200) NOT NULL COMMENT '签名令牌',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    ip VARCHAR(50) DEFAULT '' COMMENT 'IP地址',
    referer VARCHAR(500) DEFAULT '' COMMENT '来源页',
    user_agent VARCHAR(500) DEFAULT '' COMMENT 'UA',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file_token (file_token),
    INDEX idx_user (user_id, user_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='文件下载日志表';

-- 敏感操作确认令牌表
CREATE TABLE IF NOT EXISTS sensitive_confirm_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NOT NULL COMMENT '管理员ID',
    token VARCHAR(200) NOT NULL UNIQUE COMMENT '确认令牌',
    operation VARCHAR(200) NOT NULL COMMENT '操作标识',
    expired_at DATETIME NOT NULL COMMENT '过期时间',
    used TINYINT DEFAULT 0 COMMENT '0-未使用 1-已使用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token (token),
    INDEX idx_admin_id (admin_id)
) ENGINE=InnoDB COMMENT='敏感操作确认令牌表';

-- 版权申诉工单表
CREATE TABLE IF NOT EXISTS copyright_complaint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    complainant_name VARCHAR(100) NOT NULL COMMENT '申诉人姓名',
    complainant_contact VARCHAR(200) NOT NULL COMMENT '联系方式',
    book_id BIGINT DEFAULT NULL COMMENT '关联书籍ID',
    book_title VARCHAR(200) DEFAULT '' COMMENT '书籍名称',
    reason TEXT NOT NULL COMMENT '申诉原因',
    evidence_urls VARCHAR(2000) DEFAULT '' COMMENT '证明材料URL(逗号分隔)',
    status TINYINT DEFAULT 0 COMMENT '0-待处理 1-处理中 2-已下架 3-已驳回',
    handler_id BIGINT DEFAULT NULL COMMENT '处理人ID',
    handle_result TEXT COMMENT '处理结果',
    handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_book_id (book_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='版权申诉工单表';

-- 内容审核记录表
CREATE TABLE IF NOT EXISTS content_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type TINYINT NOT NULL COMMENT '1-书名 2-批注 3-书摘',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    content TEXT NOT NULL COMMENT '被审核文本',
    result TINYINT NOT NULL COMMENT '0-通过 1-疑似违规 2-确认违规',
    keywords VARCHAR(500) DEFAULT '' COMMENT '命中的敏感词(逗号分隔)',
    auditor_id BIGINT DEFAULT NULL COMMENT '审核人ID(人工审核时)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_target (target_type, target_id),
    INDEX idx_result (result),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='内容审核记录表';

-- 初始化角色
INSERT INTO role (id, code, name, description) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '拥有全部权限'),
(2, 'OPERATOR', '运营', '日常运营管理权限'),
(3, 'AUDITOR', '只读审计', '仅查看和审计权限')
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 初始化权限
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(1,  'dashboard', '仪表盘', 1, NULL, '/dashboard', 1),
(2,  'dashboard:view', '查看仪表盘', 2, 1, '/api/admin/dashboard', 1),
(10, 'user_mgmt', '用户管理', 1, NULL, '/users', 2),
(11, 'user:view', '查看用户', 2, 10, '/api/admin/users', 1),
(12, 'user:disable', '禁用用户', 2, 10, '/api/admin/users/*/disable', 2),
(20, 'book_mgmt', '书籍管理', 1, NULL, '/books', 3),
(21, 'book:view', '查看书籍', 2, 20, '/api/admin/books', 1),
(22, 'book:delete', '删除书籍', 2, 20, '/api/admin/books/*/delete', 2),
(30, 'admin_mgmt', '管理员管理', 1, NULL, '/admins', 4),
(31, 'admin:view', '查看管理员', 2, 30, '/api/admin/admins', 1),
(32, 'admin:create', '创建管理员', 2, 30, '/api/admin/admins/create', 2),
(33, 'admin:update', '修改管理员', 2, 30, '/api/admin/admins/*/update', 3),
(34, 'admin:delete', '删除管理员', 2, 30, '/api/admin/admins/*/delete', 4),
(40, 'log_mgmt', '日志管理', 1, NULL, '/logs', 5),
(41, 'log:view', '查看日志', 2, 40, '/api/admin/logs', 1),
(50, 'role_mgmt', '角色权限管理', 1, NULL, '/roles', 6),
(51, 'role:view', '查看角色', 2, 50, '/api/admin/roles', 1),
(52, 'role:update', '修改角色权限', 2, 50, '/api/admin/roles/*/update', 2),
(60, 'compliance_mgmt', '合规管理', 1, NULL, '/compliance', 7),
(61, 'complaint:view', '查看版权申诉', 2, 60, '/api/admin/complaints', 1),
(62, 'complaint:handle', '处理版权申诉', 2, 60, '/api/admin/complaints/*/handle', 2),
(63, 'audit:view', '查看内容审核', 2, 60, '/api/admin/audits', 1),
(64, 'audit:report', '合规审计报告', 2, 60, '/api/admin/compliance/report', 2)
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 超级管理员：全部权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 运营：仪表盘+用户查看+书籍管理+日志查看+合规管理
INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 1), (2, 2), (2, 10), (2, 11), (2, 20), (2, 21), (2, 22), (2, 40), (2, 41),
(2, 60), (2, 61), (2, 62), (2, 63), (2, 64)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 只读审计：仪表盘+用户查看+书籍查看+日志查看+合规查看
INSERT INTO role_permission (role_id, permission_id) VALUES
(3, 1), (3, 2), (3, 10), (3, 11), (3, 20), (3, 21), (3, 40), (3, 41),
(3, 60), (3, 61), (3, 63), (3, 64)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 初始化管理员账号 (密码: admin123)，默认超级管理员角色
-- verifyPassword 方法兼容明文与 BCrypt，首次启动后可通过接口修改为 BCrypt 密码
INSERT INTO admin_user (username, password, nickname, role_id, status) VALUES
('admin', 'admin123', '超级管理员', 1, 1)
ON DUPLICATE KEY UPDATE username = username;
