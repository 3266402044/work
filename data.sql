-- ============================================================
-- ClassPulse 演示数据（MySQL 8）
-- 使用前请先执行 schema.sql 建库建表，且确保数据库为空。
-- 说明：后端首次启动时也会自动写入同一套演示数据；
--       二者任选其一即可，不要重复执行。
-- 演示账号：
--   admin / Admin@123
--   teacher01 / Teacher@123
--   teacher02 / Teacher@123
--   student01 ~ student08 / Student@123
-- 进行中任务的口令：123456
-- ============================================================
USE classpulse;

-- ---------- 用户 ----------
INSERT INTO sys_user (id, username, password_hash, role, real_name, phone, enabled, related_id, create_time) VALUES
(1, 'admin', '$2a$10$SmrVJWGCe5qpk3rREd18aei62JMRi7/qoQ7QYqAg77yr3FtFqre5m', 'ADMIN', '系统管理员', '13800000000', 1, NULL, NOW()),
(2, 'teacher01', '$2a$10$Y9syxiCm6iEMJCAp7AWFeeRVmVfISMgudmucAgpiiOcpgmwtf3rCm', 'TEACHER', '王启明', '13800000001', 1, 1, NOW()),
(3, 'teacher02', '$2a$10$Y9syxiCm6iEMJCAp7AWFeeRVmVfISMgudmucAgpiiOcpgmwtf3rCm', 'TEACHER', '李思远', '13800000002', 1, 2, NOW()),
(4, 'student01', '$2a$10$Mi/R5KQd1v5i4kyB7bU1pOvQWCdfO.tOFUAwu3/ewTrpI23oeP2v6', 'STUDENT', '张明', '13923010101', 1, 1, NOW()),
(5, 'student02', '$2a$10$Mi/R5KQd1v5i4kyB7bU1pOvQWCdfO.tOFUAwu3/ewTrpI23oeP2v6', 'STUDENT', '李婉', '13923010102', 1, 2, NOW()),
(6, 'student03', '$2a$10$Mi/R5KQd1v5i4kyB7bU1pOvQWCdfO.tOFUAwu3/ewTrpI23oeP2v6', 'STUDENT', '陈昊', '13923010103', 1, 3, NOW()),
(7, 'student04', '$2a$10$Mi/R5KQd1v5i4kyB7bU1pOvQWCdfO.tOFUAwu3/ewTrpI23oeP2v6', 'STUDENT', '赵磊', '13923010104', 1, 4, NOW()),
(8, 'student05', '$2a$10$Mi/R5KQd1v5i4kyB7bU1pOvQWCdfO.tOFUAwu3/ewTrpI23oeP2v6', 'STUDENT', '周晴', '13923010105', 1, 5, NOW()),
(9, 'student06', '$2a$10$Mi/R5KQd1v5i4kyB7bU1pOvQWCdfO.tOFUAwu3/ewTrpI23oeP2v6', 'STUDENT', '孙浩', '13923010106', 1, 6, NOW()),
(10, 'student07', '$2a$10$Mi/R5KQd1v5i4kyB7bU1pOvQWCdfO.tOFUAwu3/ewTrpI23oeP2v6', 'STUDENT', '吴迪', '13923020201', 1, 7, NOW()),
(11, 'student08', '$2a$10$Mi/R5KQd1v5i4kyB7bU1pOvQWCdfO.tOFUAwu3/ewTrpI23oeP2v6', 'STUDENT', '郑爽', '13923020202', 1, 8, NOW());

-- ---------- 班级 / 教师 / 学生 ----------
INSERT INTO class_group (id, name, grade, major, advisor_id, create_time, deleted) VALUES
(1, '计算机科学与技术 2301', '2023', '计算机科学与技术', 1, NOW(), 0),
(2, '软件工程 2302', '2023', '软件工程', 2, NOW(), 0);

