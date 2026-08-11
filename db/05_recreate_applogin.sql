-- ============================================================
-- GameNest — Recreate GameNestAppLogin from scratch
-- Dùng khi login bị lỗi "Login failed" (Error 18456) không rõ nguyên nhân
-- dù mật khẩu đã xác nhận đúng, không bị disable/locked.
-- Chạy bằng tài khoản admin/DBA (sa)
-- ============================================================

USE master;
GO

-- Xóa login cũ (không ảnh hưởng GameNestAppUser/quyền trong GameNestDB,
-- chỉ làm user đó tạm "orphaned" — sẽ gắn lại ở bước dưới)
IF EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = 'GameNestAppLogin')
BEGIN
    DROP LOGIN GameNestAppLogin;
END
GO

CREATE LOGIN GameNestAppLogin
    WITH PASSWORD = 'Thanhhien@2008',
    CHECK_POLICY = OFF,
    CHECK_EXPIRATION = OFF;
GO

-- Gắn lại GameNestAppUser (đã có sẵn trong GameNestDB) với login mới này
USE GameNestDB;
GO

ALTER USER GameNestAppUser WITH LOGIN = GameNestAppLogin;
GO

-- Kiểm tra lại: phải thấy is_disabled = 0
SELECT name, is_disabled, type_desc, LEN(name) AS name_length
FROM sys.sql_logins
WHERE name = 'GameNestAppLogin';
GO
