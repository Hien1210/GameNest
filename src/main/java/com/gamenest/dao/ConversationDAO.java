package com.gamenest.dao;

import com.gamenest.exception.DuplicateConversationException;
import com.gamenest.model.Conversation;
import com.gamenest.model.ConversationStatus;
import com.gamenest.model.ConversationType;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Conversations never has a hard DELETE — it either doesn't exist yet or
 * exists forever (db/20_chat.sql). {@link #insertDirect}/{@link #insertTeam}
 * both run on a caller-supplied Connection because they are always the
 * first step of a larger transaction (Direct: + 2 ConversationMembers;
 * Team: + TeamMembers/ConversationMembers, in TeamService).
 */
public class ConversationDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

    private static final String SELECT_COLUMNS =
            "conversation_id, type, team_id, direct_key, status, created_at, updated_at ";
    private static final String BASE_SELECT = "SELECT " + SELECT_COLUMNS + "FROM dbo.Conversations ";

    /** Only used when no row exists yet for this exact direct_key — the UNIQUE constraint is the real race guard, this just translates the violation. */
    public Conversation insertDirect(Connection conn, String directKey) throws SQLException, DuplicateConversationException {
        String sql = "INSERT INTO dbo.Conversations (type, direct_key, status) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ConversationType.DIRECT);
            ps.setString(2, directKey);
            ps.setString(3, ConversationStatus.ACTIVE);
            ps.executeUpdate();

            Conversation conversation = new Conversation();
            conversation.setType(ConversationType.DIRECT);
            conversation.setDirectKey(directKey);
            conversation.setStatus(ConversationStatus.ACTIVE);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    conversation.setConversationId(keys.getInt(1));
                }
            }
            return conversation;

        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                throw new DuplicateConversationException("Cuộc trò chuyện đã tồn tại giữa hai tài khoản này.");
            }
            throw e;
        }
    }

    /** Only used when no row exists yet for this exact team_id — guarded by UQ_Conversations_team (filtered unique index). */
    public Conversation insertTeam(Connection conn, int teamId) throws SQLException, DuplicateConversationException {
        String sql = "INSERT INTO dbo.Conversations (type, team_id, status) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ConversationType.TEAM);
            ps.setInt(2, teamId);
            ps.setString(3, ConversationStatus.ACTIVE);
            ps.executeUpdate();

            Conversation conversation = new Conversation();
            conversation.setType(ConversationType.TEAM);
            conversation.setTeamId(teamId);
            conversation.setStatus(ConversationStatus.ACTIVE);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    conversation.setConversationId(keys.getInt(1));
                }
            }
            return conversation;

        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                throw new DuplicateConversationException("Nhóm này đã có cuộc trò chuyện.");
            }
            throw e;
        }
    }

    public Optional<Conversation> findById(int conversationId) throws SQLException {
        String sql = BASE_SELECT + "WHERE conversation_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            return findOne(ps);
        }
    }

    public Optional<Conversation> findByDirectKey(String directKey) throws SQLException {
        String sql = BASE_SELECT + "WHERE direct_key = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, directKey);
            return findOne(ps);
        }
    }

    public Optional<Conversation> findByTeamId(int teamId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByTeamId(conn, teamId);
        }
    }

    /** Same as {@link #findByTeamId(int)} but reuses a caller-supplied Connection — used inside TeamService's Accept/Leave/Remove/self-heal transactions. */
    public Optional<Conversation> findByTeamId(Connection conn, int teamId) throws SQLException {
        String sql = BASE_SELECT + "WHERE team_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            return findOne(ps);
        }
    }

    /**
     * Conversations accountId is currently a member of — newest activity
     * first (latest message time, or conversation creation time if no
     * message yet), DB-side. Excludes TEAM conversations whose Team is no
     * longer ACTIVE (that Team Chat is inaccessible — see ChatService), but
     * never excludes DIRECT conversations regardless of current Friend/
     * Block status (history stays visible, task spec §8/§9).
     */
    public List<Conversation> listMyConversations(int accountId, int offset, int limit) throws SQLException {
        String sql = "SELECT c.conversation_id, c.type, c.team_id, c.direct_key, c.status, c.created_at, c.updated_at, "
                + "t.name AS team_name, "
                + "op.username AS other_username, op.display_name AS other_display_name, op.avatar_url AS other_avatar_url, "
                + "lm.content AS latest_message_content, lm.created_at AS latest_message_created_at, "
                + "lm.sender_account_id AS latest_message_sender_id, lm.deleted_at AS latest_message_deleted_at, "
                + "lm.attachment_message_id AS latest_message_attachment_id, "
                + "(SELECT COUNT(*) FROM dbo.Messages msg WHERE msg.conversation_id = c.conversation_id "
                + "  AND msg.deleted_at IS NULL AND msg.message_id > ISNULL(cm.last_read_message_id, 0)) AS unread_count "
                + "FROM dbo.ConversationMembers cm "
                + "JOIN dbo.Conversations c ON c.conversation_id = cm.conversation_id "
                + "LEFT JOIN dbo.Teams t ON t.team_id = c.team_id "
                + "OUTER APPLY ("
                + "  SELECT TOP 1 a.username, a.display_name, a.avatar_url "
                + "  FROM dbo.ConversationMembers cm2 JOIN dbo.Accounts a ON a.account_id = cm2.account_id "
                + "  WHERE cm2.conversation_id = c.conversation_id AND cm2.account_id <> ?"
                + ") op "
                + "OUTER APPLY ("
                + "  SELECT TOP 1 m.content, m.created_at, m.sender_account_id, m.deleted_at, ma.message_id AS attachment_message_id "
                + "  FROM dbo.Messages m LEFT JOIN dbo.MessageAttachments ma ON ma.message_id = m.message_id "
                + "  WHERE m.conversation_id = c.conversation_id "
                + "  ORDER BY m.created_at DESC, m.message_id DESC"
                + ") lm "
                + "WHERE cm.account_id = ? AND (c.type = 'DIRECT' OR t.status = 'ACTIVE') "
                + "ORDER BY COALESCE(lm.created_at, c.created_at) DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, accountId);
            ps.setInt(3, offset);
            ps.setInt(4, limit);
            List<Conversation> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapListRow(rs));
                }
            }
            return results;
        }
    }

    public int countMyConversations(int accountId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.ConversationMembers cm "
                + "JOIN dbo.Conversations c ON c.conversation_id = cm.conversation_id "
                + "LEFT JOIN dbo.Teams t ON t.team_id = c.team_id "
                + "WHERE cm.account_id = ? AND (c.type = 'DIRECT' OR t.status = 'ACTIVE')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Number of accountId's conversations that have at least one unread message — for the home.jsp badge, same pattern as unread notification/friend-request/invitation counts. */
    public int countUnreadConversations(int accountId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.ConversationMembers cm "
                + "JOIN dbo.Conversations c ON c.conversation_id = cm.conversation_id "
                + "LEFT JOIN dbo.Teams t ON t.team_id = c.team_id "
                + "WHERE cm.account_id = ? AND (c.type = 'DIRECT' OR t.status = 'ACTIVE') "
                + "AND EXISTS (SELECT 1 FROM dbo.Messages m WHERE m.conversation_id = c.conversation_id "
                + "  AND m.deleted_at IS NULL AND m.message_id > ISNULL(cm.last_read_message_id, 0))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Optional<Conversation> findOne(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        }
    }

    private Conversation mapListRow(ResultSet rs) throws SQLException {
        Conversation conversation = mapRow(rs);
        conversation.setTeamName(rs.getString("team_name"));
        conversation.setOtherUsername(rs.getString("other_username"));
        conversation.setOtherDisplayName(rs.getString("other_display_name"));
        conversation.setOtherAvatarUrl(rs.getString("other_avatar_url"));
        conversation.setLatestMessageContent(rs.getString("latest_message_content"));
        conversation.setLatestMessageCreatedAt(rs.getObject("latest_message_created_at", LocalDateTime.class));
        int latestSenderId = rs.getInt("latest_message_sender_id");
        conversation.setLatestMessageSenderId(rs.wasNull() ? null : latestSenderId);
        conversation.setLatestMessageDeleted(rs.getObject("latest_message_deleted_at") != null);
        conversation.setLatestMessageHasAttachment(rs.getObject("latest_message_attachment_id") != null);
        conversation.setUnreadCount(rs.getInt("unread_count"));
        return conversation;
    }

    private Conversation mapRow(ResultSet rs) throws SQLException {
        Conversation conversation = new Conversation();
        conversation.setConversationId(rs.getInt("conversation_id"));
        conversation.setType(rs.getString("type"));
        int teamId = rs.getInt("team_id");
        conversation.setTeamId(rs.wasNull() ? null : teamId);
        conversation.setDirectKey(rs.getString("direct_key"));
        conversation.setStatus(rs.getString("status"));
        conversation.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        conversation.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return conversation;
    }
}
