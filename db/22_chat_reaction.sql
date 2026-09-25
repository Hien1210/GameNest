-- ============================================================
-- GameNest — Chat Advanced Feature #2: Message Reaction
--
-- 1 bảng mới, additive-only, không ALTER bất kỳ migration nào trước đó
-- (không ALTER Messages — reaction KHÔNG lưu trên Messages, xem lý do bên
-- dưới).
--
-- THIẾT KẾ:
--
-- MessageReactions là junction/relationship table đúng nghĩa CLAUDE.md mục
-- 7.3: mỗi dòng chỉ đại diện "Account X hiện đang thả emoji Y lên Message
-- Z" — một quan hệ HIỆN TẠI, không phải nội dung lịch sử cần giữ lại. Vì
-- vậy được phép hard-delete thật khi user bỏ reaction (un-react), giống hệt
-- AccountFollows/QuestionVotes/LFGMembers/TeamMembers.
--
-- Không thêm reaction vào bảng Messages (không thêm cột đếm, không thêm
-- JSON) vì quan hệ là 1-N (nhiều account có thể react 1 message, mỗi
-- account tối đa 1 reaction/message) — không thể biểu diễn an toàn bằng
-- cột đơn trên Messages mà không phá vỡ tính chuẩn hoá.
--
-- UNIQUE(message_id, account_id): mỗi account chỉ có TỐI ĐA 1 reaction
-- đang active trên 1 message tại một thời điểm — đổi emoji = UPDATE dòng
-- hiện có, không phải thêm dòng mới. Đây là an toàn cuối cùng chống race
-- condition khi 2 request đổi reaction cùng lúc (xem MessageReactionDAO —
-- không dùng MERGE, dùng UPDATE-rồi-INSERT với retry khi đụng UNIQUE).
--
-- CHECK(emoji IN (...)) — allowlist cố định 6 emoji, chặn ở tầng DB để
-- không phụ thuộc hoàn toàn vào validate phía Java (CLAUDE.md mục 9).
--
-- Không có deleted_at trên bảng này — un-react là xoá quan hệ thật, không
-- phải nội dung cộng đồng cần audit trail.
--
-- Chạy bằng tài khoản admin/DBA (sa) — giống các migration trước.
-- ============================================================

USE GameNestDB;
GO

CREATE TABLE dbo.MessageReactions (
    message_reaction_id    INT             IDENTITY(1,1)   NOT NULL,
    message_id              INT             NOT NULL,
    account_id                INT             NOT NULL,
    emoji                       NVARCHAR(10)    NOT NULL,
    created_at                     DATETIME2(0)    NOT NULL CONSTRAINT DF_MessageReactions_created_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_MessageReactions PRIMARY KEY (message_reaction_id),
    CONSTRAINT FK_MessageReactions_Messages FOREIGN KEY (message_id) REFERENCES dbo.Messages (message_id),
    CONSTRAINT FK_MessageReactions_Accounts FOREIGN KEY (account_id) REFERENCES dbo.Accounts (account_id),

    -- Mỗi account tối đa 1 reaction đang active trên 1 message.
    CONSTRAINT UQ_MessageReactions_message_account UNIQUE (message_id, account_id),

    -- Allowlist cố định — chặn ở tầng DB, không chỉ ở Service.
    CONSTRAINT CK_MessageReactions_emoji CHECK (emoji IN (N'👍', N'❤️', N'😂', N'😮', N'😢', N'😡'))
);
GO

-- Phục vụ: tổng hợp reaction (GROUP BY emoji) và tra "reaction của tôi" cho
-- 1 message hoặc 1 trang message khi load Chat Detail.
CREATE INDEX IX_MessageReactions_message_id ON dbo.MessageReactions (message_id);
GO

-- Bảng junction: cho phép DELETE thật khi un-react (đúng mục 7.3
-- CLAUDE.md, cùng convention với ConversationMembers/AccountFollows/
-- TeamMembers). GameNestSvcLogin bị DENY DELETE ở cấp schema
-- (06_gamenest_svc_login.sql) nên cần GRANT riêng.
GRANT DELETE ON dbo.MessageReactions TO GameNestSvcUser;
GO
