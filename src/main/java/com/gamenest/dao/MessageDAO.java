package com.gamenest.dao;

import com.gamenest.model.Message;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Messages is user-generated content — NEVER hard-deleted (db/20_chat.sql,
 * CLAUDE.md §7.1). {@link #softDeleteIfSender} only ever sets deleted_at;
 * there is no DELETE FROM Messages anywhere in this DAO.
 * <p>
 * Reply feature (db/21_chat_reply.sql): reply_to_message_id is a nullable
 * self-referencing FK. The reply target row is resolved via a LEFT JOIN
 * back onto Messages/Accounts so every read already carries enough of the
 * target (sender label, content, deleted_at) to render a Reply Preview
 * without an extra query — deliberately not a separate Replies table.
 */
public class MessageDAO {

    private static final String SELECT_COLUMNS =
            "m.message_id, m.conversation_id, m.sender_account_id, m.content, m.created_at, m.edited_at, m.deleted_at, m.reply_to_message_id, "
                    + "a.username AS sender_username, a.display_name AS sender_display_name, a.avatar_url AS sender_avatar_url, "
                    + "rm.sender_account_id AS reply_sender_account_id, ra.username AS reply_sender_username, "
                    + "ra.display_name AS reply_sender_display_name, rm.content AS reply_content, rm.deleted_at AS reply_deleted_at, "
                    + "ma.mime_type AS attachment_mime_type, ma.size_bytes AS attachment_size_bytes ";
    private static final String BASE_SELECT =
            "SELECT " + SELECT_COLUMNS + "FROM dbo.Messages m "
                    + "JOIN dbo.Accounts a ON a.account_id = m.sender_account_id "
                    + "LEFT JOIN dbo.Messages rm ON rm.message_id = m.reply_to_message_id "
                    + "LEFT JOIN dbo.Accounts ra ON ra.account_id = rm.sender_account_id "
                    + "LEFT JOIN dbo.MessageAttachments ma ON ma.message_id = m.message_id ";

    /** {@code replyToMessageId} null = tin nhắn thường (Reply feature, task spec §7). */
    public Message insert(int conversationId, int senderAccountId, String content, Integer replyToMessageId) throws SQLException {
        String sql = "INSERT INTO dbo.Messages (conversation_id, sender_account_id, content, reply_to_message_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, senderAccountId);
            ps.setString(3, content);
            if (replyToMessageId != null) {
                ps.setInt(4, replyToMessageId);
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.executeUpdate();

            Message message = new Message();
            message.setConversationId(conversationId);
            message.setSenderAccountId(senderAccountId);
            message.setContent(content);
            message.setReplyToMessageId(replyToMessageId);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    message.setMessageId(keys.getInt(1));
                }
            }
            return message;
        }
    }

    /**
     * Cùng SQL/logic dựng {@link Message} như {@link #insert(int, int, String, Integer)},
     * chỉ khác là chạy trên {@code Connection} do caller truyền vào — dùng
     * khi gửi tin nhắn kèm attachment cần chung 1 transaction với
     * {@code MessageAttachmentDAO#insert}, đúng khuôn mẫu
     * {@code ConversationMemberDAO#insert(Connection, ...)}.
     */
    public Message insert(Connection conn, int conversationId, int senderAccountId, String content, Integer replyToMessageId) throws SQLException {
        String sql = "INSERT INTO dbo.Messages (conversation_id, sender_account_id, content, reply_to_message_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, senderAccountId);
            ps.setString(3, content);
            if (replyToMessageId != null) {
                ps.setInt(4, replyToMessageId);
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.executeUpdate();

            Message message = new Message();
            message.setConversationId(conversationId);
            message.setSenderAccountId(senderAccountId);
            message.setContent(content);
            message.setReplyToMessageId(replyToMessageId);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    message.setMessageId(keys.getInt(1));
                }
            }
            return message;
        }
    }

    public Optional<Message> findById(int messageId) throws SQLException {
        String sql = BASE_SELECT + "WHERE m.message_id = ?";
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

    /** Guarded: only the sender may edit, and only while not yet deleted (task spec §15). */
    public int editIfSender(int messageId, int senderAccountId, String newContent) throws SQLException {
        String sql = "UPDATE dbo.Messages SET content = ?, edited_at = SYSUTCDATETIME() "
                + "WHERE message_id = ? AND sender_account_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newContent);
            ps.setInt(2, messageId);
            ps.setInt(3, senderAccountId);
            return ps.executeUpdate();
        }
    }

    /** Guarded: only the sender may delete, and only once (task spec §16) — soft delete only, never a hard DELETE. */
    public int softDeleteIfSender(int messageId, int senderAccountId) throws SQLException {
        String sql = "UPDATE dbo.Messages SET deleted_at = SYSUTCDATETIME() "
                + "WHERE message_id = ? AND sender_account_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, senderAccountId);
            return ps.executeUpdate();
        }
    }

    /** Newest first (matches every other paginated list in this project), DB-side paginated. Deleted messages are still returned (content intact) — the JSP substitutes "Tin nhắn đã được xóa." for display. */
    public List<Message> listByConversation(int conversationId, int offset, int limit) throws SQLException {
        String sql = BASE_SELECT + "WHERE m.conversation_id = ? "
                + "ORDER BY m.created_at DESC, m.message_id DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            List<Message> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        }
    }

    public int countByConversation(int conversationId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Messages WHERE conversation_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Search Message (APPROVED design): content search within one
     * conversation, newest first (same order as {@link #listByConversation}).
     * Excludes soft-deleted messages ({@code deleted_at IS NOT NULL} must
     * never appear in results) — the one place this DAO deliberately diverges
     * from {@link #listByConversation}, which keeps deleted rows for
     * placeholder rendering. Reuses {@link #BASE_SELECT} so a matched
     * message's Reply preview (if any) comes back for free, same shape as
     * every other Message read here. {@code keyword} is escaped by
     * {@link #likePattern} and always bound as a parameter (CLAUDE.md §19) —
     * never concatenated into SQL.
     */
    public List<Message> searchByConversation(int conversationId, String keyword, int limit) throws SQLException {
        String sql = BASE_SELECT + "WHERE m.conversation_id = ? AND m.deleted_at IS NULL "
                + "AND m.content LIKE ? ESCAPE '\\' "
                + "ORDER BY m.created_at DESC, m.message_id DESC "
                + "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setString(2, likePattern(keyword));
            ps.setInt(3, limit);
            List<Message> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        }
    }

    /**
     * 1-based position of {@code messageId} within its conversation's full
     * message history, ordered newest-first — the exact same
     * {@code created_at DESC, message_id DESC} order {@link #listByConversation}
     * paginates over, including soft-deleted messages (they still occupy a
     * slot on a page). Used by {@code ChatService#searchMessages} to compute
     * which pagination page a search result falls on, for the
     * click-to-navigate redirect (APPROVED design, Phương án B).
     */
    public int countPosition(int conversationId, LocalDateTime createdAt, int messageId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Messages WHERE conversation_id = ? "
                + "AND (created_at > ? OR (created_at = ? AND message_id >= ?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setObject(2, createdAt);
            ps.setObject(3, createdAt);
            ps.setInt(4, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Same convention as {@code QuestionDAO#likePattern} — escapes LIKE metacharacters before wrapping in wildcards; always bound via PreparedStatement + {@code ESCAPE '\\'}. */
    private String likePattern(String query) {
        String escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setMessageId(rs.getInt("message_id"));
        message.setConversationId(rs.getInt("conversation_id"));
        message.setSenderAccountId(rs.getInt("sender_account_id"));
        message.setContent(rs.getString("content"));
        message.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        message.setEditedAt(rs.getObject("edited_at", LocalDateTime.class));
        message.setDeletedAt(rs.getObject("deleted_at", LocalDateTime.class));
        message.setSenderUsername(rs.getString("sender_username"));
        message.setSenderDisplayName(rs.getString("sender_display_name"));
        message.setSenderAvatarUrl(rs.getString("sender_avatar_url"));

        message.setReplyToMessageId(rs.getObject("reply_to_message_id", Integer.class));
        message.setReplyToSenderAccountId(rs.getObject("reply_sender_account_id", Integer.class));
        message.setReplyToSenderUsername(rs.getString("reply_sender_username"));
        message.setReplyToSenderDisplayName(rs.getString("reply_sender_display_name"));
        message.setReplyToContent(rs.getString("reply_content"));
        message.setReplyToDeletedAt(rs.getObject("reply_deleted_at", LocalDateTime.class));

        message.setAttachmentMimeType(rs.getString("attachment_mime_type"));
        long attachmentSizeBytes = rs.getLong("attachment_size_bytes");
        message.setAttachmentSizeBytes(rs.wasNull() ? null : attachmentSizeBytes);
        return message;
    }
}
