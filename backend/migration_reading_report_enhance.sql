-- 阅读报告增强功能迁移脚本
-- 添加分享次数字段和更新时间字段

ALTER TABLE reading_report
ADD COLUMN share_count INT DEFAULT 0 COMMENT '分享次数' AFTER report_data,
ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER created_at;

-- 新增阅读报告管理权限
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(130, 'reading_report_mgmt', '阅读报告管理', 1, NULL, '/reading-reports', 14),
(131, 'reading_report:view', '查看阅读报告', 2, 130, '/api/admin/reading-reports', 1),
(132, 'reading_report:stats', '查看阅读报告统计', 2, 130, '/api/admin/reading-reports/stats', 2)
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- 超级管理员：阅读报告管理权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE id >= 130
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 运营：阅读报告管理权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 130), (2, 131), (2, 132)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 只读审计：查看阅读报告权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(3, 130), (3, 131), (3, 132)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
