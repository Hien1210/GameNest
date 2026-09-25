package com.gamenest.dao;

import com.gamenest.model.MessageAttachment;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MessageAttachments là bảng nội dung 1:1 với Messages (db/23_chat_attachment.sql),
 * không phải junction table — chỉ INSERT một lần lúc gửi tin nhắn, không
 * UPDATE/DELETE ở tầng ứng dụng (DENY UPDATE ở DB, DELETE đã bị chặn sẵn ở
 * cấp schema). {@link #insert} chạy trên Connection do caller truyền vào vì
 * luôn là một bước trong transaction gửi tin nhắn (Message + Attachment),
 * đúng khuôn mẫu {@code ConversationMemberDAO#insert(Connection, ...)}.
 * DAO này không phụ thuộc {@code ChatAttachmentUpload}/{@code InputStream} —
 * việc đọc/ghi file vật lý không thuộc tầng DAO.
 */
public class MessageAttachmentDAO {

    public void insert(Connection conn, int messageId, String mimeType, long sizeBytes, String storedFileName) throws SQLException {
        String sql = "INSERT INTO dbo.MessageAttachments (message_id, mime_type, size_bytes, stored_file_name) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setString(2, mimeType);
            ps.setLong(3, sizeBytes);
            ps.setString(4, storedFileName);
            ps.executeUpdate();
        }
    }

    public Optional<MessageAttachment> findByMessageId(int messageId) throws SQLException {
        String sql = "SELECT message_id, mime_type, size_bytes, stored_file_name, created_at "
                + "FROM dbo.MessageAttachments WHERE message_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    private MessageAttachment mapRow(ResultSet rs) throws SQLException {
        MessageAttachment attachment = new MessageAttachment();
        attachment.setMessageId(rs.getInt("message_id"));
        attachment.setMimeType(rs.getString("mime_type"));
        attachment.setSizeBytes(rs.getLong("size_bytes"));
        attachment.setStoredFileName(rs.getString("stored_file_name"));
        attachment.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return attachment;
    }
}
