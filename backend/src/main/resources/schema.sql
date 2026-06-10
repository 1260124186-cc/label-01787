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

-- 消息通知表
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL COMMENT '接收用户ID，NULL表示全体用户',
    type TINYINT NOT NULL COMMENT '1-系统通知 2-审核结果 3-计划提醒 4-小组动态',
    title VARCHAR(200) NOT NULL COMMENT '消息标题',
    content TEXT NOT NULL COMMENT '消息内容',
    extra_data VARCHAR(1000) DEFAULT '' COMMENT '附加数据(JSON格式，用于跳转参数)',
    is_read TINYINT DEFAULT 0 COMMENT '0-未读 1-已读',
    sender_id BIGINT DEFAULT NULL COMMENT '发送人ID(管理员ID)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME DEFAULT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='消息通知表';

-- 消息模板表
CREATE TABLE IF NOT EXISTS notification_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    type TINYINT NOT NULL COMMENT '1-系统通知 2-审核结果 3-计划提醒 4-小组动态',
    title VARCHAR(200) NOT NULL COMMENT '消息标题',
    content TEXT NOT NULL COMMENT '消息内容(支持占位符{变量名})',
    status TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_type (type)
) ENGINE=InnoDB COMMENT='消息模板表';

-- 新增权限
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(70, 'notification_mgmt', '消息管理', 1, NULL, '/notifications', 8),
(71, 'notification:view', '查看消息', 2, 70, '/api/admin/notifications', 1),
(72, 'notification:send', '发送公告', 2, 70, '/api/admin/notifications/send', 2),
(73, 'template:view', '查看模板', 2, 70, '/api/admin/notification-templates', 3),
(74, 'template:create', '创建模板', 2, 70, '/api/admin/notification-templates/create', 4),
(75, 'template:update', '修改模板', 2, 70, '/api/admin/notification-templates/*/update', 5),
(76, 'template:delete', '删除模板', 2, 70, '/api/admin/notification-templates/*/delete', 6)
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 超级管理员：全部权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE id >= 70
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 运营：消息管理权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 70), (2, 71), (2, 72), (2, 73), (2, 74), (2, 75), (2, 76)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 初始化管理员账号 (密码: admin123)，默认超级管理员角色
-- verifyPassword 方法兼容明文与 BCrypt，首次启动后可通过接口修改为 BCrypt 密码
INSERT INTO admin_user (username, password, nickname, role_id, status) VALUES
('admin', 'admin123', '超级管理员', 1, 1)
ON DUPLICATE KEY UPDATE username = username;

-- 初始化消息模板
INSERT INTO notification_template (code, name, type, title, content, status) VALUES
('SYSTEM_ANNOUNCEMENT', '系统公告', 1, '系统公告', '{content}', 1),
('BOOK_AUDIT_PASS', '书籍审核通过', 2, '审核结果通知', '您的书籍《{bookTitle}》已通过审核，快去阅读吧！', 1),
('BOOK_AUDIT_REJECT', '书籍审核驳回', 2, '审核结果通知', '您的书籍《{bookTitle}》未通过审核，原因：{reason}', 1),
('READING_PLAN_REMINDER', '阅读计划提醒', 3, '阅读提醒', '您今天的阅读计划还未完成，快去阅读吧！', 1),
('GROUP_NEW_DYNAMIC', '小组新动态', 4, '小组动态', '{userName} 在小组发布了新动态，快去看看吧！', 1)
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 备份任务表
CREATE TABLE IF NOT EXISTS backup_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    task_type TINYINT NOT NULL COMMENT '1-导出 2-导入',
    status TINYINT NOT NULL COMMENT '0-待处理 1-处理中 2-已完成 3-失败',
    file_name VARCHAR(255) DEFAULT '' COMMENT '备份文件名',
    file_path VARCHAR(500) DEFAULT '' COMMENT '备份文件路径',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    book_count INT DEFAULT 0 COMMENT '书籍数量',
    annotation_count INT DEFAULT 0 COMMENT '批注数量',
    record_count INT DEFAULT 0 COMMENT '阅读记录数量',
    category_count INT DEFAULT 0 COMMENT '分类数量',
    progress INT DEFAULT 0 COMMENT '进度(0-100)',
    error_message TEXT COMMENT '错误信息',
    expired_at DATETIME DEFAULT NULL COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='备份任务表';

