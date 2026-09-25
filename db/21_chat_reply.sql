-- ============================================================
-- GameNest — Chat Advanced Feature #1: Reply Message
--
-- Additive-only ALTER trên bảng Messages đã có (db/20_chat.sql) — KHÔNG
-- sửa file migration cũ. Reply là một thuộc tính của Message (Reply as a
-- property of Message), không phải một hệ thống Chat song song: không tạo
-- bảng Replies/MessageReplies/ConversationReplies riêng.
--
-- THIẾT KẾ:
--
-- - reply_to_message_id NULL: NULL = tin nhắn thường, NOT NULL = tin nhắn
--   trả lời tin nhắn có message_id tương ứng. Self-referencing FK tới
--   chính Messages(message_id).
--
-- - Không ON DELETE CASCADE: Messages không bao giờ hard-delete (CLAUDE.md
--   mục 7.1, db/20_chat.sql), nên cascade-on-delete là vô nghĩa; mặc định
--   NO ACTION cũng là lựa chọn bắt buộc của SQL Server cho self-referencing
--   FK để tránh lỗi cascade-cycle.
--
-- - Xóa (soft-delete) tin nhắn được trả lời KHÔNG xóa quan hệ reply: tin
--   nhắn trả lời vẫn giữ nguyên reply_to_message_id, tầng ứng dụng hiển thị
--   placeholder "Tin nhắn đã được xóa" dựa trên deleted_at của tin nhắn gốc
--   (đọc lại qua LEFT JOIN ở MessageDAO, không cần cột cờ thứ hai).
--
-- - Không thêm index mới: tính năng này không có truy vấn "liệt kê tất cả
--   reply của message X" (không có Thread Message trong phạm vi), chỉ bao
--   giờ resolve reply target bằng chính PK message_id (đã có clustered
--   index sẵn) — thêm index không dùng tới là overengineering không cần
--   thiết (CLAUDE.md mục 25).
--
-- - Không đổi GRANT: vẫn KHÔNG GRANT DELETE cho Messages/Conversations,
--   GRANT DELETE trên ConversationMembers giữ nguyên từ db/20_chat.sql.
--
-- Chạy bằng tài khoản admin/DBA (sa) — giống các migration trước.
-- ============================================================

USE GameNestDB;
GO

ALTER TABLE dbo.Messages ADD reply_to_message_id INT NULL;
GO

ALTER TABLE dbo.Messages
    ADD CONSTRAINT FK_Messages_ReplyToMessage
    FOREIGN KEY (reply_to_message_id) REFERENCES dbo.Messages (message_id);
GO
