# ClassPulse · 课堂签到与互动系统

一个可实际运行的全栈课堂运营系统：教师发布签到、学生移动端签到、实时到课监控、随机点名、课堂抢答、即时小测、公告、请假审批、课程统计与管理员基础数据维护。

- 前端：Vue 3 + TypeScript + Vite + Vue Router + Pinia + Element Plus + ECharts + UnoCSS
- 后端：Java 21 + Spring Boot 3.3 + Spring Web / Validation / Security + MyBatis-Plus + JWT
- 数据库：MySQL 8（生产）/ H2 MySQL 模式（本地零依赖默认）
- 实时：SSE（Server-Sent Events）
- 导出：Apache POI（`.xlsx`）
- 文档：Springdoc OpenAPI / Swagger UI

> 默认使用 H2 文件数据库，**无需安装 MySQL 即可直接启动**。生产或验收 MySQL 时切换 `mysql` profile 即可。

---

## 1. 快速启动

### 环境要求

| 工具 | 版本 |
| --- | --- |
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 18+（推荐 20/22） |
| MySQL | 8.x（可选） |

### 方式 A：零依赖启动（H2，推荐先跑通）

```bash
# 1. 后端（默认 h2 profile，自动建表 + 写入演示数据）
cd backend
mvn spring-boot:run

# 2. 前端（新终端）
cd frontend
npm install
npm run dev
```

打开 <http://localhost:5173>。

- 后端 API：<http://localhost:8080>
- Swagger UI：<http://localhost:8080/swagger-ui.html>
- H2 控制台：<http://localhost:8080/h2-console>  
  JDBC URL：`jdbc:h2:file:./data/classpulse`，用户名 `sa`，密码为空。

首次启动会在 `backend/data/` 生成 H2 数据文件，并自动写入演示数据。

### 方式 B：MySQL 8 启动

```bash
# 1. 建库建表
mysql -uroot -p < schema.sql

# 2.（可选）导入完整演示数据；不导入后端首次启动也会自动写入
mysql -uroot -p classpulse < data.sql

# 3. 启动后端
cd backend
# PowerShell:
$env:APP_PROFILE="mysql"
$env:DB_USER="root"
$env:DB_PASSWORD="你的密码"
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="classpulse"
mvn spring-boot:run

# macOS / Linux:
APP_PROFILE=mysql DB_USER=root DB_PASSWORD=你的密码 mvn spring-boot:run
```

```bash
# 4. 前端
cd frontend
npm install
npm run dev
```

> `schema.sql` 和 `data.sql` 位于仓库根目录；`backend/src/main/resources/db/schema-mysql.sql` 为同一份建表脚本，供打包使用。

### 生产构建

```bash
cd backend
mvn -DskipTests package
java -jar target/classpulse-backend-1.0.0.jar

cd frontend
npm run build
# dist/ 交给 Nginx 托管，并将 /api 与 /uploads 反向代理到 8080
```

前端开发环境通过 Vite 代理访问后端，见 `frontend/vite.config.ts`。

---

## 2. 演示账号

| 角色 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `Admin@123` | 运营总览、基础数据、日志、系统设置 |
| 教师 | `teacher01` | `Teacher@123` | 王启明，2 门课程，任务/点名/互动 |
| 教师 | `teacher02` | `Teacher@123` | 李思远，大学英语 |
| 学生 | `student01` ~ `student08` | `Student@123` | 张明、李婉、陈昊、赵磊、周晴、孙浩、吴迪、郑爽 |

演示数据中：

- 有 1 个进行中的签到任务（同时开启普通 / 定位 / 口令，口令 `123456`）
- 有 1 个未开始任务、1 个已结束任务、近 7 天历史任务与签到记录
- 有 1 个进行中的随机点名会话、1 个已结束会话
- 有 1 个进行中的课堂小测与若干答案
- 有 1 条待审批请假、1 条已通过请假
- 有课堂公告、消息、抢答记录

管理员可在「运营总览 / 系统设置」中点击 **重置演示数据**，将任务时间重新对齐到当前时刻（推荐每次演示前执行一次）。对应接口：`POST /api/admin/demo/reset`。

---

## 3. 功能地图

### 学生端

