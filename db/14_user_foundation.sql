-- ============================================================
-- GameNest — User / Community Database Foundation
-- Migration additive: KHÔNG ALTER Accounts/Games/Questions/Answers/Reports/
-- AuditLogs/SystemSettings. Chỉ tạo 2 bảng mới có lý do cụ thể, đã phân
-- tích trong Database Foundation report đi kèm task này.
--
-- LƯU Ý: LFGPosts/LFGMembers ĐÃ TỒN TẠI SẴN từ 01_core_schema.sql (creator,
-- game, capacity, status, junction table hard-delete hợp lệ) — đó đã là
-- LFG Foundation, không tạo lại/tạo trùng ở đây.
--
-- Đây là DATABASE-ONLY: không có Java model/DAO/Service nào đi kèm migration
-- này, giống đúng cách LFGPosts/LFGMembers được tạo trước đây (schema đứng
-- trước, business logic xây sau khi có yêu cầu cụ thể).
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestSvcLogin/AppLogin)
-- ============================================================

USE GameNestDB;
GO

-- ============================================================
-- 1. AccountGames — quan hệ Account ↔ Game (yêu thích / đang chơi)
-- Junction table thuần (giống LFGMembers): không có status/soft-delete,
-- hard-delete hợp lệ theo CLAUDE.md mục 7.3 (bỏ yêu thích/ngừng chơi = xóa
-- dòng quan hệ, không phải "xóa nội dung lịch sử").
-- ============================================================
CREATE TABLE dbo.AccountGames (
    account_game_id     INT             IDENTITY(1,1)   NOT NULL,
    account_id           INT             NOT NULL,
    game_id               INT             NOT NULL,
    relationship_type     VARCHAR(20)     NOT NULL,
    created_at             DATETIME2(0)    NOT NULL CONSTRAINT DF_AccountGames_created_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_AccountGames PRIMARY KEY (account_game_id),
    CONSTRAINT FK_AccountGames_Accounts FOREIGN KEY (account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT FK_AccountGames_Games FOREIGN KEY (game_id) REFERENCES dbo.Games (game_id),

    -- Cho phép 1 account có cả 2 dòng FAVORITE và PLAYING cho cùng 1 game
    -- (2 thuộc tính độc lập), nhưng không trùng lặp cùng 1 loại.
    CONSTRAINT UQ_AccountGames_account_game_type UNIQUE (account_id, game_id, relationship_type),

    -- Chỉ 2 giá trị được nêu cụ thể trong yêu cầu ("yêu thích" / "đang
    -- chơi"). Mở rộng thêm loại (vd WISHLIST) sau này bằng migration
    -- additive khác, giống cách CK_AuditLogs_module đã mở rộng nhiều lần.
    CONSTRAINT CK_AccountGames_relationship_type CHECK (relationship_type IN ('FAVORITE', 'PLAYING'))
);
GO

-- Phục vụ: hiển thị danh sách game trên Profile của 1 account.
CREATE INDEX IX_AccountGames_account_id ON dbo.AccountGames (account_id);
-- Phục vụ: "tìm người chơi game X" / LFG matching theo game (nêu rõ trong
-- bối cảnh USER của task).
CREATE INDEX IX_AccountGames_game_id ON dbo.AccountGames (game_id);
GO

-- ============================================================
-- 2. Notifications — foundation cho thông báo, KHÔNG kèm WebSocket/realtime.
-- Dùng lại đúng pattern polymorphic target_type/target_id đã có ở
-- Reports/AuditLogs — không tạo cơ chế reference mới.
-- ============================================================
CREATE TABLE dbo.Notifications (
    notification_id        INT             IDENTITY(1,1)   NOT NULL,
    recipient_account_id    INT             NOT NULL,
    type                     VARCHAR(30)     NOT NULL,
    title                     NVARCHAR(200)   NOT NULL,
    message                   NVARCHAR(500)   NULL,

    -- Polymorphic reference, giống Reports.target_type/target_id — không
    -- tạo FK SQL trực tiếp tới nhiều bảng.
    target_id                 INT             NULL,
    target_type               VARCHAR(30)     NULL,

    is_read                    BIT             NOT NULL CONSTRAINT DF_Notifications_is_read DEFAULT (0),
    created_at                 DATETIME2(0)    NOT NULL CONSTRAINT DF_Notifications_created_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_Notifications PRIMARY KEY (notification_id),
    CONSTRAINT FK_Notifications_Recipient FOREIGN KEY (recipient_account_id) REFERENCES dbo.Accounts (account_id),

    -- Chỉ liệt kê các type ánh xạ tới sự kiện nghiệp vụ ĐÃ TỒN TẠI thật
    -- trong code hiện tại (AnswerService.acceptAnswer, AnswerService.createAnswer,
    -- ReportService.resolveReport/rejectReport). Các type liên quan
    -- Follow/Friend/Team/LFG chưa được thêm vì các module đó chưa tồn tại
    -- — sẽ mở rộng bằng migration additive khi module tương ứng được xây.
    CONSTRAINT CK_Notifications_type CHECK (type IN
        ('ANSWER_ACCEPTED', 'ANSWER_REPLY', 'REPORT_RESOLVED', 'REPORT_REJECTED', 'SYSTEM')),

    CONSTRAINT CK_Notifications_target_type CHECK (
        target_type IS NULL OR target_type IN ('ACCOUNT', 'QUESTION', 'ANSWER', 'REPORT')
    )
);
GO

-- Phục vụ: "danh sách thông báo chưa đọc của tôi" — truy vấn phổ biến nhất
-- của mọi UI notification (bell/dropdown).
CREATE INDEX IX_Notifications_recipient_unread ON dbo.Notifications (recipient_account_id, is_read);
-- Phục vụ: sắp xếp thông báo mới nhất trước.
CREATE INDEX IX_Notifications_created_at ON dbo.Notifications (created_at DESC);
GO

-- Không GRANT DELETE cho AccountGames/Notifications ngoài chính sách mặc
-- định: GameNestSvcLogin đã bị DENY DELETE ở cấp schema (06_gamenest_svc_login.sql).
-- AccountGames là junction table (giống LFGMembers) NÊN ĐƯỢC PHÉP xóa thật —
-- nếu áp dụng đúng CLAUDE.md mục 7.3, cần GRANT DELETE riêng khi có nghiệp vụ
-- "Bỏ yêu thích / Ngừng chơi" thực sự triển khai (không GRANT trước ở đây vì
-- task này không xây business logic — xem Database Foundation report).
