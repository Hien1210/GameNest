-- ============================================================
-- GameNest — Team / Private Friend Room
--
-- 3 bảng mới, additive-only, không ALTER bảng cũ (ngoại trừ mở rộng
-- CK_Notifications_type ở cuối file, đúng khuôn mẫu additive đã dùng từ
-- migration 07 trở đi).
--
-- Team KHÁC LFG: LFGPosts/LFGMembers là "tìm người chơi công khai", còn
-- Team là "phòng riêng cho Friends" — Owner chỉ được mời người đã là
-- Friend (kiểm tra ở AccountFriendService, không lặp lại logic ở đây) và
-- không được mời/bị mời nếu đang Block nhau (kiểm tra ở AccountBlockService).
--
-- THIẾT KẾ (xem thêm phần "Quyết định thiết kế" cuối file):
--
-- 1. Teams — entity nghiệp vụ chính, giống Games/Questions/LFGPosts về
--    convention (NVARCHAR(150) name, NVARCHAR(MAX) description nullable,
--    VARCHAR(20) status). SOFT DELETE (status ACTIVE/DELETED) — Team là
--    "user-generated community content" giống Questions/LFGPosts, CLAUDE.md
--    mục 7.1 cấm hard-delete loại nội dung này. owner_account_id trên
--    Teams là con trỏ "current owner" (không phải chỉ "creator") — được
--    UPDATE cùng transaction với TeamMembers khi Transfer Ownership, để
--    Service có thể kiểm tra quyền Owner bằng 1 SELECT không cần JOIN.
--
-- 2. TeamMembers — pure membership junction table (giống LFGMembers) —
--    role chỉ 2 giá trị OWNER/MEMBER. Leave/Remove Member = hard DELETE
--    (CLAUDE.md mục 7.3). UNIQUE INDEX lọc theo role='OWNER' đảm bảo
--    KHÔNG BAO GIỜ có 2 dòng OWNER cho cùng 1 team ở tầng database — vế
--    "0 OWNER" của bất biến này được đảm bảo ở tầng Service (Owner không
--    thể Leave, phải Transfer/Delete trước).
--
-- 3. TeamInvitations — state machine (giống AccountFriendships), KHÔNG
--    hard-delete, mọi transition là UPDATE status có guard. UNIQUE
--    (team_id, invitee_account_id) — không phải (inviter, invitee) — vì
--    Owner có thể đổi qua Transfer Ownership nhưng vẫn chỉ nên có 1 lời
--    mời "đang có ý nghĩa" cho 1 người trong 1 team tại một thời điểm.
--    Mời lại sau REJECTED/CANCELLED tái sử dụng row cũ (giống re-friend ở
--    db/17_account_friendships.sql), cập nhật lại inviter_account_id vì
--    người mời lại có thể là Owner mới.
--
-- Chạy bằng tài khoản admin/DBA (sa) — giống các migration trước.
-- ============================================================

USE GameNestDB;
GO

