-- 批注功能增强：标签、置顶、颜色标记
-- 为 annotation 表添加新字段

ALTER TABLE annotation
ADD COLUMN tags VARCHAR(500) DEFAULT '' COMMENT '标签，逗号分隔',
ADD COLUMN is_pinned TINYINT DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
ADD COLUMN color VARCHAR(20) DEFAULT 'yellow' COMMENT '标记颜色 yellow/green/pink',
ADD INDEX idx_user_pinned (user_id, is_pinned);