- 首页：进行中签到大卡、倒计时、班级到课进度、今日课表、近 7 日出勤、待处理请假、最近签到、公告
- 签到任务详情：普通 / 定位 / 口令 / 二维码签到；定位失败、超范围会给出精确中文原因
- 我的签到：按状态、课程、日期筛选，桌面表格 + 移动端卡片
- 请假申请：选择任务、填写原因、上传凭证、查看审批进度
- 课程日历：周视图展示签到、小测、公告
- 学习参与度：综合评分、分项贡献、可追溯来源
- 高优先级互动浮层：被点名、课堂小测、课堂抢答

### 教师端

- 工作台：今日课程 / 任务 / 待审批 / 到课率，7 次课程趋势，实时进行中任务，异常提醒
- 创建签到：四步表单（对象与课程 → 时间标题 → 签到方式 → 预览发布），支持复制历史任务
- 实时监控：大号倒计时、实时到课率、正常/迟到/请假/未签、签到动态流、名单搜索筛选、补签、标记请假、导出 Excel、二维码 / 口令、提前关闭、定位散点图
- 签到任务：课程/班级/日期/状态筛选、复制、关闭、导出
- 课程统计：到课率、迟到率、缺勤排名、任务明细、学生明细、异常预警
- 随机点名大屏：候选范围（全班/已签到/未签到/自定义）、抽取人数、防重复规则、排除请假、2.6 秒滚动动画、后端确定结果、响应倒计时、课堂表现、再抽一次、结束会话、同步异常出勤、全屏投屏
- 互动控制台：课堂抢答（服务端排名）、单选/多选/判断小测（实时分布与正确率）、课堂公告、互动记录与导出

### 管理端

- 运营总览：活跃课程、今日任务/签到、全校出勤率、14 天趋势、班级排行、异常提示
- 基础数据：学生、教师、班级、课程、账号；搜索、分页、增改、停用/删除、学生 Excel 导入导出
- 系统日志：按用户、模块、日期检索审计记录
- 系统设置：签到默认时长、定位半径、迟到阈值、点名响应时长、预警阈值、参与度权重、点名异常同步开关
- 演示数据重置

---

## 4. 核心实现说明

### 4.1 任务状态不落库

`sign_task` 没有 `status` 字段。列表与详情都返回服务端实时计算的 `taskStatus`：

| 条件 | 状态 | 可签到 |
| --- | --- | --- |
| `closed_at != null` | `CLOSED` 已提前关闭 | 否 |
| `now < start_time` | `NOT_STARTED` 未开始 | 否 |
| `start_time <= now <= end_time` | `ONGOING` 进行中 | 是 |
| `now > end_time` | `ENDED` 已结束 | 否 |

前端不根据本机时间自行判断，全部以接口返回为准。

### 4.2 签到幂等与并发

- `sign_record` 上建立 `UNIQUE(task_id, student_id)`，是最终并发保护。
- 服务层先查重给出友好提示；并发下仍会捕获 `DuplicateKeyException` 并返回 `409 请勿重复签到`。
- 已用并发脚本验证：3 个并发请求 → 1 个 200、2 个 409，数据库仅 1 条记录。
- 前端按钮 loading / 禁用只是体验优化，不承担防重职责。

### 4.3 签到方式

| 方式 | 实现 |
| --- | --- |
| `CLICK` | 直接写记录 |
| `QR` | 教师生成 32 字节随机令牌，仅存 SHA-256 哈希，30 秒有效；扫码 URL 携带令牌 |
| `LOCATION` | Haversine 计算学生坐标与课堂中心距离，超范围返回 `距签到点 xxx 米，超出允许范围 150 米` |
| `PASSCODE` | 6 位口令，BCrypt 哈希存储，可重置 |
| `MANUAL` | 教师补签 / 请假审批通过后自动登记 |

签到状态：开始至 `late_time` 前为 `NORMAL`；`late_time` 后至结束为 `LATE`；教师手动 `MANUAL`；请假通过 `LEAVE`。

### 4.4 随机点名