-- 新增备份管理权限
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(80, 'backup_mgmt', '备份管理', 1, NULL, '/backups', 9),
(81, 'backup:view', '查看备份任务', 2, 80, '/api/admin/backups', 1),
(82, 'backup:export', '导出备份', 2, 80, '/api/admin/backups/*/export', 2),
(83, 'backup:delete', '删除备份', 2, 80, '/api/admin/backups/*/delete', 3),
(84, 'storage:view', '查看存储统计', 2, 80, '/api/admin/storage', 4)
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 超级管理员：备份管理权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE id >= 80
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 运营：备份管理权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 80), (2, 81), (2, 82), (2, 83), (2, 84)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 只读审计：查看备份和存储权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(3, 80), (3, 81), (3, 84)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 会员套餐配置表
CREATE TABLE IF NOT EXISTS membership_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '套餐编码(free/vip)',
    name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    description VARCHAR(500) DEFAULT '' COMMENT '套餐描述',
    price INT NOT NULL DEFAULT 0 COMMENT '价格(分)',
    duration_days INT NOT NULL DEFAULT 30 COMMENT '时长(天)',
    max_books INT NOT NULL DEFAULT 20 COMMENT '书籍数量上限',
    max_storage BIGINT NOT NULL DEFAULT 2147483648 COMMENT '存储上限(字节)',
    ai_daily_limit INT NOT NULL DEFAULT 5 COMMENT 'AI每日使用上限',
    priority_queue TINYINT NOT NULL DEFAULT 0 COMMENT '0-普通队列 1-优先转图队列',
    advanced_stats TINYINT NOT NULL DEFAULT 0 COMMENT '0-基础统计 1-高级统计报告',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code)
) ENGINE=InnoDB COMMENT='会员套餐配置表';

-- 用户会员状态表
CREATE TABLE IF NOT EXISTS user_membership (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    plan_code VARCHAR(50) NOT NULL DEFAULT 'free' COMMENT '当前套餐编码',
    expire_at DATETIME DEFAULT NULL COMMENT '会员过期时间',
    auto_renew TINYINT DEFAULT 0 COMMENT '0-不自动续费 1-自动续费',
    extra_storage BIGINT DEFAULT 0 COMMENT '额外购买的存储空间(字节)',
    ai_used_today INT DEFAULT 0 COMMENT '今日AI使用次数',
    ai_usage_date VARCHAR(10) DEFAULT '' COMMENT 'AI使用计数日期(yyyy-MM-dd)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_expire_at (expire_at)
) ENGINE=InnoDB COMMENT='用户会员状态表';

-- 积分账户表
CREATE TABLE IF NOT EXISTS points_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    balance INT NOT NULL DEFAULT 0 COMMENT '当前积分余额',
    total_earned INT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    total_consumed INT NOT NULL DEFAULT 0 COMMENT '累计消费积分',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='积分账户表';

-- 积分记录表
CREATE TABLE IF NOT EXISTS points_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type TINYINT NOT NULL COMMENT '1-获得 2-消费',
    category VARCHAR(50) NOT NULL COMMENT '分类(daily_checkin/upload_book/share_excerpt/exchange_vip/exchange_storage/admin_adjust)',
    points INT NOT NULL COMMENT '积分数量(正数)',
    balance_after INT NOT NULL COMMENT '变动后余额',
    description VARCHAR(200) DEFAULT '' COMMENT '描述',
    ref_id VARCHAR(100) DEFAULT '' COMMENT '关联业务ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='积分记录表';

