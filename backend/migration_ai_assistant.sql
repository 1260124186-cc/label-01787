-- AI阅读助手功能扩展

-- AI对话历史表
CREATE TABLE IF NOT EXISTS ai_chat_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    book_title VARCHAR(255) DEFAULT '' COMMENT '书籍标题',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID，按书籍分组',
    type TINYINT NOT NULL COMMENT '1-摘要 2-解释 3-翻译 4-出题自测 5-章节大纲 6-知识卡片',
    source_type TINYINT NOT NULL COMMENT '1-选中段落 2-当前页 3-全书',
    source_text TEXT COMMENT '原文内容（选中段落或当前页文本）',
    page_num INT DEFAULT NULL COMMENT '页码（当前页或选中文本所在页）',
    user_prompt TEXT COMMENT '用户提示词',
    ai_response TEXT COMMENT 'AI响应内容',
    extra_data TEXT COMMENT '额外数据(JSON格式，如题目选项、知识卡片字段)',
    status TINYINT DEFAULT 1 COMMENT '0-失败 1-成功 2-生成中',
    error_msg VARCHAR(500) DEFAULT '' COMMENT '错误信息',
    is_deleted TINYINT DEFAULT 0 COMMENT '0-未删除 1-已删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_book (user_id, book_id),
    INDEX idx_session_id (session_id),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='AI对话历史表';

-- AI结果标注表（可选扩展）
CREATE TABLE IF NOT EXISTS ai_disclaimer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    version VARCHAR(20) NOT NULL COMMENT '免责声明版本',
    agreed TINYINT DEFAULT 0 COMMENT '0-未同意 1-已同意',
    agreed_at DATETIME DEFAULT NULL COMMENT '同意时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_version (user_id, version)
) ENGINE=InnoDB COMMENT='AI免责声明确认表';

-- 扩展ai_summary表，增加类型和来源字段
ALTER TABLE ai_summary 
ADD COLUMN IF NOT EXISTS type TINYINT DEFAULT 1 COMMENT '1-全书摘要 2-章节大纲 3-知识卡片',
ADD COLUMN IF NOT EXISTS content_json TEXT COMMENT '结构化内容(JSON格式)';

-- 版权声明确认字段
ALTER TABLE book 
ADD COLUMN IF NOT EXISTS copyright_agreed_at DATETIME DEFAULT NULL COMMENT '版权声明确认时间';
