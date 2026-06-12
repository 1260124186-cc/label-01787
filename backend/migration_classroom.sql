SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS classroom (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '班级名称',
    description VARCHAR(500) DEFAULT '' COMMENT '班级描述',
    teacher_id BIGINT NOT NULL COMMENT '教师用户ID',
    invite_code VARCHAR(6) NOT NULL COMMENT '邀请码',
    member_count INT NOT NULL DEFAULT 1 COMMENT '成员数量',
    institution VARCHAR(200) DEFAULT '' COMMENT '所属机构',
    grade_level VARCHAR(50) DEFAULT '' COMMENT '年级/级别',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-已关闭 1-正常',
    ban_reason VARCHAR(500) DEFAULT '' COMMENT '关闭原因',
    banned_at DATETIME DEFAULT NULL COMMENT '关闭时间',
    banned_by BIGINT DEFAULT NULL COMMENT '关闭操作人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_invite_code (invite_code),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='班级表';

CREATE TABLE IF NOT EXISTS classroom_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    classroom_id BIGINT NOT NULL COMMENT '班级ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role TINYINT NOT NULL DEFAULT 2 COMMENT '1-教师 2-学生',
    student_no VARCHAR(50) DEFAULT '' COMMENT '学号',
    real_name VARCHAR(100) DEFAULT '' COMMENT '真实姓名',
    joined_at DATETIME NOT NULL COMMENT '加入时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_classroom_user (classroom_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_classroom_id (classroom_id)
) ENGINE=InnoDB COMMENT='班级成员表';

CREATE TABLE IF NOT EXISTS classroom_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    classroom_id BIGINT NOT NULL COMMENT '班级ID',
    teacher_id BIGINT NOT NULL COMMENT '布置教师ID',
    book_id BIGINT DEFAULT NULL COMMENT '书籍ID(可空表示非平台书目)',
    book_title VARCHAR(200) NOT NULL COMMENT '书名',
    book_author VARCHAR(200) DEFAULT '' COMMENT '作者',
    start_page INT NOT NULL DEFAULT 0 COMMENT '起始页码',
    end_page INT NOT NULL DEFAULT 0 COMMENT '结束页码',
    deadline DATETIME NOT NULL COMMENT '截止日期',
    description VARCHAR(1000) DEFAULT '' COMMENT '作业说明',
    total_score INT NOT NULL DEFAULT 100 COMMENT '满分分值',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-草稿 1-进行中 2-已截止',
    submit_count INT NOT NULL DEFAULT 0 COMMENT '提交人数',
    graded_count INT NOT NULL DEFAULT 0 COMMENT '已批改人数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_classroom_id (classroom_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_status (status),
    INDEX idx_deadline (deadline)
) ENGINE=InnoDB COMMENT='班级作业表';

CREATE TABLE IF NOT EXISTS classroom_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL COMMENT '作业ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    reading_duration INT NOT NULL DEFAULT 0 COMMENT '阅读时长(秒)',
    annotation_summary TEXT COMMENT '批注摘要',
    page_progress INT NOT NULL DEFAULT 0 COMMENT '阅读页数进度',
    proof_images VARCHAR(1000) DEFAULT '' COMMENT '阅读证明图片(JSON数组)',
    submit_at DATETIME DEFAULT NULL COMMENT '提交时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-未提交 1-已提交 2-已批改',
    score INT DEFAULT NULL COMMENT '评分',
    teacher_comment VARCHAR(1000) DEFAULT '' COMMENT '教师评语',
    graded_at DATETIME DEFAULT NULL COMMENT '批改时间',
    graded_by BIGINT DEFAULT NULL COMMENT '批改教师ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_assignment_student (assignment_id, student_id),
    INDEX idx_student_id (student_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='班级作业提交表';

CREATE TABLE IF NOT EXISTS classroom_reminder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL COMMENT '作业ID',
    teacher_id BIGINT NOT NULL COMMENT '催交教师ID',
    student_id BIGINT NOT NULL COMMENT '被催交学生ID',
    message VARCHAR(500) DEFAULT '' COMMENT '催交消息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_assignment_id (assignment_id),
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB COMMENT='班级催交记录表';

ALTER TABLE user ADD COLUMN IF NOT EXISTS is_teacher TINYINT NOT NULL DEFAULT 0 COMMENT '是否教师 0-否 1-是';
ALTER TABLE user ADD COLUMN IF NOT EXISTS teacher_verified TINYINT NOT NULL DEFAULT 0 COMMENT '教师认证状态 0-未认证 1-已认证';

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order) VALUES
(120, 'classroom_mgmt', '班级管理', 1, NULL, '/classrooms', 15),
(121, 'classroom:view', '查看班级列表', 2, 120, '/api/admin/classrooms', 1),
(122, 'classroom:ban', '关闭班级', 2, 120, '/api/admin/classrooms/*/ban', 2),
(123, 'assignment:view', '查看作业统计', 2, 120, '/api/admin/classrooms/*/assignments', 3)
ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE id >= 120 AND id <= 123
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 120), (2, 121), (2, 122), (2, 123)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO role_permission (role_id, permission_id) VALUES
(3, 120), (3, 121)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
