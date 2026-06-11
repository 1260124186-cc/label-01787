-- 双语对照阅读功能 数据库迁移脚本
SET NAMES utf8mb4;
USE xiaoan_bookstore;

-- 双语书籍关联表
CREATE TABLE IF NOT EXISTS bilingual_pair (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    left_book_id BIGINT NOT NULL COMMENT '左侧书籍ID',
    right_book_id BIGINT NOT NULL COMMENT '右侧书籍ID',
    left_language VARCHAR(20) DEFAULT '' COMMENT '左侧语言标识',
    right_language VARCHAR(20) DEFAULT '' COMMENT '右侧语言标识',
    alignment_strategy TINYINT DEFAULT 1 COMMENT '对齐策略：1-章节号对齐 2-AI段落对齐',
    name VARCHAR(100) DEFAULT '' COMMENT '关联名称（如《小王子中英对照》）',
    last_left_unit INT DEFAULT 0 COMMENT '上次左侧阅读位置（页码/章节）',
    last_right_unit INT DEFAULT 0 COMMENT '上次右侧阅读位置（页码/章节）',
    left_unit_type TINYINT DEFAULT 1 COMMENT '左侧单位类型：1-页码 2-章节',
    right_unit_type TINYINT DEFAULT 1 COMMENT '右侧单位类型：1-页码 2-章节',
    sync_enabled TINYINT DEFAULT 1 COMMENT '是否开启同步滚动：0-关闭 1-开启',
    ai_alignment_status TINYINT DEFAULT 0 COMMENT 'AI对齐状态：0-未开始 1-进行中 2-已完成 3-失败',
    ai_alignment_progress INT DEFAULT 0 COMMENT 'AI对齐进度百分比',
    ai_alignment_error VARCHAR(500) DEFAULT '' COMMENT 'AI对齐错误信息',
    status TINYINT DEFAULT 1 COMMENT '0-已删除 1-正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_left_book (left_book_id),
    INDEX idx_right_book (right_book_id)
) ENGINE=InnoDB COMMENT='双语书籍关联表';

-- 段落对齐映射表
CREATE TABLE IF NOT EXISTS bilingual_alignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pair_id BIGINT NOT NULL COMMENT '关联ID',
    left_unit_index INT NOT NULL COMMENT '左侧单元索引（页码/章节）',
    right_unit_index INT NOT NULL COMMENT '右侧单元索引（页码/章节）',
    left_paragraph_index INT DEFAULT 0 COMMENT '左侧段落索引（单元内）',
    right_paragraph_index INT DEFAULT 0 COMMENT '右侧段落索引（单元内）',
    left_text_hash VARCHAR(64) DEFAULT '' COMMENT '左文本哈希用于快速匹配',
    right_text_hash VARCHAR(64) DEFAULT '' COMMENT '右文本哈希',
    alignment_method TINYINT DEFAULT 1 COMMENT '对齐方法：1-章节号 2-AI 3-手动',
    confidence DECIMAL(5,4) DEFAULT 0.0 COMMENT '对齐置信度（AI对齐用）',
    left_text_snippet VARCHAR(200) DEFAULT '' COMMENT '左侧文本片段（预览用）',
    right_text_snippet VARCHAR(200) DEFAULT '' COMMENT '右侧文本片段（预览用）',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pair_id (pair_id),
    INDEX idx_left_unit (pair_id, left_unit_index),
    INDEX idx_right_unit (pair_id, right_unit_index)
) ENGINE=InnoDB COMMENT='双语段落对齐映射表';

-- 给book表添加语言字段（如果不存在）
-- ALTER TABLE book ADD COLUMN language VARCHAR(20) DEFAULT '' COMMENT '语言标识' AFTER author;
