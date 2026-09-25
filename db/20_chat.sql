-- ============================================================
-- GameNest — Chat Core Logic (HTTP only — no WebSocket/Realtime)
--
-- 3 bảng mới, additive-only, không ALTER bảng cũ (không có ALTER
-- Notifications ở migration này — task này CỐ Ý không thêm
-- NotificationType.MESSAGE_RECEIVED, xem TeamService/ChatService).
--
-- Một Conversation architecture DUY NHẤT cho cả DIRECT và TEAM chat —
-- không tạo DirectMessages/TeamMessages riêng.
--
-- Thứ tự tạo bảng: Conversations → Messages → ConversationMembers, vì
-- ConversationMembers.last_read_message_id FK tới Messages (cần Messages
-- tồn tại trước khi tạo FK đó).
--
-- THIẾT KẾ:
--
-- 1. Conversations — type DIRECT/TEAM dùng chung 1 bảng.
--    - TEAM: team_id NOT NULL, direct_key NULL. UNIQUE lọc theo
--      type='TEAM' đảm bảo mỗi Team ACTIVE chỉ có đúng 1 Team Conversation
--      (cùng khuôn mẫu UQ_TeamMembers_team_owner ở db/19_teams.sql).
--    - DIRECT: team_id NULL, direct_key NOT NULL. direct_key là canonical
--      key "min(accountA,accountB):max(accountA,accountB)" — luôn giống
--      nhau dù request đến từ chiều nào (tính ở ChatService, không ở DB).
--      UNIQUE(direct_key) đảm bảo 1 cặp Friend chỉ có tối đa 1 Direct
--      Conversation ở tầng database (không chỉ SELECT-rồi-INSERT).
--    - status hiện chỉ có giá trị ACTIVE — Conversation không tự có
--      lifecycle riêng: với TEAM, "còn dùng được hay không" phụ thuộc vào
--      Teams.status (kiểm tra lại ở ChatService mỗi lần truy cập), không
--      trùng lặp một cờ "deleted" thứ hai dễ lệch dữ liệu. Không tạo thêm
--      giá trị status nào khác vì task không định nghĩa rõ ý nghĩa của
--      chúng (xem "Quyết định thiết kế" cuối file).
--
-- 2. Messages — user-generated content, KHÔNG hard-delete (CLAUDE.md mục
--    7.1). Xóa tin nhắn = soft delete (deleted_at). content NVARCHAR(2000)
--    — không có convention độ dài tin nhắn chat có sẵn trong project, chọn
--    2000 ký tự (ngắn hơn nhiều so với Question/Answer NVARCHAR(MAX), phù
--    hợp bản chất tin nhắn ngắn của chat) — xem "Quyết định thiết kế".
--
-- 3. ConversationMembers — pure junction table (giống TeamMembers/
--    LFGMembers) — Leave/Remove Member = hard DELETE thật. left_at có
--    trong schema theo đúng yêu cầu task, nhưng KHÔNG được set bởi bất kỳ
--    code path nào trong phase này (Leave/Remove dùng hard-delete, không
--    dùng soft-leave) — xem "Quyết định thiết kế".
--
-- Chạy bằng tài khoản admin/DBA (sa) — giống các migration trước.
-- ============================================================

USE GameNestDB;
GO

-- ============================================================
-- 1. Conversations
-- ============================================================
CREATE TABLE dbo.Conversations (
    conversation_id      INT             IDENTITY(1,1)   NOT NULL,
    type                   VARCHAR(20)     NOT NULL,
    team_id                 INT             NULL,
    direct_key               VARCHAR(50)     NULL,
    status                     VARCHAR(20)     NOT NULL CONSTRAINT DF_Conversations_status DEFAULT ('ACTIVE'),
    created_at                   DATETIME2(0)    NOT NULL CONSTRAINT DF_Conversations_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at                     DATETIME2(0)    NULL,

    CONSTRAINT PK_Conversations PRIMARY KEY (conversation_id),
    CONSTRAINT FK_Conversations_Teams FOREIGN KEY (team_id) REFERENCES dbo.Teams (team_id),

    CONSTRAINT CK_Conversations_type CHECK (type IN ('DIRECT', 'TEAM')),
    CONSTRAINT CK_Conversations_status CHECK (status IN ('ACTIVE')),

    -- Bất biến cấu trúc: TEAM luôn có team_id, không có direct_key; DIRECT
    -- luôn ngược lại — chặn ở tầng DB, không chỉ ở Service.
    CONSTRAINT CK_Conversations_type_consistency CHECK (
        (type = 'TEAM' AND team_id IS NOT NULL AND direct_key IS NULL) OR
        (type = 'DIRECT' AND team_id IS NULL AND direct_key IS NOT NULL)
    ),

    -- Một cặp Account chỉ có tối đa 1 Direct Conversation (NULL không xung
    -- đột nhau trong UNIQUE ở SQL Server, nên các dòng TEAM với
    -- direct_key = NULL không bị chặn bởi constraint này).
    CONSTRAINT UQ_Conversations_direct_key UNIQUE (direct_key)
);
GO

-- Mỗi Team chỉ có tối đa 1 Team Conversation — filtered unique index,
-- cùng khuôn mẫu UQ_TeamMembers_team_owner ở db/19_teams.sql.
CREATE UNIQUE INDEX UQ_Conversations_team ON dbo.Conversations (team_id) WHERE type = 'TEAM';
GO

