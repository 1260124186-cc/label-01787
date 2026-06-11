-- 家庭子账号功能迁移脚本
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS family (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '家庭名称',
    invite_code VARCHAR(20) NOT NULL UNIQUE COMMENT '邀请码',
    owner_id BIGINT NOT NULL COMMENT '家长(主账号)用户ID',
    member_count INT DEFAULT 1 COMMENT '成员数量',
    max_members INT DEFAULT 6 COMMENT '最大成员数',
    shared_storage BIGINT DEFAULT 0 COMMENT '共享存储池大小(字节)',
    status TINYINT DEFAULT 1 COMMENT '0-已解散 1-正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner_id (owner_id),
    INDEX idx_status (status),
    INDEX idx_invite_code (invite_code)
) ENGINE=InnoDB COMMENT='家庭表';

CREATE TABLE IF NOT EXISTS family_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role TINYINT DEFAULT 2 COMMENT '1-家长 2-子女',
    nickname VARCHAR(50) DEFAULT '' COMMENT '家庭内昵称',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_family_user (family_id, user_id),
    INDEX idx_family_id (family_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='家庭成员表';

CREATE TABLE IF NOT EXISTS family_shared_book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    shared_by BIGINT NOT NULL COMMENT '共享操作人ID',
    shared_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '共享时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_family_book (family_id, book_id),
    INDEX idx_family_id (family_id),
    INDEX idx_book_id (book_id)
) ENGINE=InnoDB COMMENT='家庭共享书架表';