INSERT INTO teacher (id, user_id, teacher_no, name, department, deleted) VALUES
(1, 2, 'T2023001', '王启明', '数学与统计学院', 0),
(2, 3, 'T2023002', '李思远', '外国语学院', 0);

INSERT INTO student (id, student_no, name, class_id, user_id, create_time, deleted) VALUES
(1, '2023010101', '张明', 1, 4, NOW(), 0),
(2, '2023010102', '李婉', 1, 5, NOW(), 0),
(3, '2023010103', '陈昊', 1, 6, NOW(), 0),
(4, '2023010104', '赵磊', 1, 7, NOW(), 0),
(5, '2023010105', '周晴', 1, 8, NOW(), 0),
(6, '2023010106', '孙浩', 1, 9, NOW(), 0),
(7, '2023020201', '吴迪', 2, 10, NOW(), 0),
(8, '2023020202', '郑爽', 2, 11, NOW(), 0);

-- ---------- 课程与授课关系 ----------
INSERT INTO course (id, course_no, name, teacher_id, semester, credit, cover_color, classroom, longitude, latitude, default_radius, deleted) VALUES
(1, 'MATH101', '高等数学', 1, '2026-2027-1', 4.0, '#38DDF8', '教学楼 A-302', 118.1234567, 32.1234567, 150, 0),
(2, 'CS201', '数据结构', 1, '2026-2027-1', 3.5, '#2DD4BF', '教学楼 B-105', 118.1245000, 32.1240000, 150, 0),
(3, 'ENG102', '大学英语', 2, '2026-2027-1', 2.0, '#F5B942', '教学楼 C-201', 118.1250000, 32.1250000, 150, 0);

INSERT INTO course_class (course_id, class_id) VALUES (1, 1), (2, 1), (2, 2), (3, 1);

-- ---------- 签到任务 ----------
INSERT INTO sign_task (id, title, teacher_id, start_time, end_time, create_time, closed_at, course_id, class_id, sign_modes, late_time, location_name, longitude, latitude, radius_meters, passcode_hash) VALUES
(1, '高等数学 · 第 3 次签到', 1, DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 180 MINUTE), DATE_SUB(NOW(), INTERVAL 70 MINUTE), NULL, 1, 1, 'CLICK,LOCATION,PASSCODE', DATE_ADD(NOW(), INTERVAL 30 MINUTE), '教学楼 A-302', 118.1234567, 32.1234567, 150, '$2a$10$.UvLdPcMNSfp/kYu1f7O8enRuwY.a2YR1TB9tU7LkfQhAUhb1X3X6'),
(2, '数据结构 · 第 4 次签到', 1, DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 140 MINUTE), DATE_ADD(NOW(), INTERVAL 60 MINUTE), NULL, 2, 1, 'CLICK,QR', DATE_ADD(NOW(), INTERVAL 125 MINUTE), '教学楼 B-105', 118.1245000, 32.1240000, 150, NULL),
(3, '大学英语 · 第 2 次签到', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:15:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '09:00:00'), NULL, 3, 1, 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:05:00'), '教学楼 C-201', NULL, NULL, 150, NULL),
(4, '高等数学 · 第 6 次签到', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:15:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '08:00:00'), NULL, 1, 1, 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:05:00'), '教学楼 A-302', NULL, NULL, 150, NULL),
(5, '数据结构 · 第 5 次签到', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:15:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '08:00:00'), NULL, 2, NULL, 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:05:00'), '教学楼 B-105', NULL, NULL, 150, NULL),
(6, '高等数学 · 第 5 次签到', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:15:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '08:00:00'), NULL, 1, 1, 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:05:00'), '教学楼 A-302', NULL, NULL, 150, NULL),
(7, '数据结构 · 第 4 次签到', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:15:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:00:00'), NULL, 2, NULL, 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:05:00'), '教学楼 B-105', NULL, NULL, 150, NULL),
(8, '数据结构 · 第 3 次签到', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:15:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '08:00:00'), NULL, 2, NULL, 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:05:00'), '教学楼 B-105', NULL, NULL, 150, NULL),
(9, '高等数学 · 第 4 次签到', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:15:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '08:00:00'), NULL, 1, 1, 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:05:00'), '教学楼 A-302', NULL, NULL, 150, NULL);

