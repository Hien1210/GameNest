-- ============================================================
-- GameNest — System Settings (Cấu hình hệ thống)
-- Migration additive: tạo bảng SystemSettings mới (generic key/value) +
-- mở rộng CHECK constraint của AuditLogs để tích hợp Audit Log cho
-- Settings. Không đổi dữ liệu Accounts/Games/Questions/Answers hiện có.
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestSvcLogin/AppLogin)
-- ============================================================

USE GameNestDB;
GO

-- ============================================================
-- 1. SystemSettings
-- ============================================================
CREATE TABLE dbo.SystemSettings (
    setting_id       INT             IDENTITY(1,1)   NOT NULL,
    setting_key      VARCHAR(100)    NOT NULL,
    setting_value    NVARCHAR(500)   NOT NULL,
    setting_type     VARCHAR(20)     NOT NULL,
    description       NVARCHAR(255)   NULL,

    -- Ai cập nhật lần gần nhất — NULL cho tới khi Admin sửa lần đầu
    -- (giá trị mặc định do migration INSERT chưa có actor).
    updated_by         INT             NULL,
    updated_at          DATETIME2(0)    NOT NULL CONSTRAINT DF_SystemSettings_updated_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_SystemSettings PRIMARY KEY (setting_id),
    -- UNIQUE constraint tự tạo unique index trên setting_key — không cần
    -- tạo thêm index riêng (tránh index thừa).
    CONSTRAINT UQ_SystemSettings_key UNIQUE (setting_key),
    CONSTRAINT FK_SystemSettings_UpdatedBy FOREIGN KEY (updated_by) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT CK_SystemSettings_type CHECK (setting_type IN ('STRING', 'INTEGER', 'BOOLEAN', 'ENUM'))
);
GO

-- ============================================================
-- 2. Default settings — giá trị phải khớp behavior hiện tại của hệ thống
-- (OtpService đang hard-code 5 phút / 60 giây / 5 lần thử; các cờ
-- USER/COMMUNITY mặc định bật vì các chức năng này hiện đang hoạt động
-- bình thường, không có cơ chế tắt).
-- ============================================================
INSERT INTO dbo.SystemSettings (setting_key, setting_value, setting_type, description) VALUES
    ('site.name', N'GameNest', 'STRING', N'Tên hệ thống hiển thị'),
    ('site.description', N'Gaming Community Platform', 'STRING', N'Mô tả ngắn của hệ thống'),
    ('system.status', 'ONLINE', 'ENUM', N'Trạng thái hệ thống: ONLINE hoặc MAINTENANCE'),
    ('registration.enabled', 'true', 'BOOLEAN', N'Cho phép đăng ký tài khoản mới'),
    ('questions.enabled', 'true', 'BOOLEAN', N'Cho phép tạo Question mới'),
    ('answers.enabled', 'true', 'BOOLEAN', N'Cho phép tạo Answer mới'),
    ('reports.enabled', 'true', 'BOOLEAN', N'Cho phép gửi Report mới'),
    ('otp.expiration_minutes', '5', 'INTEGER', N'Thời gian hết hạn OTP (phút)'),
    ('otp.resend_cooldown_seconds', '60', 'INTEGER', N'Thời gian chờ giữa hai lần gửi lại OTP (giây)'),
    ('otp.max_attempts', '5', 'INTEGER', N'Số lần nhập sai OTP tối đa trước khi bị từ chối');
GO

-- Không GRANT DELETE cho SystemSettings: GameNestSvcLogin đã bị DENY DELETE
-- ở cấp schema (xem 06_gamenest_svc_login.sql) nên application mặc định
-- không có quyền xóa bảng này — đúng chính sách không Hard Delete Settings.

-- ============================================================
-- 3. Mở rộng AuditLogs cho module SETTINGS
-- Cùng kiểu additive như 07/09: chỉ mở rộng danh sách giá trị hợp lệ,
-- không đổi dữ liệu Audit Log cũ. action UPDATE đã tồn tại sẵn nên không
-- cần đổi CK_AuditLogs_action.
-- ============================================================

ALTER TABLE dbo.AuditLogs
    DROP CONSTRAINT CK_AuditLogs_module;
GO
ALTER TABLE dbo.AuditLogs
    ADD CONSTRAINT CK_AuditLogs_module CHECK (module IN ('ACCOUNTS', 'GAMES', 'REPORTS', 'SETTINGS'));
GO

ALTER TABLE dbo.AuditLogs
    DROP CONSTRAINT CK_AuditLogs_target_type;
GO
ALTER TABLE dbo.AuditLogs
    ADD CONSTRAINT CK_AuditLogs_target_type CHECK (target_type IN ('ACCOUNT', 'GAME', 'REPORT', 'SETTING'));
GO
