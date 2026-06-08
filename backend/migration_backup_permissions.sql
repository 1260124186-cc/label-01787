-- ============================================================
-- 数据库迁移脚本：备份管理功能
-- 执行时机：部署后一次性执行
-- 幂等性：支持重复执行，不会报错
-- ============================================================

USE xiaoan_bookstore;

-- ------------------------------------------------------------
-- 1. 创建备份任务表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS backup_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    task_type TINYINT NOT NULL COMMENT '1-导出 2-导入',
    status TINYINT NOT NULL COMMENT '0-待处理 1-处理中 2-已完成 3-失败',
    file_name VARCHAR(255) DEFAULT '' COMMENT '备份文件名',
    file_path VARCHAR(500) DEFAULT '' COMMENT '备份文件路径',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    book_count INT DEFAULT 0 COMMENT '书籍数量',
    annotation_count INT DEFAULT 0 COMMENT '批注数量',
    record_count INT DEFAULT 0 COMMENT '阅读记录数量',
    category_count INT DEFAULT 0 COMMENT '分类数量',
    progress INT DEFAULT 0 COMMENT '进度(0-100)',
    error_message TEXT COMMENT '错误信息',
    expired_at DATETIME DEFAULT NULL COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='备份任务表';

-- ------------------------------------------------------------
-- 2. 新增备份管理权限（如果不存在）
-- ------------------------------------------------------------
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(80, 'backup_mgmt', '备份管理', 1, NULL, '/backups', 9),
(81, 'backup:view', '查看备份任务', 2, 80, '/api/admin/backups', 1),
(82, 'backup:export', '导出备份', 2, 80, '/api/admin/backups/*/export', 2),
(83, 'backup:delete', '删除备份', 2, 80, '/api/admin/backups/*/delete', 3),
(84, 'storage:view', '查看存储统计', 2, 80, '/api/admin/storage', 4)
ON DUPLICATE KEY UPDATE
    code = VALUES(code),
    name = VALUES(name),
    type = VALUES(type),
    parent_id = VALUES(parent_id),
    path = VALUES(path),
    sort_order = VALUES(sort_order);

-- ------------------------------------------------------------
-- 3. 为超级管理员分配备份管理权限
-- ------------------------------------------------------------
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE id >= 80
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ------------------------------------------------------------
-- 4. 为运营角色分配备份管理权限
-- ------------------------------------------------------------
INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 80), (2, 81), (2, 82), (2, 83), (2, 84)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ------------------------------------------------------------
-- 5. 为只读审计角色分配查看备份和存储权限
-- ------------------------------------------------------------
INSERT INTO role_permission (role_id, permission_id) VALUES
(3, 80), (3, 81), (3, 84)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ------------------------------------------------------------
-- 执行完成提示
-- ------------------------------------------------------------
SELECT '备份管理数据库迁移完成！' AS message;
SELECT
    (SELECT COUNT(*) FROM backup_task) AS backup_task_count,
    (SELECT COUNT(*) FROM permission WHERE code LIKE 'backup%' OR code LIKE 'storage%') AS backup_permission_count;
