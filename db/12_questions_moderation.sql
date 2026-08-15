-- ============================================================
-- GameNest — Questions Moderation (Moderator)
-- Migration additive: KHÔNG tạo bảng mới, KHÔNG đổi schema Questions —
-- schema hiện tại đã đủ status/created_at/updated_at/account_id/game_id.
-- Chỉ mở rộng CHECK constraint của AuditLogs để Audit Log nhận module
-- QUESTIONS và target_type QUESTION. action STATUS_CHANGE đã tồn tại sẵn
-- (dùng lại, không cần đổi CK_AuditLogs_action).
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestSvcLogin/AppLogin)
-- ============================================================

USE GameNestDB;
GO

ALTER TABLE dbo.AuditLogs
    DROP CONSTRAINT CK_AuditLogs_module;
GO
ALTER TABLE dbo.AuditLogs
    ADD CONSTRAINT CK_AuditLogs_module CHECK (module IN ('ACCOUNTS', 'GAMES', 'REPORTS', 'SETTINGS', 'QUESTIONS'));
GO

ALTER TABLE dbo.AuditLogs
    DROP CONSTRAINT CK_AuditLogs_target_type;
GO
ALTER TABLE dbo.AuditLogs
    ADD CONSTRAINT CK_AuditLogs_target_type CHECK (target_type IN ('ACCOUNT', 'GAME', 'REPORT', 'SETTING', 'QUESTION'));
GO

-- Không thêm index mới: IX_Questions_status, IX_Questions_game_id,
-- IX_Questions_account_id (từ 01_core_schema.sql) đã đủ phục vụ các filter
-- của trang Questions Moderation (status/game_id/account_id).
