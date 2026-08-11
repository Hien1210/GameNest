-- ============================================================
-- GameNest — Final working application Login/User (replaces GameNestAppLogin)
--
-- LỊCH SỬ: GameNestAppLogin/GameNestAppUser bị "hỏng" vĩnh viễn sau nhiều lần
-- DROP/CREATE lặp lại trong quá trình debug — không rõ nguyên nhân chính xác,
-- nhưng test với login/user hoàn toàn mới đã chứng minh vấn đề không phải do
-- thiết kế hay do SQL Server nói chung.
--
-- BÀI HỌC QUAN TRỌNG: DENY CONTROL ở cấp SCHEMA sẽ chặn ngầm SELECT/INSERT/
-- UPDATE dù các quyền đó được GRANT tường minh riêng. KHÔNG dùng DENY CONTROL
-- trong thiết kế least-privilege — chỉ cần DENY ALTER (chặn sửa schema) và
-- DENY DELETE (chặn hard-delete) là đủ để đạt mục tiêu bảo mật của CLAUDE.md
-- mục 7/8, không cần DENY CONTROL.
--
-- Chạy bằng tài khoản admin/DBA (sa)
-- ============================================================

USE GameNestDB;
GO

IF EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'GameNestSvcUser')
    DROP USER GameNestSvcUser;
GO

USE master;
GO

IF EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'GameNestSvcLogin')
    DROP LOGIN GameNestSvcLogin;
GO

CREATE LOGIN GameNestSvcLogin
    WITH PASSWORD = 'Thanhhien@2008',
    CHECK_POLICY = OFF,
    CHECK_EXPIRATION = OFF;
GO

USE GameNestDB;
GO

CREATE USER GameNestSvcUser FOR LOGIN GameNestSvcLogin;
GO

-- Quyền tối thiểu, cấp trực tiếp cho user (không qua role — tránh mọi rủi ro
-- còn sót lại từ GameNestAppRole cũ)
GRANT SELECT, INSERT, UPDATE, EXECUTE ON SCHEMA::dbo TO GameNestSvcUser;
GO

-- CHỈ deny ALTER + DELETE — KHÔNG deny CONTROL (xem ghi chú ở đầu file)
DENY DELETE, ALTER ON SCHEMA::dbo TO GameNestSvcUser;
GO

-- Bảng junction: cho phép DELETE thật (đúng mục 7.3 CLAUDE.md)
GRANT DELETE ON dbo.LFGMembers TO GameNestSvcUser;
GO

-- Xác minh
EXECUTE AS USER = 'GameNestSvcUser';
SELECT TOP 1 * FROM dbo.Accounts;
REVERT;
GO
