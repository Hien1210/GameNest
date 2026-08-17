-- ============================================================
-- GameNest — User Friend (request/accept workflow, distinct from Follow)
--
-- Tạo bảng AccountFriendships — KHÔNG tạo Friends/FriendRequests/
-- FriendMembers riêng. Đây là state-machine table (PENDING/ACCEPTED/
-- REJECTED/CANCELLED/UNFRIENDED), KHÔNG phải junction table thuần như
-- LFGMembers/AccountGames/AccountFollows — không có hard delete, mọi thay
-- đổi quan hệ đều là UPDATE status (xem ghi chú "Re-friend" bên dưới).
--
-- Đồng thời mở rộng CK_Notifications_type (đã tạo ở db/14, mở rộng lần đầu
-- ở db/16) để hỗ trợ FRIEND_REQUEST/FRIEND_ACCEPTED — không tạo lại bảng
-- Notifications, không đổi target_type (ACCOUNT đã có sẵn).
--
-- RE-FRIEND SAU REJECTED/CANCELLED/UNFRIENDED: UNIQUE(requester_account_id,
-- receiver_account_id) là theo CHIỀU (ordered pair) — nếu A từng gửi request
-- cho B rồi bị REJECTED/CANCELLED, hoặc từng là bạn rồi UNFRIENDED, một
-- INSERT mới cho đúng chiều (A, B) sẽ vi phạm UNIQUE. Thiết kế được chốt:
-- Service TÁI SỬ DỤNG row cũ (UPDATE status trở lại PENDING, reset
-- responded_at = NULL, làm mới created_at) thay vì INSERT row mới — đúng
-- theo yêu cầu "không phá UNIQUE, không tạo row mới nếu có thể tái sử dụng
-- row cũ một cách an toàn". Hệ quả: lịch sử chi tiết của các lần
-- reject/cancel/unfriend TRƯỚC ĐÓ trên cùng một cặp (requester, receiver)
-- không được giữ lại từng dòng riêng — chỉ trạng thái mới nhất được lưu.
-- Đây là quyết định implementation, không phải thay đổi schema đã chốt,
-- nhưng được ghi rõ ở đây theo đúng yêu cầu "không tự ý giải quyết thiết kế
-- ngoài scope" của task.
--
-- Chạy bằng tài khoản admin/DBA (sa) — giống các migration trước.
-- ============================================================

USE GameNestDB;
GO

-- ============================================================
-- 1. AccountFriendships — request/accept workflow một-một giữa 2 Account
-- ============================================================
CREATE TABLE dbo.AccountFriendships (
    friendship_id            INT             IDENTITY(1,1)   NOT NULL,
    requester_account_id     INT             NOT NULL,
    receiver_account_id      INT             NOT NULL,
    status                    VARCHAR(20)     NOT NULL,
    created_at                 DATETIME2(0)    NOT NULL CONSTRAINT DF_AccountFriendships_created_at DEFAULT (SYSUTCDATETIME()),
    responded_at                DATETIME2(0)    NULL,

    CONSTRAINT PK_AccountFriendships PRIMARY KEY (friendship_id),
    CONSTRAINT FK_AccountFriendships_Requester FOREIGN KEY (requester_account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT FK_AccountFriendships_Receiver FOREIGN KEY (receiver_account_id) REFERENCES dbo.Accounts (account_id),

    -- Chặn duplicate cùng chiều ở tầng DB. Chiều ngược lại (B, A) là một cặp
    -- khác về mặt UNIQUE — Service chịu trách nhiệm chặn 2 PENDING đối
    -- xứng tồn tại đồng thời (xem AccountFriendService).
    CONSTRAINT UQ_AccountFriendships_pair UNIQUE (requester_account_id, receiver_account_id),

    -- A không thể tự kết bạn với chính mình — chặn ở tầng DB, ngoài check Service.
    CONSTRAINT CK_AccountFriendships_not_self CHECK (requester_account_id <> receiver_account_id),

    CONSTRAINT CK_AccountFriendships_status CHECK (status IN
        ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'UNFRIENDED'))
);
GO

-- Phục vụ: "Danh sách request tôi gửi" (requester_account_id + status).
CREATE INDEX IX_AccountFriendships_requester_status ON dbo.AccountFriendships (requester_account_id, status);
-- Phục vụ: "Danh sách request tôi nhận" (receiver_account_id + status).
CREATE INDEX IX_AccountFriendships_receiver_status ON dbo.AccountFriendships (receiver_account_id, status);
-- Ghi chú: "Danh sách Friends" (status=ACCEPTED, cả 2 chiều) được phục vụ
-- bởi CHÍNH 2 index trên (UNION theo requester/receiver) — không cần thêm
-- index thứ 3 riêng, tránh index thừa.
GO

-- KHÔNG GRANT DELETE — AccountFriendships không có hard-delete trong luồng
-- nghiệp vụ (chỉ chuyển status), đúng task spec §20. SELECT/INSERT/UPDATE
-- đã có sẵn từ GRANT cấp schema trong 06_gamenest_svc_login.sql
-- ("GRANT SELECT, INSERT, UPDATE, EXECUTE ON SCHEMA::dbo") — áp dụng tự
-- động cho bảng mới này, không cần GRANT riêng.

-- ============================================================
-- 2. Notifications — mở rộng CK_Notifications_type thêm FRIEND_REQUEST,
-- FRIEND_ACCEPTED. target_type dùng lại ACCOUNT (đã có sẵn) — không đổi
-- CK_Notifications_target_type.
-- ============================================================
ALTER TABLE dbo.Notifications DROP CONSTRAINT CK_Notifications_type;
GO

ALTER TABLE dbo.Notifications ADD CONSTRAINT CK_Notifications_type CHECK (type IN
    ('ANSWER_ACCEPTED', 'ANSWER_REPLY', 'REPORT_RESOLVED', 'REPORT_REJECTED', 'SYSTEM', 'FOLLOW',
     'FRIEND_REQUEST', 'FRIEND_ACCEPTED'));
GO
