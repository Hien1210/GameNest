-- ============================================================
-- GameNest — Add MODERATOR role
-- Migration additive: chỉ mở rộng danh sách giá trị hợp lệ của
-- CK_Accounts_role. Không đổi dữ liệu Account hiện có, không đổi
-- USER/ADMIN đang tồn tại.
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestSvcLogin/AppLogin)
-- ============================================================

USE GameNestDB;
GO

ALTER TABLE dbo.Accounts
    DROP CONSTRAINT CK_Accounts_role;
GO

ALTER TABLE dbo.Accounts
    ADD CONSTRAINT CK_Accounts_role CHECK (role IN ('USER', 'ADMIN', 'MODERATOR'));
GO

-- Không có cơ chế tự đăng ký/tự nâng cấp thành MODERATOR trong ứng dụng
-- (tránh leo thang đặc quyền), giống hệt quy tắc đã áp dụng cho ADMIN.
-- Phải tự tay nâng một tài khoản lên MODERATOR bằng DBA sau khi chạy
-- migration này:
--
-- UPDATE dbo.Accounts SET role = 'MODERATOR' WHERE username = '<đổi thành username cần nâng>';