-- 积分规则配置表
CREATE TABLE IF NOT EXISTS points_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    category VARCHAR(50) NOT NULL COMMENT '分类',
    points INT NOT NULL COMMENT '积分数',
    daily_limit INT DEFAULT 0 COMMENT '每日上限(0=不限)',
    description VARCHAR(200) DEFAULT '' COMMENT '描述',
    status TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code)
) ENGINE=InnoDB COMMENT='积分规则配置表';

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    plan_id BIGINT DEFAULT NULL COMMENT '套餐ID',
    order_type TINYINT NOT NULL COMMENT '1-会员购买 2-存储包购买',
    amount INT NOT NULL COMMENT '金额(分)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已取消 3-已退款',
    wx_prepay_id VARCHAR(64) DEFAULT '' COMMENT '微信预支付ID',
    wx_transaction_id VARCHAR(64) DEFAULT '' COMMENT '微信支付交易号',
    storage_gb INT DEFAULT NULL COMMENT '存储包大小(GB，order_type=2时有效)',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    expired_at DATETIME DEFAULT NULL COMMENT '订单过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='订单表';

-- 积分兑换记录表
CREATE TABLE IF NOT EXISTS points_exchange (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    exchange_type TINYINT NOT NULL COMMENT '1-兑换会员天数 2-兑换存储包',
    points_cost INT NOT NULL COMMENT '消耗积分',
    value INT NOT NULL COMMENT '兑换值(天数或MB数)',
    order_no VARCHAR(64) DEFAULT '' COMMENT '关联订单号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='积分兑换记录表';

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

-- 初始化会员套餐
INSERT INTO membership_plan (code, name, description, price, duration_days, max_books, max_storage, ai_daily_limit, priority_queue, advanced_stats, sort_order) VALUES
('free', '免费版', '基础功能，适合轻度阅读用户', 0, 0, 20, 2147483648, 5, 0, 0, 1),
('vip', '会员版', '无限书籍、50GB存储、优先转图、高级统计', 1500, 30, 0, 53687091200, 0, 1, 1, 2)
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 初始化积分规则
INSERT INTO points_rule (code, name, category, points, daily_limit, description) VALUES
('daily_checkin', '每日打卡', 'daily_checkin', 10, 1, '每日签到获得积分'),
('upload_book', '上传书籍', 'upload_book', 20, 3, '上传一本新书获得积分'),
('share_excerpt', '分享书摘', 'share_excerpt', 15, 5, '分享书摘获得积分')
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 新增会员管理权限
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(90, 'membership_mgmt', '会员管理', 1, NULL, '/membership', 10),
(91, 'plan:view', '查看套餐', 2, 90, '/api/admin/membership/plans', 1),
(92, 'plan:create', '创建套餐', 2, 90, '/api/admin/membership/plans/create', 2),
(93, 'plan:update', '修改套餐', 2, 90, '/api/admin/membership/plans/*/update', 3),
(94, 'order:view', '查看订单', 2, 90, '/api/admin/membership/orders', 4),
(95, 'member:view', '查看会员状态', 2, 90, '/api/admin/membership/members', 5),
(96, 'points_rule:view', '查看积分规则', 2, 90, '/api/admin/membership/points-rules', 6),
(97, 'points_rule:update', '修改积分规则', 2, 90, '/api/admin/membership/points-rules/*/update', 7),
(98, 'points_adjust', '调整用户积分', 2, 90, '/api/admin/membership/points-adjust', 8)
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 超级管理员：会员管理权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE id >= 90
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 运营：会员管理权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 90), (2, 91), (2, 92), (2, 93), (2, 94), (2, 95), (2, 96), (2, 97), (2, 98)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 只读审计：查看会员和订单权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(3, 90), (3, 91), (3, 94), (3, 95), (3, 96)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
