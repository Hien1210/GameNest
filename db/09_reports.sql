-- ============================================================
-- GameNest — Reports module (báo cáo Account/Question/Answer)
-- Migration additive: tạo bảng Reports mới + mở rộng CHECK constraint của
-- AuditLogs để tích hợp Audit Log cho Reports. Không đổi dữ liệu hiện có.
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestSvcLogin/AppLogin)
-- ============================================================

USE GameNestDB;
GO

-- ============================================================
-- 1. Reports
-- ============================================================
CREATE TABLE dbo.Reports (
    report_id               INT             IDENTITY(1,1)   NOT NULL,

    -- Người tạo report (luôn lấy từ session, không bao giờ từ client)
    reporter_account_id     INT             NOT NULL,

    -- Target: polymorphic reference — không tạo FK SQL trực tiếp tới nhiều
    -- bảng; Service kiểm tra target tồn tại/hợp lệ trước khi tạo Report.
    target_id                INT             NOT NULL,
    target_type              VARCHAR(20)     NOT NULL,

    reason                   VARCHAR(30)     NOT NULL,
    description              NVARCHAR(1000)  NULL,

    status                   VARCHAR(20)     NOT NULL CONSTRAINT DF_Reports_status DEFAULT ('PENDING'),

    -- Xử lý bởi Admin — nullable cho tới khi được review
    reviewed_by              INT             NULL,
    reviewed_at               DATETIME2(0)    NULL,
    resolution_note           NVARCHAR(1000)  NULL,

    created_at                DATETIME2(0)    NOT NULL CONSTRAINT DF_Reports_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at                 DATETIME2(0)    NULL,

    CONSTRAINT PK_Reports PRIMARY KEY (report_id),
    CONSTRAINT FK_Reports_Reporter FOREIGN KEY (reporter_account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT FK_Reports_ReviewedBy FOREIGN KEY (reviewed_by) REFERENCES dbo.Accounts (account_id),

    CONSTRAINT CK_Reports_target_type CHECK (target_type IN ('ACCOUNT', 'QUESTION', 'ANSWER')),
    CONSTRAINT CK_Reports_reason CHECK (reason IN
        ('SPAM', 'HARASSMENT', 'INAPPROPRIATE_CONTENT', 'HATE_SPEECH', 'MISINFORMATION', 'CHEATING', 'OTHER')),
    CONSTRAINT CK_Reports_status CHECK (status IN ('PENDING', 'RESOLVED', 'REJECTED'))
);
GO

-- Query thực tế: list Admin lọc theo status; lọc theo target; lọc theo
-- reporter; sort mặc định created_at DESC.
CREATE INDEX IX_Reports_status ON dbo.Reports (status);
CREATE INDEX IX_Reports_target ON dbo.Reports (target_type, target_id);
CREATE INDEX IX_Reports_reporter ON dbo.Reports (reporter_account_id);
CREATE INDEX IX_Reports_created_at ON dbo.Reports (created_at DESC);
GO

-- Chống spam report: 1 Account chỉ được có tối đa 1 Report PENDING cho
-- cùng 1 target. Filtered unique index — chỉ áp dụng khi status = PENDING
-- nên không chặn việc tạo report mới sau khi report cũ đã RESOLVED/REJECTED.
-- Đây là lớp phòng thủ ở DB, bổ sung cho pre-check ở Service layer.
CREATE UNIQUE INDEX UQ_Reports_pending_target
    ON dbo.Reports (reporter_account_id, target_type, target_id)
    WHERE status = 'PENDING';
GO

-- Không GRANT DELETE cho Reports: GameNestSvcLogin đã bị DENY DELETE ở cấp
-- schema (xem 06_gamenest_svc_login.sql) nên application mặc định không có
-- quyền xóa bảng này — đúng chính sách không Hard Delete cho Reports.

-- ============================================================
-- 2. Mở rộng AuditLogs cho module REPORTS
-- Cùng kiểu additive như 07_otp_change_email_purpose.sql: chỉ mở rộng danh
-- sách giá trị hợp lệ, không đổi dữ liệu cũ, không ảnh hưởng Audit Log của
-- Accounts/Games đang chạy.
-- ============================================================

ALTER TABLE dbo.AuditLogs
    DROP CONSTRAINT CK_AuditLogs_module;
GO
ALTER TABLE dbo.AuditLogs
    ADD CONSTRAINT CK_AuditLogs_module CHECK (module IN ('ACCOUNTS', 'GAMES', 'REPORTS'));
GO

ALTER TABLE dbo.AuditLogs
    DROP CONSTRAINT CK_AuditLogs_action;
GO
ALTER TABLE dbo.AuditLogs
    ADD CONSTRAINT CK_AuditLogs_action CHECK (action IN ('CREATE', 'UPDATE', 'STATUS_CHANGE', 'RESOLVE', 'REJECT'));
GO

ALTER TABLE dbo.AuditLogs
    DROP CONSTRAINT CK_AuditLogs_target_type;
GO
ALTER TABLE dbo.AuditLogs
    ADD CONSTRAINT CK_AuditLogs_target_type CHECK (target_type IN ('ACCOUNT', 'GAME', 'REPORT'));
GO
