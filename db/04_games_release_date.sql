-- ============================================================
-- GameNest — Add release_date to Games
-- Cần thiết vì User flow (mục 4, 6 của yêu cầu Games module) yêu cầu hiển thị
-- release date ở Game List và Game Detail. Cột nullable nên không ảnh hưởng
-- dữ liệu hiện có.
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestAppLogin)
-- ============================================================

USE GameNestDB;
GO

ALTER TABLE dbo.Games
    ADD release_date DATE NULL;
GO
