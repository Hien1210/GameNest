-- ============================================================
-- GameNest — Add role to Accounts (required for Admin authorization)
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestAppLogin)
-- ============================================================

USE GameNestDB;
GO

ALTER TABLE dbo.Accounts
    ADD role VARCHAR(20) NOT NULL CONSTRAINT DF_Accounts_role DEFAULT ('USER');
GO

ALTER TABLE dbo.Accounts
    ADD CONSTRAINT CK_Accounts_role CHECK (role IN ('USER', 'ADMIN'));
GO

-- Không có cơ chế tự đăng ký làm ADMIN trong ứng dụng (tránh leo thang đặc quyền).
-- Phải tự tay nâng một tài khoản lên ADMIN bằng DBA sau khi chạy migration này:
--
-- UPDATE dbo.Accounts SET role = 'ADMIN' WHERE username = '<đổi thành username của bạn>';
