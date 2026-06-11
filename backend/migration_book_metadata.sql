-- 书籍元数据聚合功能迁移脚本

-- 1. 扩展 book 表，增加元数据字段
ALTER TABLE book
    ADD COLUMN IF NOT EXISTS isbn VARCHAR(20) DEFAULT '' COMMENT 'ISBN号' AFTER status,
    ADD COLUMN IF NOT EXISTS cover_url VARCHAR(500) DEFAULT '' COMMENT '封面图片URL（第三方数据源）' AFTER isbn,
    ADD COLUMN IF NOT EXISTS description TEXT COMMENT '书籍简介' AFTER cover_url,
    ADD COLUMN IF NOT EXISTS rating DOUBLE DEFAULT NULL COMMENT '平均评分' AFTER description,
    ADD COLUMN IF NOT EXISTS rating_count INT DEFAULT 0 COMMENT '评分人数' AFTER rating,
    ADD COLUMN IF NOT EXISTS tags VARCHAR(500) DEFAULT '' COMMENT '标签（逗号分隔）' AFTER rating_count,
    ADD COLUMN IF NOT EXISTS publisher VARCHAR(200) DEFAULT '' COMMENT '出版社' AFTER tags,
    ADD COLUMN IF NOT EXISTS publish_date VARCHAR(50) DEFAULT '' COMMENT '出版日期' AFTER publisher,
    ADD COLUMN IF NOT EXISTS language VARCHAR(20) DEFAULT '' COMMENT '语言' AFTER publish_date,
    ADD COLUMN IF NOT EXISTS metadata_source VARCHAR(50) DEFAULT '' COMMENT '元数据来源(openlibrary/google_books)' AFTER language,
    ADD COLUMN IF NOT EXISTS metadata_fetched_at DATETIME DEFAULT NULL COMMENT '元数据拉取时间' AFTER metadata_source;

CREATE INDEX IF NOT EXISTS idx_book_isbn ON book(isbn);
CREATE INDEX IF NOT EXISTS idx_book_metadata_source ON book(metadata_source);

-- 2. 初始化元数据源系统配置
INSERT INTO sys_config (config_key, config_value, config_type, description, category, is_editable) VALUES
('metadata.auto_fetch.enabled', 'true', 'boolean', '上传书籍时是否自动拉取元数据', 'metadata', 1),
('metadata.cache.local_max_size', '1000', 'number', '本地缓存最大条目数', 'metadata', 1),
('metadata.cache.local_expire_minutes', '60', 'number', '本地缓存过期时间（分钟）', 'metadata', 1),
('metadata.cache.redis_expire_hours', '168', 'number', 'Redis缓存过期时间（小时，默认7天）', 'metadata', 1),

('metadata.openlibrary.enabled', 'true', 'boolean', '是否启用Open Library数据源', 'metadata', 1),
('metadata.openlibrary.priority', '10', 'number', 'Open Library优先级（数字越小优先级越高）', 'metadata', 1),
('metadata.openlibrary.api_key', '', 'string', 'Open Library API Key（可选）', 'metadata', 1),
('metadata.openlibrary.daily_limit', '1000', 'number', 'Open Library每日调用上限', 'metadata', 1),
('metadata.openlibrary.timeout_ms', '5000', 'number', 'Open Library请求超时时间（毫秒）', 'metadata', 1),

('metadata.google_books.enabled', 'true', 'boolean', '是否启用Google Books数据源', 'metadata', 1),
('metadata.google_books.priority', '20', 'number', 'Google Books优先级（数字越小优先级越高）', 'metadata', 1),
('metadata.google_books.api_key', '', 'string', 'Google Books API Key', 'metadata', 1),
('metadata.google_books.daily_limit', '1000', 'number', 'Google Books每日调用上限', 'metadata', 1),
('metadata.google_books.timeout_ms', '5000', 'number', 'Google Books请求超时时间（毫秒）', 'metadata', 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);
