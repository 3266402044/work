-- ============================================================
-- ClassPulse 课堂签到与互动系统 - MySQL 8 建表脚本
-- 用法: mysql -uroot -p < schema.sql
--       CREATE DATABASE classpulse DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--       USE classpulse;
--       SOURCE schema.sql;
-- 说明: 演示数据由后端 DataInitializer 自动写入(首次启动时), 无需手工执行 data.sql。
-- ============================================================
CREATE DATABASE IF NOT EXISTS classpulse DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE classpulse;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL COMMENT '登录名',
    password_hash VARCHAR(120) NOT NULL COMMENT 'BCrypt 密码哈希',
    role VARCHAR(16) NOT NULL COMMENT 'STUDENT/TEACHER/ADMIN',
    real_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32),
    avatar_url VARCHAR(255),
    enabled TINYINT NOT NULL DEFAULT 1,
    related_id BIGINT NULL COMMENT '关联学生/教师档案ID',
    last_login_time DATETIME NULL,
    create_time DATETIME NOT NULL,
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS class_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    grade VARCHAR(32),
    major VARCHAR(64),
    advisor_id BIGINT NULL,
    create_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级';

CREATE TABLE IF NOT EXISTS teacher (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    teacher_no VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    department VARCHAR(64),
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_teacher_no (teacher_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师档案';

CREATE TABLE IF NOT EXISTS student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_no VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    class_id BIGINT NULL,
    user_id BIGINT NULL,
    create_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_student_no (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息';

CREATE TABLE IF NOT EXISTS course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_no VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    teacher_id BIGINT NULL,
    semester VARCHAR(32),
    credit DECIMAL(4,1),
    cover_color VARCHAR(16),
    classroom VARCHAR(128),
    longitude DECIMAL(10,7),
    latitude DECIMAL(10,7),
    default_radius INT DEFAULT 150,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_course_no (course_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程';

CREATE TABLE IF NOT EXISTS course_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    UNIQUE KEY uk_course_class (course_id, class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程-班级关系';

CREATE TABLE IF NOT EXISTS sign_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(128) NOT NULL,
    teacher_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    closed_at DATETIME NULL COMMENT '教师提前关闭时间',
    course_id BIGINT NULL,
    class_id BIGINT NULL,
    sign_modes VARCHAR(64) NOT NULL COMMENT 'CLICK,QR,LOCATION,PASSCODE',
    late_time DATETIME NULL,
    location_name VARCHAR(128) NULL,
    longitude DECIMAL(10,7) NULL,
    latitude DECIMAL(10,7) NULL,
    radius_meters INT NULL DEFAULT 150,
    passcode_hash VARCHAR(255) NULL,
    KEY idx_task_teacher_time (teacher_id, start_time),
    KEY idx_task_class_time (class_id, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到任务(不持久化状态)';

CREATE TABLE IF NOT EXISTS sign_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    sign_time DATETIME NOT NULL,
    status VARCHAR(16) NOT NULL COMMENT 'NORMAL/LATE/MANUAL/LEAVE',
    method VARCHAR(16) NOT NULL COMMENT 'CLICK/QR/LOCATION/PASSCODE/MANUAL',
    longitude DECIMAL(10,7) NULL,
    latitude DECIMAL(10,7) NULL,
    distance_meters DECIMAL(10,2) NULL,
    remark VARCHAR(255) NULL,
    operator_id BIGINT NULL,
    create_time DATETIME NOT NULL,
    UNIQUE KEY uk_task_student (task_id, student_id),
    KEY idx_task_id (task_id),
    KEY idx_student_id (student_id, sign_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录';

CREATE TABLE IF NOT EXISTS leave_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    proof_url VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT NULL,
    review_time DATETIME NULL,
    remark VARCHAR(255),
    create_time DATETIME NOT NULL,
    KEY idx_leave_student (student_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请';

CREATE TABLE IF NOT EXISTS sign_qr_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    token_hash VARCHAR(120) NOT NULL,
    expired_at DATETIME NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    KEY idx_qr_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二维码令牌(仅存哈希)';

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    module VARCHAR(32),
    action VARCHAR(64),
    target_id BIGINT,
    detail VARCHAR(1000),
    ip VARCHAR(64),
    create_time DATETIME NOT NULL,
    KEY idx_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

CREATE TABLE IF NOT EXISTS roll_call_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    teacher_id BIGINT NOT NULL,
    candidate_scope VARCHAR(16) NOT NULL COMMENT 'ALL/SIGNED/UNSIGNED/CUSTOM',
    draw_count INT NOT NULL,
    anti_repeat_rule VARCHAR(32) NULL COMMENT 'NONE/SESSION/DAYS7/LAST_N',
    exclude_leave TINYINT DEFAULT 1,
    task_id BIGINT NULL COMMENT '候选范围关联的签到任务',
    custom_student_ids VARCHAR(1000) NULL COMMENT '自定义候选学生ID列表',
    response_deadline_seconds INT DEFAULT 30,
    status VARCHAR(16) NOT NULL COMMENT 'RUNNING/FINISHED/CANCELED',
    seed BIGINT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    KEY idx_rollcall_course (course_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随机点名会话';

CREATE TABLE IF NOT EXISTS roll_call_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    draw_order INT NOT NULL,
    result VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    performance VARCHAR(16) NULL,
    drawn_at DATETIME NOT NULL,
    responded_at DATETIME NULL,
    remark VARCHAR(255),
    UNIQUE KEY uk_rollcall_session_student (session_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点名结果';

CREATE TABLE IF NOT EXISTS quiz (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(16) NOT NULL COMMENT 'SINGLE/MULTIPLE/TRUE_FALSE',
    options_json TEXT,
    correct_answer VARCHAR(255),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    anonymous TINYINT DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    explanation VARCHAR(1000),
    create_time DATETIME NOT NULL,
    KEY idx_quiz_course (course_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课堂小测/投票';

CREATE TABLE IF NOT EXISTS quiz_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quiz_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    answer_json VARCHAR(255) NOT NULL,
    is_correct TINYINT NULL,
    answer_time DATETIME NOT NULL,
    UNIQUE KEY uk_quiz_student (quiz_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小测答案';

CREATE TABLE IF NOT EXISTS class_announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    pinned TINYINT DEFAULT 0,
    expire_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    KEY idx_ann_course (course_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课堂公告';

CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(1000),
    type VARCHAR(32),
    read_flag TINYINT NOT NULL DEFAULT 0,
    related_id BIGINT,
    create_time DATETIME NOT NULL,
    KEY idx_notify_user (user_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息中心';

CREATE TABLE IF NOT EXISTS race_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status VARCHAR(16) NOT NULL,
    create_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课堂抢答';

CREATE TABLE IF NOT EXISTS race_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    race_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    seq INT NOT NULL,
    create_time DATETIME NOT NULL,
    UNIQUE KEY uk_race_student (race_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢答记录';

CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(64) NOT NULL,
    config_value VARCHAR(255),
    description VARCHAR(255),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';
