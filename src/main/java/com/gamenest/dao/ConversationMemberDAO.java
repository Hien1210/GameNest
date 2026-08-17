package com.gamenest.dao;

import com.gamenest.model.Account;
import com.gamenest.model.ConversationMember;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ConversationMembers is a pure membership junction table (db/20_chat.sql,
 * like TeamMembers/LFGMembers) — Leave/Remove is a real DELETE. {@link #insert}/
 * {@link #delete} only ever run on a caller-supplied Connection because they
 * are always one step of a larger transaction (Direct Conversation creation;
 * Team create/accept/leave/remove — see TeamService/ChatService), never a
 * standalone operation on their own in this task's scope.
 */
public class ConversationMemberDAO {

    public void insert(Connection conn, int conversationId, int accountId) throws SQLException {
        String sql = "INSERT INTO dbo.ConversationMembers (conversation_id, account_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    /** Scoped to (conversation_id, account_id) — hard delete, mirrors TeamMemberDAO#deleteMemberIfRole's scoping discipline. */
    public int delete(Connection conn, int conversationId, int accountId) throws SQLException {
        String sql = "DELETE FROM dbo.ConversationMembers WHERE conversation_id = ? AND account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        }
    }

    public Optional<ConversationMember> findByConversationAndAccount(int conversationId, int accountId) throws SQLException {
        String sql = "SELECT conversation_member_id, conversation_id, account_id, last_read_message_id, joined_at, left_at "
                + "FROM dbo.ConversationMembers WHERE conversation_id = ? AND account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Guarded: caller must be a current member, and messageId must actually
     * belong to this conversation (EXISTS check folded into the same
     * UPDATE, not a separate SELECT-then-UPDATE). Never regresses
     * last_read_message_id backward. A 0-row result (not a member, message
     * doesn't belong here, or already read further) is treated as a silent
     * no-op by the caller (task spec §17 "fail safely") — never an error.
     */
    public int markRead(int conversationId, int accountId, int messageId) throws SQLException {
        String sql = "UPDATE dbo.ConversationMembers SET last_read_message_id = ? "
                + "WHERE conversation_id = ? AND account_id = ? "
                + "AND EXISTS (SELECT 1 FROM dbo.Messages m WHERE m.message_id = ? AND m.conversation_id = ?) "
                + "AND (last_read_message_id IS NULL OR last_read_message_id < ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, conversationId);
            ps.setInt(3, accountId);
            ps.setInt(4, messageId);
            ps.setInt(5, conversationId);
            ps.setInt(6, messageId);
            return ps.executeUpdate();
        }
    }

    /**
     * The other participant of a DIRECT conversation — selects only public
     * profile columns (account_id, username, display_name, avatar_url),
     * mirroring AccountFollowDAO/AccountFriendshipDAO's minimal-column list
     * pattern. A DIRECT conversation always has exactly 2 members, so this
     * returns at most 1 row.
     */
    public Optional<Account> findOtherDirectParticipant(int conversationId, int selfAccountId) throws SQLException {
        String sql = "SELECT a.account_id, a.username, a.display_name, a.avatar_url "
                + "FROM dbo.ConversationMembers cm JOIN dbo.Accounts a ON a.account_id = cm.account_id "
                + "WHERE cm.conversation_id = ? AND cm.account_id <> ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, selfAccountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Account account = new Account();
                    account.setAccountId(rs.getInt("account_id"));
                    account.setUsername(rs.getString("username"));
                    account.setDisplayName(rs.getString("display_name"));
                    account.setAvatarUrl(rs.getString("avatar_url"));
                    return Optional.of(account);
                }
                return Optional.empty();
            }
        }
    }

    private ConversationMember mapRow(ResultSet rs) throws SQLException {
        ConversationMember member = new ConversationMember();
        member.setConversationMemberId(rs.getInt("conversation_member_id"));
        member.setConversationId(rs.getInt("conversation_id"));
        member.setAccountId(rs.getInt("account_id"));
        int lastReadMessageId = rs.getInt("last_read_message_id");
        member.setLastReadMessageId(rs.wasNull() ? null : lastReadMessageId);
        member.setJoinedAt(rs.getObject("joined_at", LocalDateTime.class));
        member.setLeftAt(rs.getObject("left_at", LocalDateTime.class));
        return member;
    }
}
