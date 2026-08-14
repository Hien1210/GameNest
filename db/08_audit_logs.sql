-- ============================================================
-- GameNest — Audit Log (Nhật ký hệ thống)
-- Ghi lại các thao tác quản trị quan trọng (Admin Accounts, Admin Games).
-- Bảng này là APPEND-ONLY: chỉ INSERT/SELECT, không UPDATE/DELETE.
--
-- Quy ước theo schema hiện có (xem 01_core_schema.sql, 03_accounts_role.sql):
--   - PK dạng INT IDENTITY(1,1)
--   - timestamp dùng DATETIME2(0) DEFAULT (SYSUTCDATETIME())
--   - naming snake_case, FK đặt tên FK_<Child>_<Parent>
--   - enum-like column dùng VARCHAR + CHECK constraint
--
-- account_id tham chiếu Accounts.account_id (INT IDENTITY) nhưng để NULL
-- được vì actor snapshot (username/role_name) mới là nguồn sự thật cho lịch
-- sử — account_id chỉ là liên kết tham khảo thêm.
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestSvcLogin/AppLogin)
-- ============================================================

USE GameNestDB;
GO

CREATE TABLE dbo.AuditLogs (
    audit_log_id    INT             IDENTITY(1,1)   NOT NULL,

    -- Actor (snapshot tại thời điểm hành động — không phụ thuộc JOIN Accounts)
    account_id      INT             NULL,
    username        NVARCHAR(50)    NOT NULL,
    role_name       VARCHAR(20)     NOT NULL,

    -- Action
    module          VARCHAR(30)     NOT NULL,
    action          VARCHAR(30)     NOT NULL,

    -- Target
    target_id       INT             NULL,
    target_type     VARCHAR(30)     NULL,

    -- Description
    description     NVARCHAR(500)   NULL,

    -- Request information
    ip_address      VARCHAR(45)     NULL,   -- đủ cho cả IPv4 và IPv6
    user_agent      NVARCHAR(255)   NULL,

    -- Timestamp
    created_at      DATETIME2(0)    NOT NULL CONSTRAINT DF_AuditLogs_created_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_AuditLogs PRIMARY KEY (audit_log_id),
    CONSTRAINT FK_AuditLogs_Accounts FOREIGN KEY (account_id) REFERENCES dbo.Accounts (account_id),

    -- Giai đoạn này chỉ tích hợp Audit Log cho Accounts và Games; mở rộng
    -- thêm module/target_type sau này cần một migration ALTER riêng (cùng
    -- kiểu additive như 07_otp_change_email_purpose.sql).
    CONSTRAINT CK_AuditLogs_module CHECK (module IN ('ACCOUNTS', 'GAMES')),
    CONSTRAINT CK_AuditLogs_action CHECK (action IN ('CREATE', 'UPDATE', 'STATUS_CHANGE')),
    CONSTRAINT CK_AuditLogs_target_type CHECK (target_type IN ('ACCOUNT', 'GAME'))
);
GO

-- created_at DESC: sort mặc định của trang danh sách (mới nhất trước)
CREATE INDEX IX_AuditLogs_created_at ON dbo.AuditLogs (created_at DESC);
-- lọc theo actor
CREATE INDEX IX_AuditLogs_account_id ON dbo.AuditLogs (account_id);
-- lọc theo Module + Action (bộ lọc trên UI)
CREATE INDEX IX_AuditLogs_module_action ON dbo.AuditLogs (module, action);
-- lọc/tra cứu theo target
CREATE INDEX IX_AuditLogs_target ON dbo.AuditLogs (target_type, target_id);
GO

-- Không GRANT DELETE cho AuditLogs: GameNestSvcLogin đã bị DENY DELETE ở
-- cấp schema (xem 06_gamenest_svc_login.sql) nên mặc định application
-- không có quyền xoá bảng này — đúng chính sách append-only.
