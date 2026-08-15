-- ============================================================
-- GameNest — Answers Moderation (Moderator)
-- Migration additive: KHÔNG tạo bảng mới, KHÔNG đổi schema cột của Answers.
-- Answers hiện tại đã đủ status/is_deleted/deleted_at/deleted_by/created_at/
-- updated_at/account_id/question_id cho nghiệp vụ moderation.
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestSvcLogin/AppLogin)
-- ============================================================

USE GameNestDB;
GO

-- 1) Mở rộng CHECK constraint của AuditLogs để nhận module ANSWERS và
--    target_type ANSWER. action STATUS_CHANGE đã tồn tại sẵn (dùng lại,
--    không cần đổi CK_AuditLogs_action).
ALTER TABLE dbo.AuditLogs
    DROP CONSTRAINT CK_AuditLogs_module;
GO
ALTER TABLE dbo.AuditLogs
    ADD CONSTRAINT CK_AuditLogs_module CHECK (module IN ('ACCOUNTS', 'GAMES', 'REPORTS', 'SETTINGS', 'QUESTIONS', 'ANSWERS'));
GO

ALTER TABLE dbo.AuditLogs
    DROP CONSTRAINT CK_AuditLogs_target_type;
GO
ALTER TABLE dbo.AuditLogs
    ADD CONSTRAINT CK_AuditLogs_target_type CHECK (target_type IN ('ACCOUNT', 'GAME', 'REPORT', 'SETTING', 'QUESTION', 'ANSWER'));
GO

-- 2) Index mới — lý do cụ thể: trang Moderator Answers List
--    (ModeratorAnswersServlet -> AnswerDAO.searchForModeration/countForModeration)
--    lọc theo "WHERE an.status = ?". Questions đã có IX_Questions_status
--    tương ứng từ trước (01_core_schema.sql); Answers thì chưa có index nào
--    trên cột status, chỉ có IX_Answers_question_id/IX_Answers_account_id.
--    Không ảnh hưởng dữ liệu hiện có, không ảnh hưởng flow USER/Admin.
CREATE INDEX IX_Answers_status ON dbo.Answers (status);
GO