-- ---------- 签到记录 ----------
INSERT INTO sign_record (task_id, student_id, sign_time, status, method, remark, create_time) VALUES
(1, 1, DATE_SUB(NOW(), INTERVAL 8 MINUTE), 'NORMAL', 'CLICK', NULL, DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
(3, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:02:00'), 'NORMAL', 'CLICK', NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:02:00')),
(3, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:09:00'), 'LATE', 'CLICK', NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:09:00')),
(3, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:01:00'), 'LEAVE', 'MANUAL', '病假，已提交证明', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:01:00'));

INSERT INTO sign_record (task_id, student_id, sign_time, status, method, create_time) VALUES
(4, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:02:00')),
(4, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:03:00')),
(4, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:01:00')),
(4, 4, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:02:00')),
(4, 5, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:03:00')),
(4, 6, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:01:00')),
(5, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:01:00')),
(5, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:08:00'), 'LATE', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:08:00')),
(5, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:02:00')),
(5, 4, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:03:00')),
(5, 5, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:01:00')),
(5, 6, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:02:00')),
(5, 7, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:03:00')),
(5, 8, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:01:00')),
(6, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:02:00')),
(6, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:03:00')),
(6, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:01:00')),
(6, 4, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:02:00')),
(6, 5, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:03:00')),
(7, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:01:00')),
(7, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:02:00')),
(7, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:03:00')),
(7, 4, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:01:00')),
(7, 5, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:02:00')),
(7, 6, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:03:00')),
(7, 7, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:01:00')),
(7, 8, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:02:00')),
(8, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:02:00')),
(8, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:03:00')),
(8, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:01:00')),
(8, 4, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:02:00')),
(8, 5, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:03:00')),
(8, 6, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:01:00')),
(9, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:01:00')),
(9, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:02:00')),
(9, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:03:00')),
(9, 4, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:01:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:01:00')),
(9, 5, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:02:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:02:00')),
(9, 6, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:03:00'), 'NORMAL', 'CLICK', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:03:00'));

-- ---------- 请假 ----------
INSERT INTO leave_request (student_id, task_id, reason, proof_url, status, reviewer_id, review_time, remark, create_time) VALUES
(3, 3, '发烧就医', NULL, 'APPROVED', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '09:00:00'), '已核实就医记录', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '08:00:00')),
(3, 1, '参加校外竞赛，需请假一次', NULL, 'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 20 MINUTE));

-- ---------- 随机点名 ----------
INSERT INTO roll_call_session (id, course_id, class_id, teacher_id, candidate_scope, draw_count, anti_repeat_rule, exclude_leave, task_id, custom_student_ids, response_deadline_seconds, status, seed, started_at, ended_at) VALUES
(1, 1, 1, 1, 'ALL', 2, 'DAYS7', 1, NULL, NULL, 600, 'RUNNING', 20261024, DATE_SUB(NOW(), INTERVAL 1 MINUTE), NULL),
(2, 1, 1, 1, 'ALL', 3, 'DAYS7', 1, NULL, NULL, 30, 'FINISHED', 20261023, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '11:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '11:06:00'));

INSERT INTO roll_call_record (session_id, student_id, draw_order, result, performance, drawn_at, responded_at) VALUES
(1, 2, 1, 'PENDING', NULL, DATE_SUB(NOW(), INTERVAL 30 SECOND), NULL),
(1, 4, 2, 'RESPONDED', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 30 SECOND), DATE_SUB(NOW(), INTERVAL 22 SECOND)),
(2, 1, 1, 'RESPONDED', 'EXCELLENT', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '11:01:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '11:01:08')),
(2, 2, 2, 'NO_RESPONSE', NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '11:01:00'), NULL),
(2, 3, 3, 'SKIPPED', 'NORMAL', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '11:01:00'), NULL);

-- ---------- 课堂小测 ----------
INSERT INTO quiz (id, course_id, teacher_id, title, type, options_json, correct_answer, start_time, end_time, anonymous, status, explanation, create_time) VALUES
(1, 1, 1, '极限与连续 · 随堂小测', 'SINGLE', '["A. 0","B. 1","C. ∞","D. 不存在"]', '1', DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 40 MINUTE), 0, 'RUNNING', '重要极限：lim(x→0) sinx/x = 1。', DATE_SUB(NOW(), INTERVAL 3 MINUTE)),
(2, 2, 1, '二叉树遍历 · 课堂投票', 'SINGLE', '["前序遍历","中序遍历","后序遍历","层序遍历"]', '1', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:05:00'), 1, 'ENDED', '中序遍历二叉搜索树可得到有序序列。', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '13:58:00'));

INSERT INTO quiz_answer (quiz_id, student_id, answer_json, is_correct, answer_time) VALUES
(1, 2, '[1]', 1, DATE_SUB(NOW(), INTERVAL 40 SECOND)),
(1, 3, '[0]', 0, DATE_SUB(NOW(), INTERVAL 30 SECOND)),
(1, 4, '[1]', 1, DATE_SUB(NOW(), INTERVAL 20 SECOND)),
(2, 1, '[1]', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:01:00')),
(2, 2, '[0]', 0, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:02:00'));

-- ---------- 课堂抢答 ----------
INSERT INTO race_session (id, course_id, teacher_id, title, start_time, end_time, status, create_time) VALUES
(1, 1, 1, '课堂抢答：导数定义', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:10:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:10:30'), 'ENDED', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:09:00'));

INSERT INTO race_record (race_id, student_id, seq, create_time) VALUES
(1, 2, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:10:04')),
(1, 4, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:10:06')),
(1, 1, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:10:09'));

-- ---------- 公告与消息 ----------
INSERT INTO class_announcement (course_id, teacher_id, content, pinned, expire_at, create_time) VALUES
(1, 1, '下周三将进行第二章随堂测验，请提前复习极限与连续章节。', 1, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(1, 1, '本周作业提交截止时间为周日 22:00，请按时提交。', 0, DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(2, 1, '实验课请自带笔记本电脑，并提前安装好 JDK 21。', 1, DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 HOUR));

INSERT INTO sys_notification (user_id, title, content, type, read_flag, related_id, create_time) VALUES
(4, '签到即将开始', '《高等数学》第 3 次签到正在进行，请及时完成签到。', 'TASK', 0, 1, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(4, '点名提醒', '你在《高等数学》课堂被点到，请准备回答问题。', 'ROLL_CALL', 0, 2, DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(5, '签到提醒', '《高等数学》第 3 次签到正在进行，请及时完成签到。', 'TASK', 0, 1, DATE_SUB(NOW(), INTERVAL 9 MINUTE));

-- ---------- 系统配置 ----------
INSERT INTO sys_config (config_key, config_value, description) VALUES
('default.sign.duration.minutes', '15', '默认签到时长（分钟）'),
('default.location.radius', '150', '默认定位签到半径（米）'),
('late.threshold.minutes', '5', '迟到判定阈值（分钟）'),
('rollcall.response.seconds', '30', '点名默认响应时长（秒）'),
('rollcall.absence.warning', '3', '连续未响应预警阈值'),
('participation.sign.weight', '40', '参与度-正常签到权重'),
('participation.late.weight', '20', '参与度-迟到签到权重'),
('participation.rollcall.weight', '25', '参与度-点名响应权重'),
('participation.quiz.weight', '10', '参与度-小测完成权重'),
('participation.race.weight', '5', '参与度-抢答权重'),
('allow.sync.rollcall.attendance', 'true', '允许将点名异常同步为出勤提醒');
