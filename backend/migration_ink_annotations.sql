-- ============================================================
-- 手写墨迹批注功能数据库迁移脚本
-- 包含 ink_stroke 表，用于存储矢量笔迹数据
-- ============================================================

CREATE TABLE IF NOT EXISTS `ink_stroke` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `book_id` BIGINT NOT NULL COMMENT '书籍ID',
  `page_num` INT NOT NULL COMMENT '页码',
  `stroke_id` VARCHAR(64) NOT NULL COMMENT '笔迹唯一标识UUID',
  `stroke_type` VARCHAR(32) DEFAULT 'pen' COMMENT '笔迹类型: pen/highlighter/eraser',
  `color` VARCHAR(16) DEFAULT '#000000' COMMENT '颜色',
  `line_width` DOUBLE DEFAULT 2.0 COMMENT '线宽',
  `opacity` DOUBLE DEFAULT 1.0 COMMENT '透明度',
  `points` LONGTEXT COMMENT '点数据 JSON: [[x,y,p],...',
  `bounding_box` VARCHAR(128) COMMENT '边界框: x,y,w,h',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_book_stroke` (`user_id`, `book_id`, `stroke_id`),
  KEY `idx_user_book_page` (`user_id`, `book_id`, `page_num`),
  KEY `idx_book_page` (`book_id`, `page_num`),
  KEY `idx_user_book` (`user_id`, `book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手写墨迹笔迹表';