-- ============================================================
-- 2. Messages (soft-delete only — không hard delete)
-- ============================================================
CREATE TABLE dbo.Messages (
    message_id             INT             IDENTITY(1,1)   NOT NULL,
    conversation_id           INT             NOT NULL,
    sender_account_id           INT             NOT NULL,
    content                       NVARCHAR(2000)  NOT NULL,
    created_at                       DATETIME2(0)    NOT NULL CONSTRAINT DF_Messages_created_at DEFAULT (SYSUTCDATETIME()),
    edited_at                           DATETIME2(0)    NULL,
    deleted_at                             DATETIME2(0)    NULL,

    CONSTRAINT PK_Messages PRIMARY KEY (message_id),
    CONSTRAINT FK_Messages_Conversations FOREIGN KEY (conversation_id) REFERENCES dbo.Conversations (conversation_id),
    CONSTRAINT FK_Messages_Sender FOREIGN KEY (sender_account_id) REFERENCES dbo.Accounts (account_id)
);
GO

-- Phục vụ: liệt kê + phân trang tin nhắn của 1 conversation (mới nhất
-- trước, cùng convention với các list khác), và subquery "tin nhắn mới
-- nhất" cho Chat List.
CREATE INDEX IX_Messages_conversation_created ON dbo.Messages (conversation_id, created_at DESC);
GO

-- ============================================================
-- 3. ConversationMembers (junction table — hard delete cho phép khi
-- Leave/Remove, giống TeamMembers/LFGMembers)
-- ============================================================
CREATE TABLE dbo.ConversationMembers (
    conversation_member_id   INT             IDENTITY(1,1)   NOT NULL,
    conversation_id             INT             NOT NULL,
    account_id                     INT             NOT NULL,
    last_read_message_id             INT             NULL,
    joined_at                           DATETIME2(0)    NOT NULL CONSTRAINT DF_ConversationMembers_joined_at DEFAULT (SYSUTCDATETIME()),
    left_at                                 DATETIME2(0)    NULL,

    CONSTRAINT PK_ConversationMembers PRIMARY KEY (conversation_member_id),
    CONSTRAINT FK_ConversationMembers_Conversations FOREIGN KEY (conversation_id) REFERENCES dbo.Conversations (conversation_id),
    CONSTRAINT FK_ConversationMembers_Accounts FOREIGN KEY (account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT FK_ConversationMembers_LastReadMessage FOREIGN KEY (last_read_message_id) REFERENCES dbo.Messages (message_id),

    CONSTRAINT UQ_ConversationMembers_conv_account UNIQUE (conversation_id, account_id)
);
GO

CREATE INDEX IX_ConversationMembers_conversation_id ON dbo.ConversationMembers (conversation_id);
-- Phục vụ: "Chat của tôi" (Chat List, task spec §18/§19).
CREATE INDEX IX_ConversationMembers_account_id ON dbo.ConversationMembers (account_id);
GO

-- Bảng junction: cho phép DELETE thật khi Leave/Remove Member (đúng mục
-- 7.3 CLAUDE.md, cùng convention với LFGMembers/AccountGames/
-- AccountFollows/TeamMembers). GameNestSvcLogin bị DENY DELETE ở cấp
-- schema (06_gamenest_svc_login.sql) nên cần GRANT riêng.
GRANT DELETE ON dbo.ConversationMembers TO GameNestSvcUser;
GO

-- KHÔNG GRANT DELETE cho Conversations/Messages — Conversations không có
-- nghiệp vụ xóa (chỉ dùng status), Messages soft-delete only (task spec
-- §12/§16). Không ALTER Notifications — task này cố ý không thêm
-- NotificationType.MESSAGE_RECEIVED (task spec §25).

-- ============================================================
-- Quyết định thiết kế (ghi lại để không lặp lại phân tích ở lần đọc sau):
--
-- - Message content NVARCHAR(2000): không có convention độ dài tin nhắn
--   chat có sẵn trong project (Questions/Answers dùng NVARCHAR(MAX) vì là
--   nội dung dài); 2000 ký tự là lựa chọn hợp lý riêng cho tin nhắn chat
--   ngắn, khớp với ChatService.MESSAGE_MAX_LENGTH ở tầng Java.
--
-- - Conversations.status chỉ có 1 giá trị ACTIVE trong phase này — không
--   tạo DELETED/CLOSED/ARCHIVED vì task không định nghĩa rõ ý nghĩa các
--   giá trị đó. "Team Conversation không còn truy cập được khi Team bị
--   xóa" được kiểm tra bằng cách đọc lại Teams.status tại thời điểm truy
--   cập (ChatService), không phải bằng một cờ trạng thái thứ hai trên
--   Conversations có thể lệch với Teams.status theo thời gian.
--
-- - ConversationMembers.left_at có trong schema đúng theo yêu cầu task,
--   nhưng KHÔNG được ghi bởi bất kỳ code path nào trong phase này — Leave
--   Team và Remove Member đều hard-delete dòng ConversationMembers (phản
--   ánh đúng TeamMembers, vốn cũng hard-delete), không dùng soft-leave.
--   Cột này để sẵn cho một thiết kế soft-leave trong tương lai nếu cần,
--   không phải lỗi hay thiếu sót.
-- ============================================================
