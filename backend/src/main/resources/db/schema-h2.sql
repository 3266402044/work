-- ClassPulse H2 schema (MySQL 兼容模式, 开发环境默认使用)
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    role VARCHAR(16) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32),
    avatar_url VARCHAR(255),
    enabled TINYINT DEFAULT 1 NOT NULL,
    related_id BIGINT,
    last_login_time DATETIME,
    create_time DATETIME NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username ON sys_user(username);

CREATE TABLE IF NOT EXISTS class_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    grade VARCHAR(32),
    major VARCHAR(64),
    advisor_id BIGINT,
    create_time DATETIME NOT NULL,
    deleted TINYINT DEFAULT 0 NOT NULL
);

CREATE TABLE IF NOT EXISTS teacher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    teacher_no VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    department VARCHAR(64),
    deleted TINYINT DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_teacher_no ON teacher(teacher_no);

CREATE TABLE IF NOT EXISTS student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_no VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    class_id BIGINT,
    user_id BIGINT,
    create_time DATETIME NOT NULL,
    deleted TINYINT DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_student_no ON student(student_no);

CREATE TABLE IF NOT EXISTS course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_no VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    teacher_id BIGINT,
    semester VARCHAR(32),
    credit DECIMAL(4,1),
    cover_color VARCHAR(16),
    classroom VARCHAR(128),
    longitude DECIMAL(10,7),
    latitude DECIMAL(10,7),
    default_radius INT DEFAULT 150,
    deleted TINYINT DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_course_no ON course(course_no);

CREATE TABLE IF NOT EXISTS course_class (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_course_class ON course_class(course_id, class_id);

CREATE TABLE IF NOT EXISTS sign_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    teacher_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    closed_at DATETIME,
    course_id BIGINT,
    class_id BIGINT,
    sign_modes VARCHAR(64) NOT NULL,
    late_time DATETIME,
    location_name VARCHAR(128),
    longitude DECIMAL(10,7),
    latitude DECIMAL(10,7),
    radius_meters INT DEFAULT 150,
    passcode_hash VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_task_teacher_time ON sign_task(teacher_id, start_time);
CREATE INDEX IF NOT EXISTS idx_task_class_time ON sign_task(class_id, start_time);

CREATE TABLE IF NOT EXISTS sign_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    sign_time DATETIME NOT NULL,
    status VARCHAR(16) NOT NULL,
    method VARCHAR(16) NOT NULL,
    longitude DECIMAL(10,7),
    latitude DECIMAL(10,7),
    distance_meters DECIMAL(10,2),
    remark VARCHAR(255),
    operator_id BIGINT,
    create_time DATETIME NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_task_student ON sign_record(task_id, student_id);
CREATE INDEX IF NOT EXISTS idx_record_task ON sign_record(task_id);
CREATE INDEX IF NOT EXISTS idx_record_student_time ON sign_record(student_id, sign_time);

CREATE TABLE IF NOT EXISTS leave_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    proof_url VARCHAR(255),
    status VARCHAR(16) NOT NULL,
    reviewer_id BIGINT,
    review_time DATETIME,
    remark VARCHAR(255),
    create_time DATETIME NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_leave_student ON leave_request(student_id, create_time);

CREATE TABLE IF NOT EXISTS sign_qr_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    token_hash VARCHAR(120) NOT NULL,
    expired_at DATETIME NOT NULL,
    used_count INT DEFAULT 0 NOT NULL,
    create_time DATETIME NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_qr_task ON sign_qr_token(task_id);

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    module VARCHAR(32),
    action VARCHAR(64),
    target_id BIGINT,
    detail VARCHAR(1000),
    ip VARCHAR(64),
    create_time DATETIME NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_log_time ON operation_log(create_time);

CREATE TABLE IF NOT EXISTS roll_call_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    class_id BIGINT,
    teacher_id BIGINT NOT NULL,
    candidate_scope VARCHAR(16) NOT NULL,
    draw_count INT NOT NULL,
    anti_repeat_rule VARCHAR(32),
    exclude_leave TINYINT DEFAULT 1,
    task_id BIGINT,
    custom_student_ids VARCHAR(1000),
    response_deadline_seconds INT DEFAULT 30,
    status VARCHAR(16) NOT NULL,
    seed BIGINT,
    started_at DATETIME NOT NULL,
    ended_at DATETIME
);
CREATE INDEX IF NOT EXISTS idx_rollcall_course ON roll_call_session(course_id, started_at);

CREATE TABLE IF NOT EXISTS roll_call_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    draw_order INT NOT NULL,
    result VARCHAR(16) NOT NULL,
    performance VARCHAR(16),
    drawn_at DATETIME NOT NULL,
    responded_at DATETIME,
    remark VARCHAR(255)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_rollcall_session_student ON roll_call_record(session_id, student_id);

CREATE TABLE IF NOT EXISTS quiz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(16) NOT NULL,
    options_json TEXT,
    correct_answer VARCHAR(255),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    anonymous TINYINT DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    explanation VARCHAR(1000),
    create_time DATETIME NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_quiz_course ON quiz(course_id, create_time);

CREATE TABLE IF NOT EXISTS quiz_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    answer_json VARCHAR(255) NOT NULL,
    is_correct TINYINT,
    answer_time DATETIME NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_quiz_student ON quiz_answer(quiz_id, student_id);

CREATE TABLE IF NOT EXISTS class_announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    pinned TINYINT DEFAULT 0,
    expire_at DATETIME,
    create_time DATETIME NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ann_course ON class_announcement(course_id, create_time);

CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(1000),
    type VARCHAR(32),
    read_flag TINYINT DEFAULT 0 NOT NULL,
    related_id BIGINT,
    create_time DATETIME NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_notify_user ON sys_notification(user_id, read_flag);

CREATE TABLE IF NOT EXISTS race_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status VARCHAR(16) NOT NULL,
    create_time DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS race_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    race_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    seq INT NOT NULL,
    create_time DATETIME NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_race_student ON race_record(race_id, student_id);

CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL,
    config_value VARCHAR(255),
    description VARCHAR(255)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_config_key ON sys_config(config_key);