-- ============================================================
-- 1. Teams
-- ============================================================
CREATE TABLE dbo.Teams (
    team_id             INT             IDENTITY(1,1)   NOT NULL,
    owner_account_id     INT             NOT NULL,
    name                  NVARCHAR(150)   NOT NULL,
    description            NVARCHAR(MAX)   NULL,
    status                  VARCHAR(20)     NOT NULL CONSTRAINT DF_Teams_status DEFAULT ('ACTIVE'),
    created_at               DATETIME2(0)    NOT NULL CONSTRAINT DF_Teams_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at                DATETIME2(0)    NULL,

    CONSTRAINT PK_Teams PRIMARY KEY (team_id),
    CONSTRAINT FK_Teams_Owner FOREIGN KEY (owner_account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT CK_Teams_status CHECK (status IN ('ACTIVE', 'DELETED'))
);
GO

-- Phục vụ: kiểm tra quyền Owner nhanh, thống kê Team theo chủ sở hữu.
CREATE INDEX IX_Teams_owner_account_id ON dbo.Teams (owner_account_id);
GO

-- ============================================================
-- 2. TeamMembers (junction table — hard delete cho phép khi Leave/Remove)
-- ============================================================
CREATE TABLE dbo.TeamMembers (
    team_member_id       INT             IDENTITY(1,1)   NOT NULL,
    team_id               INT             NOT NULL,
    account_id             INT             NOT NULL,
    role                    VARCHAR(20)     NOT NULL,
    joined_at                DATETIME2(0)    NOT NULL CONSTRAINT DF_TeamMembers_joined_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_TeamMembers PRIMARY KEY (team_member_id),
    CONSTRAINT FK_TeamMembers_Teams FOREIGN KEY (team_id) REFERENCES dbo.Teams (team_id),
    CONSTRAINT FK_TeamMembers_Accounts FOREIGN KEY (account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT UQ_TeamMembers_team_account UNIQUE (team_id, account_id),
    CONSTRAINT CK_TeamMembers_role CHECK (role IN ('OWNER', 'MEMBER'))
);
GO

CREATE INDEX IX_TeamMembers_team_id ON dbo.TeamMembers (team_id);
CREATE INDEX IX_TeamMembers_account_id ON dbo.TeamMembers (account_id);
-- Bất biến "không bao giờ 2 OWNER cho cùng 1 team", enforce ở tầng DB.
CREATE UNIQUE INDEX UQ_TeamMembers_team_owner ON dbo.TeamMembers (team_id) WHERE role = 'OWNER';
GO

-- Bảng junction: cho phép DELETE thật khi Leave/Remove Member (đúng mục
-- 7.3 CLAUDE.md, cùng convention với LFGMembers/AccountGames/AccountFollows).
GRANT DELETE ON dbo.TeamMembers TO GameNestSvcUser;
GO

-- ============================================================
-- 3. TeamInvitations (state machine — không hard-delete)
-- ============================================================
CREATE TABLE dbo.TeamInvitations (
    invitation_id            INT             IDENTITY(1,1)   NOT NULL,
    team_id                    INT             NOT NULL,
    inviter_account_id          INT             NOT NULL,
    invitee_account_id            INT             NOT NULL,
    status                          VARCHAR(20)     NOT NULL CONSTRAINT DF_TeamInvitations_status DEFAULT ('PENDING'),
    created_at                       DATETIME2(0)    NOT NULL CONSTRAINT DF_TeamInvitations_created_at DEFAULT (SYSUTCDATETIME()),
    responded_at                      DATETIME2(0)    NULL,

    CONSTRAINT PK_TeamInvitations PRIMARY KEY (invitation_id),
    CONSTRAINT FK_TeamInvitations_Teams FOREIGN KEY (team_id) REFERENCES dbo.Teams (team_id),
    CONSTRAINT FK_TeamInvitations_Inviter FOREIGN KEY (inviter_account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT FK_TeamInvitations_Invitee FOREIGN KEY (invitee_account_id) REFERENCES dbo.Accounts (account_id),

    -- Chặn duplicate PENDING cho cùng 1 (team, invitee) — không theo
    -- (inviter, invitee) vì inviter có thể đổi qua Transfer Ownership.
    CONSTRAINT UQ_TeamInvitations_team_invitee UNIQUE (team_id, invitee_account_id),

    CONSTRAINT CK_TeamInvitations_not_self CHECK (inviter_account_id <> invitee_account_id),
    CONSTRAINT CK_TeamInvitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED'))
);
GO

-- Phục vụ: "lời mời tôi nhận" (Team Invitation List, task spec §18).
CREATE INDEX IX_TeamInvitations_invitee_status ON dbo.TeamInvitations (invitee_account_id, status);
-- Phục vụ: "lời mời đang chờ của team này" (Owner xem/Cancel, task spec §12).
CREATE INDEX IX_TeamInvitations_team_status ON dbo.TeamInvitations (team_id, status);
GO

-- KHÔNG GRANT DELETE — TeamInvitations là state machine giống
-- AccountFriendships, mọi transition là UPDATE, không có DELETE trong
-- business flow.

-- ============================================================
-- 4. Notifications — mở rộng CK_Notifications_type thêm TEAM_INVITE.
-- target_type dùng lại ACCOUNT (target_id = inviter_account_id) — KHÔNG
-- thêm giá trị TEAM vào CK_Notifications_target_type chỉ để "cho đẹp"
-- (task spec §20); xem quyết định đầy đủ trong TeamService/NotificationService.
-- ============================================================
ALTER TABLE dbo.Notifications DROP CONSTRAINT CK_Notifications_type;
GO

ALTER TABLE dbo.Notifications ADD CONSTRAINT CK_Notifications_type CHECK (type IN
    ('ANSWER_ACCEPTED', 'ANSWER_REPLY', 'REPORT_RESOLVED', 'REPORT_REJECTED', 'SYSTEM', 'FOLLOW',
     'FRIEND_REQUEST', 'FRIEND_ACCEPTED', 'TEAM_INVITE'));
GO

-- ============================================================
-- Quyết định thiết kế (ghi lại để không lặp lại phân tích ở lần đọc sau):
--
-- - owner_account_id trên Teams là "current owner", KHÔNG chỉ "creator".
--   Transfer Ownership UPDATE cả Teams.owner_account_id VÀ TeamMembers.role
--   trong cùng 1 transaction — 2 nguồn dữ liệu này redundant có chủ đích
--   (Teams.owner_account_id cho truy vấn quyền nhanh, TeamMembers.role cho
--   hiển thị danh sách thành viên), luôn đồng bộ vì chỉ có đúng 1 code path
--   (TeamService.transferOwnership) được phép sửa cả hai.
--
-- - "Không bao giờ 2 OWNER" được DB enforce (UQ_TeamMembers_team_owner).
--   "Không bao giờ 0 OWNER" được Service enforce (Owner không thể Leave;
--   Delete Team không xóa TeamMembers, chỉ soft-delete Teams — xem
--   TeamService.deleteTeam).
-- ============================================================
