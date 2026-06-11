-- PDF阅读性能优化 数据库迁移脚本

-- 1. 为书籍表添加封面缩略图和预渲染相关字段
ALTER TABLE book 
ADD COLUMN IF NOT EXISTS cover_thumbnail VARCHAR(500) DEFAULT '' COMMENT '封面缩略图路径' AFTER file_path,
ADD COLUMN IF NOT EXISTS pre_render_status TINYINT DEFAULT 0 COMMENT '预渲染状态：0-未开始 1-进行中 2-已完成 3-失败' AFTER page_count,
ADD COLUMN IF NOT EXISTS pre_rendered_pages INT DEFAULT 0 COMMENT '已预渲染页数' AFTER pre_render_status,
ADD COLUMN IF NOT EXISTS pre_render_error VARCHAR(500) DEFAULT '' COMMENT '预渲染错误信息' AFTER pre_rendered_pages;

-- 2. 创建系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(20) DEFAULT 'string' COMMENT '配置类型：string/number/boolean/json',
    description VARCHAR(500) DEFAULT '' COMMENT '配置描述',
    category VARCHAR(50) DEFAULT 'general' COMMENT '配置分类：general/pdf/reader',
    is_editable TINYINT DEFAULT 1 COMMENT '是否可编辑：0-否 1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key),
    INDEX idx_category (category)
) ENGINE=InnoDB COMMENT='系统配置表';

-- 3. 插入PDF渲染相关默认配置
INSERT INTO sys_config (config_key, config_value, config_type, description, category, is_editable) VALUES
('pdf.render.dpi', '150', 'number', 'PDF渲染DPI（分辨率），建议值：72-300', 'pdf', 1),
('pdf.thumbnail.dpi', '72', 'number', '缩略图渲染DPI，建议值：36-150', 'pdf', 1),
('pdf.prerender.pages', '10', 'number', '上传后预渲染页数，建议值：5-20', 'pdf', 1),
('pdf.prerender.enabled', 'true', 'boolean', '是否启用上传后异步预渲染', 'pdf', 1),
('pdf.cache.enabled', 'true', 'boolean', '是否启用PDF页面缓存', 'pdf', 1),
('pdf.cache.expire_hours', '24', 'number', 'PDF页面缓存过期时间（小时）', 'pdf', 1),
('reader.preload.offset', '2', 'number', '阅读器预加载偏移页数，当前页±N页', 'reader', 1),
('reader.preload.enabled', 'true', 'boolean', '是否启用阅读器预加载', 'reader', 1),
('reader.skeleton.enabled', 'true', 'boolean', '弱网时是否显示骨架屏', 'reader', 1),
('reader.weaknetwork.threshold_kb', '50', 'number', '弱网阈值（KB/s），低于此值判定为弱网', 'reader', 1)
ON DUPLICATE KEY UPDATE 
    config_value = VALUES(config_value),
    config_type = VALUES(config_type),
    description = VALUES(description),
    category = VALUES(category),
    is_editable = VALUES(is_editable);

-- 4. 为已有PDF书籍生成缩略图（后台任务处理，此处仅标记状态）
UPDATE book 
SET pre_render_status = 0 
WHERE book_format = 'pdf' AND (pre_render_status IS NULL OR pre_render_status = 0);