- 由后端在同一事务内计算候选池、应用防重复规则、写入 `roll_call_record`，再通过 SSE 推送结果。
- 动画只负责呈现，刷新大屏/学生端结果不变。
- 同一会话内依靠 `UNIQUE(session_id, student_id)` 保证绝不重复抽取。
- 默认公平策略：从未被抽到的学生优先，其次按最近一次被抽时间从早到晚排序。
- 支持 `NONE / SESSION / DAYS7 / LAST_N` 防重复规则；候选池耗尽时才回填近期已抽学生。
- 学生端通过 SSE 收到 `rollcall:drawn`，在响应倒计时内点击“我在，准备回答”，教师端实时更新。
- 点名结果不自动改写签到记录；「同步异常出勤」只发送提醒并写审计日志（可在系统设置中关闭）。

### 4.5 实时通道

- 后端内存 SSE Hub，按 `userId` 维护连接；事件包括：
  `notification`、`task:changed`、`sign:new`、`rollcall:drawn`、`rollcall:update`、`quiz:new`、`quiz:update`、`race:started`、`leave:result` 等。
- 前端 `EventSource` 使用 `/api/stream?token=JWT`，断线自动重连；任务监控页另有 20 秒轮询兜底。
- 事件推送失败不影响业务事务，结果以数据库为准。

### 4.6 权限与安全

- JWT 无状态认证，`BCryptPasswordEncoder` 存储密码。
- 后端按角色与数据归属校验：教师只能管理自己的任务与授课课程；学生只能查看/签到自己的任务；管理员可查看全校。
- 服务层使用 `SecurityUtils` 获取当前身份，**不信任请求体中的 `studentId`**。
- 所有关键操作写入 `operation_log`。
- 口令与二维码令牌均不保存明文。
- 删除基础数据时：班级有学生禁止删除；课程有任务禁止删除；学生/教师采用逻辑删除 + 账号禁用。

### 4.7 时间与定位

- 数据库、后端统一使用 `Asia/Shanghai`（JDBC `serverTimezone=Asia/Shanghai`）。
- 任务时间校验：`start < end`，`late_time` 位于两者之间，开始时间不能早于当前 5 分钟以上。
- 距离保留两位小数；坐标缺失或超限返回明确中文错误。

---

## 5. API 示例

完整接口见 Swagger：<http://localhost:8080/swagger-ui.html>。统一响应：

```json
{ "code": 200, "message": "操作成功", "data": {} }
```

### 登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"teacher01","password":"Teacher@123"}'
```

### 学生查看任务并签到

```bash
TOKEN="登录返回的 token"

curl http://localhost:8080/api/student/tasks?scope=ONGOING \
  -H "Authorization: Bearer $TOKEN"

curl -X POST http://localhost:8080/api/tasks/1/sign \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"method":"PASSCODE","passcode":"123456"}'
```

### 定位签到

```json
{
  "method": "LOCATION",
  "longitude": 118.1234567,
  "latitude": 32.1234567
}
```

### 创建签到任务（教师）

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TEACHER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "高等数学 · 第 4 次签到",
    "courseId": 1,
    "classId": 1,
    "startTime": "2026-10-25T09:00:00",
    "endTime": "2026-10-25T09:20:00",
    "lateTime": "2026-10-25T09:05:00",
    "signModes": ["CLICK", "LOCATION"],
    "locationName": "教学楼 A-302",
    "longitude": 118.1234567,
    "latitude": 32.1234567,
    "radiusMeters": 150
  }'
```

### 随机点名

```bash
curl -X POST http://localhost:8080/api/roll-calls \
  -H "Authorization: Bearer $TEACHER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": 1,
    "classId": 1,
    "candidateScope": "ALL",
    "drawCount": 2,
    "antiRepeatRule": "DAYS7",
    "excludeLeave": true,
    "responseDeadlineSeconds": 30
  }'
```

### 学生响应点名

```bash
curl -X POST http://localhost:8080/api/roll-calls/1/respond \
  -H "Authorization: Bearer $STUDENT_TOKEN"
```

### SSE

```bash
curl -N "http://localhost:8080/api/stream?token=$TOKEN"
```

---

## 6. 数据库设计

核心三表：

