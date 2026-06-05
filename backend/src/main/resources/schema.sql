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
    status TINYINT DEFAULT 1 COMMENT '0-已删除 1-正常',
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

-- 初始化管理员账号 (密码: admin123)
-- verifyPassword 方法兼容明文与 BCrypt，首次启动后可通过接口修改为 BCrypt 密码
INSERT INTO admin_user (username, password, nickname, status) VALUES
('admin', 'admin123', '超级管理员', 1)
ON DUPLICATE KEY UPDATE username = username;
