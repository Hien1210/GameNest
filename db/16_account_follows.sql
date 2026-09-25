-- ============================================================
-- GameNest — User Follow (one-way, no approval)
--
-- Tạo bảng AccountFollows — junction/relationship table thuần (giống
-- LFGMembers/AccountGames): không có status/soft-delete, Unfollow là hard
-- DELETE thật theo CLAUDE.md mục 7.3. KHÔNG tạo bảng UserFollows/FollowHistory
-- khác, KHÔNG ALTER Accounts.
--
-- Đồng thời mở rộng CK_Notifications_type (đã tạo ở db/14_user_foundation.sql)
-- để hỗ trợ type FOLLOW — không tạo lại bảng Notifications, không đổi
-- target_type (ACCOUNT đã tồn tại sẵn trong CK_Notifications_target_type).
--
-- Chạy bằng tài khoản admin/DBA (sa) — giống 14_user_foundation.sql/
-- 15_account_games_delete_grant.sql.
-- ============================================================

USE GameNestDB;
GO

-- ============================================================
-- 1. AccountFollows — quan hệ một chiều A Follow B
-- ============================================================
CREATE TABLE dbo.AccountFollows (
    follow_id                INT             IDENTITY(1,1)   NOT NULL,
    follower_account_id      INT             NOT NULL,
    following_account_id     INT             NOT NULL,
    created_at                DATETIME2(0)    NOT NULL CONSTRAINT DF_AccountFollows_created_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_AccountFollows PRIMARY KEY (follow_id),
    CONSTRAINT FK_AccountFollows_Follower FOREIGN KEY (follower_account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT FK_AccountFollows_Following FOREIGN KEY (following_account_id) REFERENCES dbo.Accounts (account_id),

    -- A không thể có 2 row Follow cùng 1 B (chặn duplicate Follow ở tầng DB,
    -- ngoài check ở Service).
    CONSTRAINT UQ_AccountFollows_pair UNIQUE (follower_account_id, following_account_id),

    -- A không thể tự Follow chính mình — chặn ở tầng DB, ngoài check ở Service.
    CONSTRAINT CK_AccountFollows_not_self CHECK (follower_account_id <> following_account_id)
);
GO

-- Phục vụ: "Tôi đang Follow ai?" (Following list)
CREATE INDEX IX_AccountFollows_follower ON dbo.AccountFollows (follower_account_id);
-- Phục vụ: "Ai đang Follow tôi?" (Followers list)
CREATE INDEX IX_AccountFollows_following ON dbo.AccountFollows (following_account_id);
GO

-- Bảng junction: cho phép DELETE thật khi Unfollow (đúng mục 7.3 CLAUDE.md,
-- cùng convention với LFGMembers/AccountGames). GameNestSvcLogin bị DENY
-- DELETE ở cấp schema (06_gamenest_svc_login.sql) nên cần GRANT riêng, y hệt
-- 15_account_games_delete_grant.sql.
GRANT DELETE ON dbo.AccountFollows TO GameNestSvcUser;
GO

-- ============================================================
-- 2. Notifications — mở rộng CK_Notifications_type để hỗ trợ FOLLOW.
-- target_type dùng lại ACCOUNT (đã có sẵn trong CK_Notifications_target_type
-- từ db/14_user_foundation.sql) — không đổi constraint đó.
-- ============================================================
ALTER TABLE dbo.Notifications DROP CONSTRAINT CK_Notifications_type;
GO

ALTER TABLE dbo.Notifications ADD CONSTRAINT CK_Notifications_type CHECK (type IN
    ('ANSWER_ACCEPTED', 'ANSWER_REPLY', 'REPORT_RESOLVED', 'REPORT_REJECTED', 'SYSTEM', 'FOLLOW'));
GO
