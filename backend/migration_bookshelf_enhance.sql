-- 书籍元数据与书架体验增强 数据库迁移
-- 增加回收站软删除、最近阅读时间、分类颜色等字段

-- 1. book 表增加字段
ALTER TABLE book 
ADD COLUMN last_read_at DATETIME DEFAULT NULL COMMENT '最近阅读时间' AFTER status,
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间(回收站)' AFTER last_read_at,
ADD INDEX idx_deleted_at (deleted_at),
ADD INDEX idx_last_read_at (last_read_at);

-- 2. category 表增加颜色字段
ALTER TABLE category 
ADD COLUMN color VARCHAR(20) DEFAULT '' COMMENT '分类标签颜色' AFTER sort_order;
