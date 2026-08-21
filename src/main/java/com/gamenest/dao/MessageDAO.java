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
                    + "ra.display_name AS reply_sender_display_name, rm.content AS reply_content, rm.deleted_at AS reply_deleted_at ";
    private static final String BASE_SELECT =
            "SELECT " + SELECT_COLUMNS + "FROM dbo.Messages m "
                    + "JOIN dbo.Accounts a ON a.account_id = m.sender_account_id "
                    + "LEFT JOIN dbo.Messages rm ON rm.message_id = m.reply_to_message_id "
                    + "LEFT JOIN dbo.Accounts ra ON ra.account_id = rm.sender_account_id ";

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
        return message;
    }
}
