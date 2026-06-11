-- 分类体系统一与排序功能：增加 color/icon 字段
ALTER TABLE category ADD COLUMN color VARCHAR(20) DEFAULT '' COMMENT '分类颜色(十六进制)' AFTER name;
ALTER TABLE category ADD COLUMN icon VARCHAR(50) DEFAULT '' COMMENT '分类图标标识' AFTER color;
