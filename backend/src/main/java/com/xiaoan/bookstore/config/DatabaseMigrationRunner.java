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
            migrateBookPerformanceFields();
            migrateSysConfigTable();
            migrateConfigPermissions();
            migrateFamilyTables();
            migrateFamilyPermissions();
            migrateReadingSeasonTables();
            migrateReadingSeasonPermissions();
            log.info("数据库迁移检查完成");
        } catch (Exception e) {
            log.error("数据库迁移失败: {}", e.getMessage(), e);
        }
    }

    private void migrateBookPerformanceFields() {
        try {
            Integer colCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = 'book' AND column_name = 'cover_thumbnail'",
                    Integer.class
            );
            if (colCount == null || colCount == 0) {
                log.info("添加书籍封面缩略图和预渲染相关字段...");
                jdbcTemplate.execute("""
                    ALTER TABLE book
                    ADD COLUMN IF NOT EXISTS cover_thumbnail VARCHAR(500) DEFAULT '' COMMENT '封面缩略图路径' AFTER file_path,
                    ADD COLUMN IF NOT EXISTS pre_render_status TINYINT DEFAULT 0 COMMENT '预渲染状态' AFTER page_count,
                    ADD COLUMN IF NOT EXISTS pre_rendered_pages INT DEFAULT 0 COMMENT '已预渲染页数' AFTER pre_render_status,
                    ADD COLUMN IF NOT EXISTS pre_render_error VARCHAR(500) DEFAULT '' COMMENT '预渲染错误信息' AFTER pre_rendered_pages
                    """);
                log.info("书籍字段添加成功");
            } else {
                log.info("书籍性能优化字段已存在，跳过");
            }
        } catch (Exception e) {
            log.warn("添加书籍字段失败（可能已存在）: {}", e.getMessage());
        }
    }

    private void migrateSysConfigTable() {
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = DATABASE() AND table_name = 'sys_config'",
                    Integer.class
            );
            if (tableCount == null || tableCount == 0) {
                log.info("创建系统配置表...");
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS sys_config (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
                        config_value TEXT COMMENT '配置值',
                        config_type VARCHAR(20) DEFAULT 'string' COMMENT '配置类型',
                        description VARCHAR(500) DEFAULT '' COMMENT '配置描述',
                        category VARCHAR(50) DEFAULT 'general' COMMENT '配置分类',
                        is_editable TINYINT DEFAULT 1 COMMENT '是否可编辑',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB COMMENT='系统配置表'
                    """);
                log.info("系统配置表创建成功");
            } else {
                log.info("系统配置表已存在，跳过创建");
            }

            log.info("初始化系统配置默认值...");
            String[][] defaultConfigs = {
                    {"pdf.render.dpi", "150", "number", "PDF渲染DPI", "pdf", "1"},
                    {"pdf.thumbnail.dpi", "72", "number", "缩略图渲染DPI", "pdf", "1"},
                    {"pdf.prerender.pages", "10", "number", "上传后预渲染页数", "pdf", "1"},
                    {"pdf.prerender.enabled", "true", "boolean", "是否启用PDF上传后预渲染", "pdf", "1"},
                    {"pdf.cache.enabled", "true", "boolean", "是否启用PDF页面缓存", "pdf", "1"},
                    {"pdf.cache.expire_hours", "72", "number", "PDF缓存过期时间(小时)", "pdf", "1"},
                    {"reader.preload.offset", "2", "number", "阅读器预加载偏移页数", "reader", "1"},
                    {"reader.preload.enabled", "true", "boolean", "是否启用阅读器预加载", "reader", "1"},
                    {"reader.skeleton.enabled", "true", "boolean", "弱网时是否显示骨架屏", "reader", "1"},
                    {"reader.weaknetwork.threshold_kb", "50", "number", "弱网判断阈值(KB/s)", "reader", "1"}
            };

            for (String[] config : defaultConfigs) {
                try {
                    jdbcTemplate.update("""
                        INSERT INTO sys_config (config_key, config_value, config_type, description, category, is_editable)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            config_value = VALUES(config_value),
                            config_type = VALUES(config_type),
                            description = VALUES(description),
                            category = VALUES(category),
                            is_editable = VALUES(is_editable)
                        """,
                        config[0],
                        config[1],
                        config[2],
                        config[3],
                        config[4],
                        Integer.parseInt(config[5])
                    );
                } catch (Exception e) {
                    log.warn("配置项初始化失败（可能已存在）: {} - {}", config[0], e.getMessage());
                }
            }
            log.info("系统配置默认值初始化完成");
        } catch (Exception e) {
            log.error("迁移系统配置表失败: {}", e.getMessage());
        }
    }

    private void migrateConfigPermissions() {
        String[] permissions = {
                "(90, 'config_mgmt', '系统配置', 1, NULL, '/configs', 12)",
                "(91, 'config:view', '查看系统配置', 2, 90, '/api/admin/configs', 1)",
                "(92, 'config:update', '修改系统配置', 2, 90, '/api/admin/configs', 2)"
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
        log.info("系统配置权限初始化完成");

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id)
                SELECT 1, id FROM permission WHERE id >= 90 AND id <= 92
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("超级管理员系统配置权限分配完成");
        } catch (Exception e) {
            log.warn("超级管理员权限分配失败: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id) VALUES
                (2, 90), (2, 91), (2, 92)
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("运营角色系统配置权限分配完成");
        } catch (Exception e) {
            log.warn("运营角色权限分配失败: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id) VALUES
                (3, 90), (3, 91)
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("只读审计角色系统配置权限分配完成");
        } catch (Exception e) {
            log.warn("只读审计角色权限分配失败: {}", e.getMessage());
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

    private void migrateFamilyTables() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'family'",
                    Integer.class
            );
            if (count != null && count > 0) {
                log.info("家庭相关表已存在，跳过创建");
                return;
            }

            log.info("创建家庭子账号相关表...");
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS family (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL COMMENT '家庭名称',
                    invite_code VARCHAR(20) NOT NULL UNIQUE COMMENT '邀请码',
                    owner_id BIGINT NOT NULL COMMENT '家长(主账号)用户ID',
                    member_count INT DEFAULT 1 COMMENT '成员数量',
                    max_members INT DEFAULT 6 COMMENT '最大成员数',
                    shared_storage BIGINT DEFAULT 0 COMMENT '共享存储池大小(字节)',
                    status TINYINT DEFAULT 1 COMMENT '0-已解散 1-正常',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_owner_id (owner_id),
                    INDEX idx_status (status),
                    INDEX idx_invite_code (invite_code)
                ) ENGINE=InnoDB COMMENT='家庭表'
                """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS family_member (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    family_id BIGINT NOT NULL COMMENT '家庭ID',
                    user_id BIGINT NOT NULL COMMENT '用户ID',
                    role TINYINT DEFAULT 2 COMMENT '1-家长 2-子女',
                    nickname VARCHAR(50) DEFAULT '' COMMENT '家庭内昵称',
                    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_family_user (family_id, user_id),
                    INDEX idx_family_id (family_id),
                    INDEX idx_user_id (user_id)
                ) ENGINE=InnoDB COMMENT='家庭成员表'
                """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS family_shared_book (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    family_id BIGINT NOT NULL COMMENT '家庭ID',
                    book_id BIGINT NOT NULL COMMENT '书籍ID',
                    shared_by BIGINT NOT NULL COMMENT '共享操作人ID',
                    shared_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '共享时间',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_family_book (family_id, book_id),
                    INDEX idx_family_id (family_id),
                    INDEX idx_book_id (book_id)
                ) ENGINE=InnoDB COMMENT='家庭共享书架表'
                """);

            log.info("家庭子账号相关表创建成功");
        } catch (Exception e) {
            log.error("创建家庭子账号相关表失败: {}", e.getMessage());
        }
    }

    private void migrateFamilyPermissions() {
        String[] permissions = {
                "(130, 'family_mgmt', '家庭管理', 1, NULL, '/families', 14)",
                "(131, 'family:view', '查看家庭', 2, 130, '/api/admin/families', 1)"
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
        log.info("家庭管理权限初始化完成");

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id)
                SELECT 1, id FROM permission WHERE id >= 130 AND id <= 131
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("超级管理员家庭管理权限分配完成");
        } catch (Exception e) {
            log.warn("超级管理员权限分配失败: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id) VALUES
                (2, 130), (2, 131)
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("运营角色家庭管理权限分配完成");
        } catch (Exception e) {
            log.warn("运营角色权限分配失败: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id) VALUES
                (3, 130), (3, 131)
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("只读审计角色家庭管理权限分配完成");
        } catch (Exception e) {
            log.warn("只读审计角色权限分配失败: {}", e.getMessage());
        }
    }

    private void migrateReadingSeasonTables() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'reading_season'",
                    Integer.class
            );
            if (count != null && count > 0) {
                log.info("阅读赛季相关表已存在，跳过创建");
                return;
            }

            log.info("创建阅读赛季相关表...");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS reading_season (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    title VARCHAR(100) NOT NULL COMMENT '赛季标题',
                    subtitle VARCHAR(200) DEFAULT '' COMMENT '赛季副标题',
                    cover_image VARCHAR(500) DEFAULT '' COMMENT '封面图',
                    description TEXT COMMENT '赛季描述',
                    season_type TINYINT DEFAULT 1 COMMENT '1-挑战赛 2-阅读马拉松 3-读书月',
                    status TINYINT DEFAULT 0 COMMENT '0-草稿 1-报名中 2-进行中 3-已结束 4-已取消',
                    start_date DATE NOT NULL COMMENT '赛季开始日期',
                    end_date DATE NOT NULL COMMENT '赛季结束日期',
                    signup_start DATE DEFAULT NULL COMMENT '报名开始日期',
                    signup_end DATE DEFAULT NULL COMMENT '报名截止日期',
                    duration_days INT NOT NULL COMMENT '赛季天数',
                    daily_min_duration INT DEFAULT 600 COMMENT '每日最低阅读时长(秒)',
                    daily_max_duration INT DEFAULT 28800 COMMENT '每日最高有效阅读时长(秒)',
                    max_participants INT DEFAULT 0 COMMENT '最大参与人数，0-不限',
                    points_reward INT DEFAULT 0 COMMENT '完成赛季奖励积分',
                    badge_icon VARCHAR(100) DEFAULT '' COMMENT '赛季徽章图标标识',
                    badge_name VARCHAR(100) DEFAULT '' COMMENT '赛季徽章名称',
                    rules TEXT COMMENT '赛季规则说明',
                    prize_config TEXT COMMENT '奖品配置JSON',
                    cheat_threshold_speed INT DEFAULT 7200 COMMENT '作弊检测：每小时最高阅读秒数',
                    cheat_threshold_streak INT DEFAULT 168 COMMENT '作弊检测：连续阅读最长小时数',
                    cheat_auto_flag TINYINT DEFAULT 1 COMMENT '是否自动标记可疑记录',
                    published_at DATETIME DEFAULT NULL COMMENT '发布时间',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_status (status),
                    INDEX idx_start_date (start_date),
                    INDEX idx_end_date (end_date)
                ) ENGINE=InnoDB COMMENT='阅读赛季配置表'
                """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS season_participant (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    season_id BIGINT NOT NULL COMMENT '赛季ID',
                    user_id BIGINT NOT NULL COMMENT '用户ID',
                    status TINYINT DEFAULT 1 COMMENT '0-已退出 1-进行中 2-已完成 3-未完成 4-作弊取消资格',
                    signup_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
                    qualified_days INT DEFAULT 0 COMMENT '达标天数',
                    total_duration BIGINT DEFAULT 0 COMMENT '总阅读时长(秒)',
                    total_books INT DEFAULT 0 COMMENT '阅读书籍数',
                    streak_days INT DEFAULT 0 COMMENT '连续达标天数',
                    max_streak_days INT DEFAULT 0 COMMENT '最大连续达标天数',
                    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
                    points_awarded INT DEFAULT 0 COMMENT '已发放积分数',
                    badge_awarded TINYINT DEFAULT 0 COMMENT '是否已颁发徽章',
                    prize_claimed TINYINT DEFAULT 0 COMMENT '奖品是否已领取',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_season_user (season_id, user_id),
                    INDEX idx_season_id (season_id),
                    INDEX idx_user_id (user_id),
                    INDEX idx_status (status),
                    INDEX idx_season_streak (season_id, streak_days DESC)
                ) ENGINE=InnoDB COMMENT='赛季参与者表'
                """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS season_daily_record (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    season_id BIGINT NOT NULL COMMENT '赛季ID',
                    user_id BIGINT NOT NULL COMMENT '用户ID',
                    record_date DATE NOT NULL COMMENT '记录日期',
                    duration INT DEFAULT 0 COMMENT '当日阅读时长(秒)',
                    book_count INT DEFAULT 0 COMMENT '当日阅读书籍数',
                    is_qualified TINYINT DEFAULT 0 COMMENT '是否达标',
                    is_flagged TINYINT DEFAULT 0 COMMENT '是否被标记可疑',
                    flag_reason VARCHAR(500) DEFAULT '' COMMENT '标记原因',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_season_user_date (season_id, user_id, record_date),
                    INDEX idx_season_date (season_id, record_date),
                    INDEX idx_user_date (user_id, record_date),
                    INDEX idx_flagged (is_flagged)
                ) ENGINE=InnoDB COMMENT='赛季每日记录表'
                """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS season_badge (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL COMMENT '用户ID',
                    season_id BIGINT NOT NULL COMMENT '赛季ID',
                    badge_type VARCHAR(50) NOT NULL COMMENT '徽章类型',
                    badge_name VARCHAR(100) NOT NULL COMMENT '徽章名称',
                    badge_icon VARCHAR(100) NOT NULL COMMENT '徽章图标',
                    earned_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
                    INDEX idx_user_id (user_id),
                    INDEX idx_season_id (season_id),
                    UNIQUE KEY uk_user_season_type (user_id, season_id, badge_type)
                ) ENGINE=InnoDB COMMENT='赛季徽章表'
                """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS season_cheat_detection (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    season_id BIGINT NOT NULL COMMENT '赛季ID',
                    user_id BIGINT NOT NULL COMMENT '用户ID',
                    detection_date DATE NOT NULL COMMENT '检测日期',
                    detection_type TINYINT NOT NULL COMMENT '1-阅读速度异常 2-连续时长异常 3-数据突变 4-设备异常',
                    detection_detail TEXT COMMENT '检测详情JSON',
                    severity TINYINT DEFAULT 1 COMMENT '1-轻微 2-中等 3-严重',
                    status TINYINT DEFAULT 0 COMMENT '0-待审核 1-确认作弊 2-误报 3-已处理',
                    handled_by BIGINT DEFAULT NULL COMMENT '处理人ID',
                    handle_result VARCHAR(500) DEFAULT '' COMMENT '处理结果',
                    handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_season_id (season_id),
                    INDEX idx_user_id (user_id),
                    INDEX idx_status (status),
                    INDEX idx_detection_date (detection_date)
                ) ENGINE=InnoDB COMMENT='赛季作弊检测记录表'
                """);

            log.info("阅读赛季相关表创建成功");
        } catch (Exception e) {
            log.error("创建阅读赛季相关表失败: {}", e.getMessage());
        }
    }

    private void migrateReadingSeasonPermissions() {
        String[] permissions = {
                "(140, 'season_mgmt', '赛季管理', 1, NULL, '/seasons', 15)",
                "(141, 'season:view', '查看赛季', 2, 140, '/api/admin/seasons', 1)",
                "(142, 'season:create', '创建赛季', 2, 140, '/api/admin/seasons', 2)",
                "(143, 'season:update', '编辑赛季', 2, 140, '/api/admin/seasons', 3)",
                "(144, 'season:delete', '删除赛季', 2, 140, '/api/admin/seasons', 4)",
                "(145, 'season:prize', '奖品发放', 2, 140, '/api/admin/seasons/prize', 5)",
                "(146, 'season:cheat', '作弊审核', 2, 140, '/api/admin/seasons/cheat', 6)"
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
        log.info("赛季管理权限初始化完成");

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id) VALUES
                (1, 140), (1, 141), (1, 142), (1, 143), (1, 144), (1, 145), (1, 146)
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("超级管理员赛季管理权限分配完成");
        } catch (Exception e) {
            log.warn("超级管理员权限分配失败: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id) VALUES
                (2, 140), (2, 141), (2, 142), (2, 143), (2, 145), (2, 146)
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("运营角色赛季管理权限分配完成");
        } catch (Exception e) {
            log.warn("运营角色权限分配失败: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                INSERT INTO role_permission (role_id, permission_id) VALUES
                (3, 140), (3, 141)
                ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
                """);
            log.info("只读审计角色赛季管理权限分配完成");
        } catch (Exception e) {
            log.warn("只读审计角色权限分配失败: {}", e.getMessage());
        }
    }
}
