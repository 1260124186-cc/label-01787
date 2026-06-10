package com.xiaoan.bookstore.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("开始执行数据库迁移检查...");

        try {
            migrateBackupTaskTable();
            migrateBackupPermissions();
            log.info("数据库迁移检查完成");
        } catch (Exception e) {
            log.error("数据库迁移失败: {}", e.getMessage(), e);
        }
    }

    private void migrateBackupTaskTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'backup_task'",
                Integer.class
        );

        if (count != null && count > 0) {
            log.info("backup_task 表已存在，跳过创建");
            return;
        }

        log.info("backup_task 表不存在(count={})，开始创建...", count);
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB COMMENT='备份任务表'
                """);
        log.info("backup_task 表创建成功");
    }

    private void migrateBackupPermissions() {
        String[] permissions = {
                "(80, 'backup_mgmt', '备份管理', 1, NULL, '/backups', 9)",
                "(81, 'backup:view', '查看备份任务', 2, 80, '/api/admin/backups', 1)",
                "(82, 'backup:export', '导出备份', 2, 80, '/api/admin/backups/*/export', 2)",
                "(83, 'backup:delete', '删除备份', 2, 80, '/api/admin/backups/*/delete', 3)",
                "(84, 'storage:view', '查看存储统计', 2, 80, '/api/admin/storage', 4)"
        };

        for (String perm : permissions) {
            try {
                jdbcTemplate.execute("""
                    INSERT INTO permission (id, code, name, type, parent_id, path, sort_order)
                    VALUES
                    """ + perm + """
                    ON DUPLICATE KEY UPDATE
                        code = VALUES(code),
                        name = VALUES(name),
                        type = VALUES(type),
                        parent_id = VALUES(parent_id),
                        path = VALUES(path),
                        sort_order = VALUES(sort_order)
                    """);
            } catch (Exception e) {
                log.warn("权限插入失败（可能已存在）: {}", e.getMessage());
            }
        }
        log.info("备份管理权限初始化完成");

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id)
                SELECT 1, id FROM permission WHERE id >= 80
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("超级管理员备份权限分配完成");
        } catch (Exception e) {
            log.warn("超级管理员权限分配失败: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id) VALUES
                (2, 80), (2, 81), (2, 82), (2, 83), (2, 84)
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("运营角色备份权限分配完成");
        } catch (Exception e) {
            log.warn("运营角色权限分配失败: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id) VALUES
                (3, 80), (3, 81), (3, 84)
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("只读审计角色备份权限分配完成");
        } catch (Exception e) {
            log.warn("只读审计角色权限分配失败: {}", e.getMessage());
        }
    }
}
