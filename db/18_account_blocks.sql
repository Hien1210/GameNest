-- ============================================================
-- GameNest — User Block (one-way row, two-sided restriction)
--
-- Tạo bảng AccountBlocks — junction/relationship table thuần (giống
-- LFGMembers/AccountGames/AccountFollows): không status, không soft-delete.
-- Unblock là hard DELETE thật theo CLAUDE.md mục 7.3.
--
-- QUAN TRỌNG — Block có side effects lên Social layer hiện có (thực thi
-- trong AccountBlockService.block, cùng transaction với INSERT dưới đây,
-- KHÔNG thuộc phạm vi migration này vì đây là business logic, không phải
-- schema):
--   1. Xóa AccountFollows cả 2 chiều (A→B và B→A) — hard DELETE, đã có
--      GRANT DELETE từ db/16_account_follows.sql.
--   2. AccountFriendships đang ACCEPTED giữa A/B (bất kỳ chiều nào)
--      → chuyển UNFRIENDED (UPDATE, không cần quyền DELETE mới).
--   3. AccountFriendships đang PENDING giữa A/B (bất kỳ chiều nào)
--      → chuyển CANCELLED (UPDATE, không cần quyền DELETE mới).
-- Không tạo NotificationType mới cho Block — Block/Unblock không tạo
-- Notification (task spec §13).
--
-- Chạy bằng tài khoản admin/DBA (sa) — giống các migration trước.
-- ============================================================

USE GameNestDB;
GO

CREATE TABLE dbo.AccountBlocks (
    block_id                 INT             IDENTITY(1,1)   NOT NULL,
    blocker_account_id       INT             NOT NULL,
    blocked_account_id       INT             NOT NULL,
    created_at                 DATETIME2(0)    NOT NULL CONSTRAINT DF_AccountBlocks_created_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_AccountBlocks PRIMARY KEY (block_id),
    CONSTRAINT FK_AccountBlocks_Blocker FOREIGN KEY (blocker_account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT FK_AccountBlocks_Blocked FOREIGN KEY (blocked_account_id) REFERENCES dbo.Accounts (account_id),

    -- Chặn duplicate cùng chiều ở tầng DB (backstop cho Service-level
    -- idempotent check). Chiều ngược lại (B, A) là một cặp khác về UNIQUE —
    -- nghĩa là cả A→B và B→A CÓ THỂ cùng tồn tại (chặn lẫn nhau), đúng theo
    -- task spec §10 "Block restriction có hiệu lực hai chiều" mà không cần
    -- một row đại diện quan hệ hai chiều.
    CONSTRAINT UQ_AccountBlocks_pair UNIQUE (blocker_account_id, blocked_account_id),

    -- A không thể tự block chính mình — chặn ở tầng DB, ngoài check Service.
    CONSTRAINT CK_AccountBlocks_not_self CHECK (blocker_account_id <> blocked_account_id)
);
GO

-- Phục vụ: "Tôi đang block những ai?" (Blocked Users list, task spec §15).
CREATE INDEX IX_AccountBlocks_blocker ON dbo.AccountBlocks (blocker_account_id);
-- Phục vụ: isBlockedBetween(A, B) — kiểm tra chiều B→A khi B là target.
CREATE INDEX IX_AccountBlocks_blocked ON dbo.AccountBlocks (blocked_account_id);
GO

-- Bảng junction: cho phép DELETE thật khi Unblock (đúng mục 7.3 CLAUDE.md,
-- cùng convention với LFGMembers/AccountGames/AccountFollows).
-- GameNestSvcLogin bị DENY DELETE ở cấp schema (06_gamenest_svc_login.sql)
-- nên cần GRANT riêng.
GRANT DELETE ON dbo.AccountBlocks TO GameNestSvcUser;
GO