- `student`：学生档案，`student_no` 唯一，关联 `class_group`
- `sign_task`：签到任务，不存状态；含时间、方式、迟到截止、定位、口令哈希
- `sign_record`：签到记录，`UNIQUE(task_id, student_id)`，`idx_task_id`、`idx_student_id(student_id, sign_time)`

支撑表：

`sys_user`、`class_group`、`teacher`、`course`、`course_class`、`leave_request`、`sign_qr_token`、`operation_log`、`roll_call_session`、`roll_call_record`、`quiz`、`quiz_answer`、`class_announcement`、`sys_notification`、`race_session`、`race_record`、`sys_config`。

完整 DDL 见根目录 `schema.sql`（MySQL 8，`utf8mb4`）。H2 开发库的 DDL 见 `backend/src/main/resources/db/schema-h2.sql`。

---

## 7. 测试与验收建议

1. **登录与角色**：三个演示账号分别进入学生首页 / 教师工作台 / 运营总览。
2. **任务状态**：创建任务并调整开始/结束时间；进行中显示倒计时；提前关闭后学生立即无法签到。
3. **签到幂等**：同一学生双击、两个标签页或并发请求，只产生一条记录，其余返回 409。
4. **越权**：学生直接请求其他班级任务 → 403；教师请求其他教师任务记录 → 403。
5. **签到方式**：普通、口令 `123456`、二维码（教师监控页生成 30 秒二维码）、定位（允许浏览器定位，超范围返回精确距离）。
6. **实时性**：教师监控页保持打开，学生签到后 1 秒内出现动态与计数更新。
7. **随机点名**：开始抽取后同一会话“再抽一次”不会重复；刷新页面结果不变；学生端浮层响应后教师端计数实时变化。
8. **统计与导出**：任务名单、学生名单、点名记录导出 `.xlsx`；课程统计按课程/班级/日期过滤。
9. **管理端**：学生/教师/班级/课程增改、学号唯一校验、学生 Excel 导入（返回逐行错误）。
10. **响应式**：375px 宽度下学生端、点名浮层、任务详情均为单列；1440px 下为多列布局。

已验证的关键链路：登录、学生首页、口令签到、二维码签到、定位超范围与成功签到、并发签到幂等、随机点名创建/响应/结果、教师实时监控 SSE、管理端总览与基础数据。

---

## 8. 目录结构

```
.
├── backend
│   ├── pom.xml
│   └── src/main
│       ├── java/com/classpulse
│       │   ├── config        # Security / MyBatis-Plus / OpenAPI / 初始化
│       │   ├── controller    # REST + SSE
│       │   ├── dto / vo
│       │   ├── entity / mapper
│       │   ├── security      # JWT
│       │   ├── service       # 业务、实时、导出、演示数据
│       │   └── common        # 统一响应、异常、工具
│       └── resources
│           ├── application.yml
│           └── db            # H2 / MySQL DDL
├── frontend
│   ├── src
│   │   ├── api / stores / router / types / utils
│   │   ├── components        # 布局、图表、倒计时、学生浮层
│   │   └── views             # student / teacher / admin
│   └── vite.config.ts
├── schema.sql                # MySQL 8 建库建表
├── data.sql                  # MySQL 8 完整演示数据
└── README.md
```

---

## 9. 常见问题

**Q：前端提示网络异常？**  
确认后端 8080 已启动；前端开发环境通过 Vite 代理转发 `/api`。

**Q：H2 数据想重置？**  
管理员登录后点击「重置演示数据」，或调用 `POST /api/admin/demo/reset`。也可停止后端后删除 `backend/data/` 再启动。

**Q：二维码过期？**  
令牌 30 秒有效，教师在监控页点击「立即刷新二维码」后重新扫码。

**Q：定位签到失败？**  
浏览器必须允许定位；`localhost` 属于安全上下文可直接使用定位。若拒绝授权，页面会明确提示，而不是静默失败。

**Q：没有配置 Redis 能运行吗？**  
可以。当前二维码令牌落库并按哈希校验，并发幂等由数据库唯一索引保证，Redis 不是必需依赖。项目保留了 Redis 依赖与开关配置，便于后续扩展分布式锁与热点缓存。

**Q：时间不一致？**  
后端与数据库统一 `Asia/Shanghai`；请确保 MySQL 容器/服务器时区一致。
