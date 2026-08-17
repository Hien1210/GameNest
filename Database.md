# GameNest — Database Design

Tài liệu này mô tả toàn bộ thiết kế database hiện tại của GameNest (Microsoft SQL Server, database `GameNestDB`), tổng hợp từ các migration script trong thư mục [`db/`](db/) theo đúng thứ tự đã chạy (`01_...` → `13_...`). Đây là tài liệu *mô tả trạng thái cuối cùng* của schema sau khi toàn bộ migration được áp dụng — không phải bản thân migration.

> Toàn bộ migration là **additive**, do DBA tự chạy bằng SSMS/tài khoản `sa` — ứng dụng không tự động sửa schema lúc runtime.

---

## 1. Tổng quan kiến trúc

```text
Browser → Servlet/Controller → Service → DAO → JDBC → SQL Server (GameNestDB)
```

- **Engine**: Microsoft SQL Server
- **Driver**: mssql-jdbc 13.4.0.jre11
- **Truy cập DB của ứng dụng**: qua login riêng `GameNestSvcLogin` / user `GameNestSvcUser`, **không** dùng `sa`/`sysadmin`/`db_owner` (xem [§8 Bảo mật](#8-bảo-mật--phân-quyền-database)).
- **Naming convention**: bảng `PascalCase` số nhiều (`Accounts`, `Questions`), cột `snake_case`, khóa chính `<table_singular>_id`, khóa ngoại giữ nguyên tên cột được tham chiếu, constraint đặt tên `PK_/FK_/UQ_/CK_/DF_/IX_<Table>_<...>`.
- **Timestamp**: `DATETIME2(0)`, mặc định `SYSUTCDATETIME()` (UTC).
- **Soft delete**: dữ liệu nghiệp vụ chính (Accounts, Games, Questions, Answers) không bao giờ bị `DELETE` — chỉ đổi `status`/cờ `is_deleted`. Chỉ bảng junction (`LFGMembers`) và số ít trường hợp đặc thù được phép hard-delete thật.
- **Pagination**: mọi truy vấn danh sách dùng `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY` ở tầng DB, không paginate bằng Java.

---

## 2. Sơ đồ quan hệ (ERD dạng text)

```text
Accounts ──┬──< Questions (account_id = author)
           ├──< Answers (account_id = author)
           ├──< LFGPosts (account_id = creator)
           ├──< LFGMembers (account_id = member)
           ├──< AuditLogs (account_id = actor, nullable)
           ├──< Reports (reporter_account_id)
           ├──< Reports (reviewed_by, nullable)
           └──< SystemSettings (updated_by, nullable)

Games ──┬──< Questions (game_id)
        └──< LFGPosts (game_id)

Questions ──< Answers (question_id)

LFGPosts ──< LFGMembers (lfg_id)

OtpVerifications           — độc lập, khóa theo (email, purpose), không có FK
Reports.target_id/target_type      — polymorphic (ACCOUNT / QUESTION / ANSWER), không có FK SQL
AuditLogs.target_id/target_type    — polymorphic (ACCOUNT / GAME / REPORT / SETTING / QUESTION / ANSWER), không có FK SQL
SystemSettings                     — độc lập, key/value, chỉ FK tới Accounts qua updated_by
```

Ghi chú: `target_id`/`target_type` của `Reports` và `AuditLogs` là tham chiếu đa hình (polymorphic) — cố ý **không** tạo FK SQL trực tiếp tới nhiều bảng khác nhau. Tầng Service chịu trách nhiệm kiểm tra target tồn tại/hợp lệ trước khi ghi.

---

## 3. Danh sách bảng

| # | Bảng | Vai trò | Migration gốc |
|---|------|---------|----------------|
| 1 | `Accounts` | Tài khoản người dùng (USER/ADMIN/MODERATOR) | `01` (+`03`, `11`) |
| 2 | `Games` | Danh mục game | `01` (+`04`) |
| 3 | `Questions` | Câu hỏi cộng đồng | `01` |
| 4 | `Answers` | Câu trả lời | `01` |
| 5 | `LFGPosts` | Bài đăng Looking‑For‑Group | `01` |
| 6 | `LFGMembers` | Thành viên tham gia LFG (junction) | `01` |
| 7 | `OtpVerifications` | Mã OTP (Register/Forgot Password/Change Email) | `02` (+`07`) |
| 8 | `AuditLogs` | Nhật ký hành động quản trị/kiểm duyệt | `08` (+`09`,`10`,`12`,`13`) |
| 9 | `Reports` | Báo cáo vi phạm (Account/Question/Answer) | `09` |
| 10 | `SystemSettings` | Cấu hình hệ thống dạng key/value | `10` |

> `LFGPosts`/`LFGMembers` là schema nền tảng dựng sẵn từ `01_core_schema.sql` theo CLAUDE.md mục 6 — **chưa có business logic/UI** (chưa xây tính năng LFG ở tầng ứng dụng), chỉ tồn tại ở tầng database.

---

## 4. Chi tiết từng bảng

### 4.1 `Accounts`

| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| account_id | INT IDENTITY(1,1) | PK |
| username | NVARCHAR(50) | NOT NULL, UNIQUE |
| email | NVARCHAR(255) | NOT NULL, UNIQUE |
| password_hash | NVARCHAR(255) | NOT NULL (BCrypt) |
| display_name | NVARCHAR(100) | NULL |
| avatar_url | NVARCHAR(500) | NULL (Cloudinary CDN URL) |
| status | VARCHAR(20) | NOT NULL, DEFAULT `'ACTIVE'` |
| role | VARCHAR(20) | NOT NULL, DEFAULT `'USER'` *(thêm ở `03`)* |
| created_at | DATETIME2(0) | NOT NULL, DEFAULT `SYSUTCDATETIME()` |
| updated_at | DATETIME2(0) | NULL |

**CHECK constraints**
- `CK_Accounts_status`: `status IN ('ACTIVE','BANNED','DELETED','SUSPENDED')`
- `CK_Accounts_role`: `role IN ('USER','ADMIN','MODERATOR')` *(mở rộng ở `03` → `11`)*

**Index**: PK, UQ(username), UQ(email) — không có index bổ sung.

**Ghi chú nghiệp vụ**
- Không có cơ chế tự đăng ký `ADMIN`/`MODERATOR` — chỉ DBA nâng cấp role thủ công qua `UPDATE`.
- Account không bao giờ hard-delete; `status = 'DELETED'` là soft-delete.
- `password_hash` không bao giờ được đọc ra ngoài tầng Service/DAO.

---

### 4.2 `Games`

| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| game_id | INT IDENTITY(1,1) | PK |
| name | NVARCHAR(150) | NOT NULL, UNIQUE |
| description | NVARCHAR(MAX) | NULL |
| cover_image_url | NVARCHAR(500) | NULL |
| release_date | DATE | NULL *(thêm ở `04`)* |
| status | VARCHAR(20) | NOT NULL, DEFAULT `'ACTIVE'` |
| created_at | DATETIME2(0) | NOT NULL, DEFAULT `SYSUTCDATETIME()` |
| updated_at | DATETIME2(0) | NULL |

**CHECK**: `CK_Games_status`: `status IN ('ACTIVE','INACTIVE')`

**Index**: PK, UQ(name).

---

### 4.3 `Questions`

| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| question_id | INT IDENTITY(1,1) | PK |
| account_id | INT | NOT NULL, FK → `Accounts(account_id)` |
| game_id | INT | NOT NULL, FK → `Games(game_id)` |
| title | NVARCHAR(250) | NOT NULL |
| content | NVARCHAR(MAX) | NOT NULL |
| status | VARCHAR(20) | NOT NULL, DEFAULT `'ACTIVE'` |
| is_deleted | BIT | NOT NULL, DEFAULT `0` |
| deleted_at | DATETIME2(0) | NULL |
| deleted_by | INT | NULL, FK → `Accounts(account_id)` |
| created_at | DATETIME2(0) | NOT NULL, DEFAULT `SYSUTCDATETIME()` |
| updated_at | DATETIME2(0) | NULL |

**CHECK**: `CK_Questions_status`: `status IN ('ACTIVE','HIDDEN','DELETED','LOCKED')`

**Index**: PK; `IX_Questions_game_id`, `IX_Questions_account_id`, `IX_Questions_status`.

**Ghi chú nghiệp vụ**
- Soft-delete: `status='DELETED'` đi kèm `is_deleted=1` + `deleted_at` + `deleted_by`, set đồng thời qua `softDelete()`.
- Moderator/Admin chỉ đổi qua lại `ACTIVE ↔ HIDDEN ↔ LOCKED` bằng `UPDATE status` thuần (không đụng `is_deleted`); không cho đổi status khi đang `DELETED` để tránh mâu thuẫn `is_deleted=1` nhưng `status≠DELETED`.

---

### 4.4 `Answers`

| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| answer_id | INT IDENTITY(1,1) | PK |
| question_id | INT | NOT NULL, FK → `Questions(question_id)` |
| account_id | INT | NOT NULL, FK → `Accounts(account_id)` |
| content | NVARCHAR(MAX) | NOT NULL |
| is_accepted | BIT | NOT NULL, DEFAULT `0` |
| status | VARCHAR(20) | NOT NULL, DEFAULT `'ACTIVE'` |
| is_deleted | BIT | NOT NULL, DEFAULT `0` |
| deleted_at | DATETIME2(0) | NULL |
| deleted_by | INT | NULL, FK → `Accounts(account_id)` |
| created_at | DATETIME2(0) | NOT NULL, DEFAULT `SYSUTCDATETIME()` |
| updated_at | DATETIME2(0) | NULL |

**CHECK**: `CK_Answers_status`: `status IN ('ACTIVE','HIDDEN','DELETED')` — **không có `LOCKED`** (khác `Questions`).

**Index**: PK; `IX_Answers_question_id`, `IX_Answers_account_id`, `IX_Answers_status` *(thêm ở `13`, phục vụ filter status của Moderator Answers List)*.

**Ghi chú nghiệp vụ**
- Chỉ 1 answer được `is_accepted=1` trên mỗi question tại một thời điểm — đảm bảo bằng transaction 2 bước (`unacceptAllForQuestion` + `setAccepted`) trong `AnswerService.acceptAnswer`, không phải CHECK constraint.
- Moderator chỉ đổi qua lại `ACTIVE ↔ HIDDEN` (không có `LOCKED` để đổi tới); cùng guard chống mâu thuẫn `is_deleted` như Questions.

---

### 4.5 `LFGPosts`

| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| lfg_id | INT IDENTITY(1,1) | PK |
| account_id | INT | NOT NULL, FK → `Accounts(account_id)` (creator) |
| game_id | INT | NOT NULL, FK → `Games(game_id)` |
| title | NVARCHAR(250) | NOT NULL |
| description | NVARCHAR(MAX) | NULL |
| game_mode | NVARCHAR(100) | NULL |
| required_rank | NVARCHAR(100) | NULL |
| region | NVARCHAR(100) | NULL |
| max_players | INT | NOT NULL |
| current_players | INT | NOT NULL, DEFAULT `0` |
| start_time | DATETIME2(0) | NULL |
| status | VARCHAR(20) | NOT NULL, DEFAULT `'OPEN'` |
| created_at | DATETIME2(0) | NOT NULL, DEFAULT `SYSUTCDATETIME()` |
| updated_at | DATETIME2(0) | NULL |

**CHECK**
- `CK_LFGPosts_status`: `status IN ('OPEN','FULL','CLOSED','EXPIRED','DELETED')`
- `CK_LFGPosts_max_players`: `max_players > 0`
- `CK_LFGPosts_current_players`: `current_players >= 0 AND current_players <= max_players`

**Index**: PK; `IX_LFGPosts_game_id`, `IX_LFGPosts_account_id`, `IX_LFGPosts_status`.

---

### 4.6 `LFGMembers` (junction table)

| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| lfg_member_id | INT IDENTITY(1,1) | PK |
| lfg_id | INT | NOT NULL, FK → `LFGPosts(lfg_id)` |
| account_id | INT | NOT NULL, FK → `Accounts(account_id)` |
| joined_at | DATETIME2(0) | NOT NULL, DEFAULT `SYSUTCDATETIME()` |

**Constraint**: `UQ_LFGMembers_lfg_account`: UNIQUE `(lfg_id, account_id)` — 1 account không join trùng 1 LFG.

**Index**: PK; `IX_LFGMembers_lfg_id`, `IX_LFGMembers_account_id`.

**Đặc biệt**: Đây là bảng **junction/quan hệ hiện tại**, không phải nội dung lịch sử — theo CLAUDE.md mục 7.3, được phép **hard-delete thật** khi member rời nhóm (là bảng duy nhất được `GRANT DELETE` tường minh cho login ứng dụng — xem §8).

---

### 4.7 `OtpVerifications`

| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| otp_id | INT IDENTITY(1,1) | PK |
| email | NVARCHAR(255) | NOT NULL |
| purpose | VARCHAR(20) | NOT NULL |
| otp_hash | CHAR(64) | NOT NULL (SHA-256 hex, không lưu OTP plaintext) |
| expires_at | DATETIME2(0) | NOT NULL |
| is_used | BIT | NOT NULL, DEFAULT `0` |
| attempt_count | INT | NOT NULL, DEFAULT `0` |
| created_at | DATETIME2(0) | NOT NULL, DEFAULT `SYSUTCDATETIME()` |

**CHECK**: `CK_OtpVerifications_purpose`: `purpose IN ('REGISTER','RESET_PASSWORD','CHANGE_EMAIL')` *(`CHANGE_EMAIL` thêm ở `07`)*

**Index**: PK; `IX_OtpVerifications_email_purpose` (email, purpose, is_used).

**Ghi chú nghiệp vụ**: TTL 5 phút, resend cooldown 60 giây, tối đa 5 lần thử sai — 3 giá trị này hiện đọc từ `SystemSettings` (key `otp.expiration_minutes`/`otp.resend_cooldown_seconds`/`otp.max_attempts`) với fallback y hệt giá trị mặc định nếu đọc lỗi. Không có FK tới `Accounts` vì OTP cho Register xảy ra **trước khi** account tồn tại.

---

### 4.8 `AuditLogs` (append-only)

| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| audit_log_id | INT IDENTITY(1,1) | PK |
| account_id | INT | NULL, FK → `Accounts(account_id)` (actor) |
| username | NVARCHAR(50) | NOT NULL (snapshot tại thời điểm ghi) |
| role_name | VARCHAR(20) | NOT NULL (snapshot) |
| module | VARCHAR(30) | NOT NULL |
| action | VARCHAR(30) | NOT NULL |
| target_id | INT | NULL (polymorphic) |
| target_type | VARCHAR(30) | NULL (polymorphic) |
| description | NVARCHAR(500) | NULL |
| ip_address | VARCHAR(45) | NULL |
| user_agent | NVARCHAR(255) | NULL |
| created_at | DATETIME2(0) | NOT NULL, DEFAULT `SYSUTCDATETIME()` |

**CHECK** (mở rộng dần qua các migration `08 → 09 → 10 → 12 → 13`)
- `CK_AuditLogs_module`: `module IN ('ACCOUNTS','GAMES','REPORTS','SETTINGS','QUESTIONS','ANSWERS')`
- `CK_AuditLogs_action`: `action IN ('CREATE','UPDATE','STATUS_CHANGE','RESOLVE','REJECT')`
- `CK_AuditLogs_target_type`: `target_type IN ('ACCOUNT','GAME','REPORT','SETTING','QUESTION','ANSWER')`

**Index**: PK; `IX_AuditLogs_created_at` (DESC, sort mặc định), `IX_AuditLogs_account_id`, `IX_AuditLogs_module_action`, `IX_AuditLogs_target`.

**Nguyên tắc**: **append-only tuyệt đối** — DAO chỉ có `insert()` + các method đọc, không có `update()`/`delete()`. `username`/`role_name` là **snapshot** tại thời điểm hành động (không phụ thuộc JOIN `Accounts`), nên nếu username/role đổi sau này, lịch sử cũ vẫn giữ nguyên giá trị cũ.

---

### 4.9 `Reports`

| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| report_id | INT IDENTITY(1,1) | PK |
| reporter_account_id | INT | NOT NULL, FK → `Accounts(account_id)` |
| target_id | INT | NOT NULL (polymorphic) |
| target_type | VARCHAR(20) | NOT NULL |
| reason | VARCHAR(30) | NOT NULL |
| description | NVARCHAR(1000) | NULL |
| status | VARCHAR(20) | NOT NULL, DEFAULT `'PENDING'` |
| reviewed_by | INT | NULL, FK → `Accounts(account_id)` |
| reviewed_at | DATETIME2(0) | NULL |
| resolution_note | NVARCHAR(1000) | NULL |
| created_at | DATETIME2(0) | NOT NULL, DEFAULT `SYSUTCDATETIME()` |
| updated_at | DATETIME2(0) | NULL |

**CHECK**
- `CK_Reports_target_type`: `target_type IN ('ACCOUNT','QUESTION','ANSWER')` *(không hỗ trợ GAME)*
- `CK_Reports_reason`: `reason IN ('SPAM','HARASSMENT','INAPPROPRIATE_CONTENT','HATE_SPEECH','MISINFORMATION','CHEATING','OTHER')`
- `CK_Reports_status`: `status IN ('PENDING','RESOLVED','REJECTED')` — không có `DELETED`

**Index**: PK; `IX_Reports_status`, `IX_Reports_target` (target_type, target_id), `IX_Reports_reporter`, `IX_Reports_created_at`.

**Chống trùng report**: `UQ_Reports_pending_target` — **filtered unique index** trên `(reporter_account_id, target_type, target_id) WHERE status = 'PENDING'`. Một account chỉ được có tối đa 1 report `PENDING` cho cùng 1 target; sau khi report cũ `RESOLVED`/`REJECTED`, account có thể report lại.

**State machine**: `PENDING → RESOLVED` hoặc `PENDING → REJECTED`, một chiều, enforce atomic qua `UPDATE ... WHERE report_id=? AND status='PENDING'` (0 dòng ảnh hưởng = báo lỗi "đã được xử lý", không cho ghi đè quyết định trước).

---

### 4.10 `SystemSettings`

| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| setting_id | INT IDENTITY(1,1) | PK |
| setting_key | VARCHAR(100) | NOT NULL, UNIQUE |
| setting_value | NVARCHAR(500) | NOT NULL |
| setting_type | VARCHAR(20) | NOT NULL |
| description | NVARCHAR(255) | NULL |
| updated_by | INT | NULL, FK → `Accounts(account_id)` |
| updated_at | DATETIME2(0) | NOT NULL, DEFAULT `SYSUTCDATETIME()` |

**CHECK**: `CK_SystemSettings_type`: `setting_type IN ('STRING','INTEGER','BOOLEAN','ENUM')`

**Index**: PK; `UQ_SystemSettings_key` (UNIQUE constraint tự tạo index, không cần index riêng).

**10 dòng dữ liệu mặc định** (seed sẵn ở migration `10`, khớp behavior mặc định của hệ thống):

| setting_key | setting_value | setting_type |
|---|---|---|
| site.name | GameNest | STRING |
| site.description | Gaming Community Platform | STRING |
| system.status | ONLINE | ENUM |
| registration.enabled | true | BOOLEAN |
| questions.enabled | true | BOOLEAN |
| answers.enabled | true | BOOLEAN |
| reports.enabled | true | BOOLEAN |
| otp.expiration_minutes | 5 | INTEGER |
| otp.resend_cooldown_seconds | 60 | INTEGER |
| otp.max_attempts | 5 | INTEGER |

**Nguyên tắc**: generic key/value để mở rộng setting về sau **không cần ALTER TABLE liên tục**. Application chỉ `UPDATE` giá trị của key đã tồn tại — không có method `insert`, key lạ từ form bị bỏ qua ở tầng Service.

---

## 5. Hard Delete Policy — bảng nào được phép DELETE

| Bảng | Hard Delete? | Cơ chế thay thế |
|------|---------------|------------------|
| Accounts | ❌ Không | `status = 'DELETED'` |
| Games | ❌ Không | `status = 'INACTIVE'` |
| Questions | ❌ Không | `status='DELETED'` + `is_deleted=1` |
| Answers | ❌ Không | `status='DELETED'` + `is_deleted=1` |
| Reports | ❌ Không | `status` chuyển `RESOLVED`/`REJECTED`, giữ lịch sử vĩnh viễn |
| AuditLogs | ❌ Không | Append-only, không có nghiệp vụ "xóa log" |
| SystemSettings | ❌ Không | Chỉ `UPDATE value`, không xóa key |
| OtpVerifications | ⚠️ Không có delete, chỉ `is_used`/hết hạn tự nhiên | — |
| **LFGMembers** | ✅ **Có** (junction) | Rời nhóm = xóa dòng quan hệ thật |

Đây là chính sách cốt lõi xuyên suốt toàn bộ dự án (CLAUDE.md mục 7) và được **enforce ở 2 lớp độc lập**: tầng ứng dụng (không DAO nào có method `DELETE FROM` ngoài `LFGMembers`) **và** tầng database (xem §8 — login ứng dụng bị `DENY DELETE` ở cấp schema).

---

## 6. Bảng vai trò (role) và giới hạn

`Accounts.role ∈ {USER, ADMIN, MODERATOR}` — không có bảng `Roles` riêng, không có `MODERATOR` chuyên trách theo bảng phụ. Phân quyền hoàn toàn dựa vào cột `role` + filter tầng servlet (`AdminAuthorizationFilter`, `ModeratorAuthorizationFilter`), **không có** khác biệt gì ở tầng database giữa các role (không RLS, không permission theo role trong SQL Server).

---

## 7. Lịch sử Migration

| File | Nội dung chính |
|------|----------------|
| `01_core_schema.sql` | Tạo `Accounts`, `Games`, `Questions`, `Answers`, `LFGPosts`, `LFGMembers` |
| `02_otp_verifications.sql` | Tạo `OtpVerifications` |
| `03_accounts_role.sql` | Thêm `Accounts.role` (USER/ADMIN) |
| `04_games_release_date.sql` | Thêm `Games.release_date` |
| `05_recreate_applogin.sql` | *(lịch sử, đã superseded bởi `06`)* |
| `06_gamenest_svc_login.sql` | Tạo login/user ứng dụng cuối cùng `GameNestSvcLogin` với least-privilege đúng |
| `07_otp_change_email_purpose.sql` | Mở rộng `CK_OtpVerifications_purpose` thêm `CHANGE_EMAIL` |
| `08_audit_logs.sql` | Tạo `AuditLogs` (module ACCOUNTS/GAMES) |
| `09_reports.sql` | Tạo `Reports`; mở rộng AuditLogs nhận `REPORTS`/`RESOLVE`/`REJECT`/`REPORT` |
| `10_system_settings.sql` | Tạo `SystemSettings` + seed mặc định; mở rộng AuditLogs nhận `SETTINGS`/`SETTING` |
| `11_moderator_role.sql` | Mở rộng `CK_Accounts_role` thêm `MODERATOR` |
| `12_questions_moderation.sql` | Mở rộng AuditLogs nhận `QUESTIONS`/`QUESTION` |
| `13_answers_moderation.sql` | Mở rộng AuditLogs nhận `ANSWERS`/`ANSWER`; thêm `IX_Answers_status` |

Tất cả migration từ `07` trở đi đều theo cùng một khuôn mẫu: **additive only** — `DROP CONSTRAINT` + `ADD CONSTRAINT` với danh sách giá trị mở rộng (không bao giờ thu hẹp), không đổi dữ liệu hiện có.

---

## 8. Bảo mật & phân quyền Database

Ứng dụng kết nối bằng một login/user riêng, **không phải** `sa`/`sysadmin`/`db_owner`:

```text
SQL Server
│
├── GameNestSvcLogin  (SQL login, CHECK_POLICY=OFF)
│       ↓
│   GameNestSvcUser   (database user trong GameNestDB)
│       ↓
│   GRANT SELECT, INSERT, UPDATE, EXECUTE ON SCHEMA::dbo
│   DENY DELETE, ALTER ON SCHEMA::dbo
│   GRANT DELETE ON dbo.LFGMembers   (ngoại lệ junction table)
│
└── (Vai trò DBA/sa riêng cho migration/backup/schema change)
```

**Bài học quan trọng đã ghi lại trong `06_gamenest_svc_login.sql`**: **không dùng `DENY CONTROL`** ở cấp schema trong thiết kế least-privilege — `DENY CONTROL` chặn ngầm cả `SELECT`/`INSERT`/`UPDATE` dù các quyền đó đã được `GRANT` tường minh riêng (đã từng gây lỗi "SELECT permission denied" khó chẩn đoán). Chỉ cần `DENY ALTER` (chặn sửa schema) + `DENY DELETE` (chặn hard-delete) là đủ đạt mục tiêu bảo mật.

Kết quả: **hard delete bị chặn ở tầng SQL Server** ngay cả khi có bug ở tầng Java vô tình gọi `DELETE FROM` — đúng nguyên tắc phòng thủ nhiều lớp của CLAUDE.md mục 8/29.

---

## 9. Nguyên tắc thiết kế xuyên suốt

1. **Không tạo bảng phụ cho mỗi role/module** — `Accounts.role` dùng chung cho USER/ADMIN/MODERATOR; `AuditLogs`/`Reports` dùng chung cho cả Admin Portal lẫn Moderator Portal (không có `ModeratorReports`, `ModeratorQuestions`, v.v.).
2. **Audit Log dùng target đa hình** (`target_type` + `target_id`) thay vì tạo bảng audit riêng cho từng module.
3. **CHECK constraint mở rộng dần, không bao giờ thu hẹp** — mọi enum nghiệp vụ (status, role, purpose, module, action...) đều có CHECK constraint tương ứng ở DB, không chỉ validate ở Java.
4. **Index chỉ thêm khi có query cụ thể cần** — mỗi index mới trong migration đều kèm giải thích truy vấn nào cần nó (xem `13_answers_moderation.sql`).
5. **Snapshot cho dữ liệu lịch sử** — `AuditLogs.username`/`role_name` là bản sao tại thời điểm ghi, không JOIN động, để lịch sử không bị "viết lại" khi Account đổi username/role sau này.
