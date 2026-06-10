-- 积分与会员体系 迁移脚本
-- 执行前请备份数据库

-- 1. 用户会员状态表新增 AI 使用计数字段
ALTER TABLE user_membership
    ADD COLUMN ai_used_today INT DEFAULT 0 COMMENT '今日AI使用次数' AFTER extra_storage,
    ADD COLUMN ai_usage_date VARCHAR(10) DEFAULT '' COMMENT 'AI使用计数日期(yyyy-MM-dd)' AFTER ai_used_today;

-- 2. 订单表新增存储包大小字段
ALTER TABLE `order`
    ADD COLUMN storage_gb INT DEFAULT NULL COMMENT '存储包大小(GB，order_type=2时有效)' AFTER wx_transaction_id;
