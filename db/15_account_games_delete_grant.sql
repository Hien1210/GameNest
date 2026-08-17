-- ============================================================
-- GameNest — GRANT DELETE on AccountGames (User Profile: Playing/Favorite Games)
--
-- BỐI CẢNH: db/14_user_foundation.sql tạo AccountGames là junction table
-- thuần (giống LFGMembers) và CỐ Ý KHÔNG cấp quyền DELETE lúc đó vì task
-- đó không xây business logic. Task "User Profile" hiện tại triển khai
-- chức năng "Bỏ game khỏi Đang chơi/Yêu thích" — về code, thao tác này chỉ
-- xóa ĐÚNG 1 dòng quan hệ (account_id + game_id + relationship_type) của
-- chính người dùng, KHÔNG xóa Game, KHÔNG xóa Account, KHÔNG phải hard
-- delete nội dung lịch sử.
--
-- GameNestSvcLogin hiện bị DENY DELETE ở cấp schema (06_gamenest_svc_login.sql:
-- "DENY DELETE, ALTER ON SCHEMA::dbo") — nên AccountGames hiện KHÔNG có
-- quyền DELETE, mã "Bỏ game" đã viết đúng nhưng sẽ ném SQLException cho tới
-- khi migration này được chạy.
--
-- Đây là đúng convention đã có sẵn cho LFGMembers (dòng
-- "GRANT DELETE ON dbo.LFGMembers TO GameNestSvcUser;" trong
-- 06_gamenest_svc_login.sql) — CLAUDE.md mục 7.3 cho phép hard-delete trên
-- bảng junction/quan hệ hiện tại (không phải nội dung lịch sử). Migration
-- này áp dụng đúng ngoại lệ đó cho AccountGames, không mở rộng quyền gì
-- khác.
--
-- Chạy bằng tài khoản admin/DBA (sa) — GIỐNG 06_gamenest_svc_login.sql.
-- ============================================================

USE GameNestDB;
GO

GRANT DELETE ON dbo.AccountGames TO GameNestSvcUser;
GO
