-- ============================================================
-- GameNest — Chat Advanced Feature #3: Message Attachment (Image)
--
-- 1 bảng mới, additive-only, không ALTER bất kỳ migration nào trước đó
-- (không ALTER Messages — attachment KHÔNG lưu trên Messages, xem lý do
-- bên dưới).
--
-- THIẾT KẾ:
--
-- MessageAttachments là quan hệ 1:1 với Messages — mỗi Message có TỐI ĐA
-- 1 attachment, và attachment luôn gắn liền với đúng 1 Message tại thời
-- điểm tạo (không có tính năng thêm/xóa/thay ảnh sau khi gửi trong phạm
-- vi task này). Vì vậy dùng shared primary key: message_id vừa là PK vừa
-- là FK trực tiếp tới Messages(message_id) — không có cột định danh
-- riêng (message_attachment_id), tránh một bảng 1:1 có 2 khóa khác nhau
-- không cần thiết (CLAUDE.md mục 25 — không overengineer).
--
-- KHÔNG CASCADE, mặc định NO ACTION (giống FK_Messages_ReplyToMessage ở
-- db/21_chat_reply.sql): Messages không bao giờ hard-delete (CLAUDE.md
-- mục 7.1), nên cascade-on-delete là vô nghĩa ở tầng DB. File vật lý
-- tương ứng (nếu Message từng bị xóa) được dọn ở tầng ứng dụng, không
-- phải qua cascade SQL.
--
-- MessageAttachments KHÔNG phải junction/relationship table (khác
-- MessageReactions/ConversationMembers ở CLAUDE.md mục 7.3) — đây là nội
-- dung gắn liền với 1 Message cụ thể (giống bản thân Message), nên KHÔNG
-- cấp quyền DELETE cho tầng ứng dụng (đã bị DENY DELETE sẵn ở cấp schema,
-- xem db/06_gamenest_svc_login.sql, không cần GRANT ngoại lệ ở đây).
--
-- CHECK(mime_type IN (...)): allowlist cố định 3 định dạng ảnh, chặn ở
-- tầng DB để không phụ thuộc hoàn toàn vào validate phía Java (CLAUDE.md
-- mục 9) — cùng khuôn mẫu CK_MessageReactions_emoji ở db/22_chat_reaction.sql.
--
-- CHECK(size_bytes <= 5242880): giới hạn 5MB, khớp với
-- @MultipartConfig(maxFileSize = 5_242_880) ở tầng Servlet — chặn kép ở
-- cả 2 tầng, không chỉ tin vào giới hạn phía Java.
--
-- UNIQUE(stored_file_name): tên file lưu trên đĩa là UUID do server sinh
-- (không dùng tên file client gửi lên) — UNIQUE đảm bảo không có 2 dòng
-- MessageAttachments trỏ tới cùng 1 file vật lý.
--
-- Không thêm index ngoài PK/UNIQUE (index của PK và của UNIQUE constraint
-- được SQL Server tự tạo, không phải "index thừa" thêm ngoài yêu cầu):
-- tính năng này không có truy vấn "liệt kê attachment theo mime_type" hay
-- tương tự — tra cứu duy nhất là theo message_id, vốn đã là PK/clustered
-- index sẵn có (CLAUDE.md mục 25).
--
-- Không trigger, không stored procedure.
--
-- Grants: quyền mặc định ở cấp schema (db/06_gamenest_svc_login.sql) là
-- GRANT SELECT, INSERT, UPDATE, EXECUTE + DENY DELETE, ALTER. DELETE đã
-- bị chặn sẵn (đúng yêu cầu, không cần khai báo gì thêm). UPDATE thì
-- KHÔNG được yêu cầu cho bảng này (attachment ghi 1 lần lúc gửi tin
-- nhắn, không có luồng "sửa ảnh") — vì UPDATE đang được GRANT sẵn ở cấp
-- schema, phải DENY UPDATE riêng cho bảng này để thực sự giới hạn quyền
-- xuống chỉ còn SELECT + INSERT, đúng yêu cầu PHASE 1.
--
-- Chạy bằng tài khoản admin/DBA (sa) — giống các migration trước.
-- ============================================================

USE GameNestDB;
GO

CREATE TABLE dbo.MessageAttachments (
    message_id              INT             NOT NULL,
    mime_type                  VARCHAR(20)     NOT NULL,
    size_bytes                    INT             NOT NULL,
    stored_file_name                  VARCHAR(255)    NOT NULL,
    created_at                           DATETIME2(0)    NOT NULL CONSTRAINT DF_MessageAttachments_created_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_MessageAttachments PRIMARY KEY (message_id),
    CONSTRAINT FK_MessageAttachments_Messages FOREIGN KEY (message_id) REFERENCES dbo.Messages (message_id),

    -- Allowlist cố định — chặn ở tầng DB, không chỉ ở Service.
    CONSTRAINT CK_MessageAttachments_mime_type CHECK (mime_type IN ('image/jpeg', 'image/png', 'image/webp')),

    -- Khớp giới hạn 5MB ở tầng Servlet (@MultipartConfig).
    CONSTRAINT CK_MessageAttachments_size_bytes CHECK (size_bytes <= 5242880),

    -- Tên file vật lý do server sinh (UUID) — không trùng nhau.
    CONSTRAINT UQ_MessageAttachments_stored_file_name UNIQUE (stored_file_name)
);
GO

-- Bảng nội dung 1:1 với Message, không phải junction table (khác mục
-- 7.3 CLAUDE.md) — KHÔNG GRANT DELETE (giữ nguyên DENY DELETE mặc định
-- ở cấp schema). UPDATE đang được GRANT sẵn ở cấp schema nhưng bảng này
-- không có luồng sửa attachment sau khi tạo, nên DENY UPDATE riêng để
-- giới hạn đúng quyền tối thiểu (chỉ SELECT + INSERT) theo CLAUDE.md
-- mục 8/25.
DENY UPDATE ON dbo.MessageAttachments TO GameNestSvcUser;
GO
