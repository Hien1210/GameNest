package com.gamenest.dao;

import com.gamenest.model.MessageReactionSummary;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MessageReactions is a pure junction table (db/22_chat_reaction.sql) —
 * each row represents an account's CURRENT reaction on a message, not
 * historical content, so un-react/change-reaction are real DELETE/UPDATE
 * (CLAUDE.md §7.3), mirroring AccountFollows/ConversationMembers.
 * <p>
 * {@link #toggle} intentionally does NOT use SQL Server MERGE (project
 * constraint) — it is a plain SELECT-then-branch (UPDATE/INSERT/DELETE),
 * with {@code UNIQUE(message_id, account_id)} as the final safety net: a
 * concurrent INSERT race is caught via the same error-code convention every
 * other DAO in this project uses (2627/2601, see AccountFollowDAO) and
 * retried as an UPDATE, never left to propagate as an unhandled exception.
 * {@link #toggle} runs on a caller-supplied {@link Connection} because
 * {@code ChatService#toggleReaction} always wraps it in one transaction.
 */
public class MessageReactionDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

    /**
     * Sets accountId's reaction on messageId to emoji, or removes it if
     * emoji already equals the account's current reaction (toggle-off).
     * Returns the account's resulting reaction (the emoji, or null if
     * removed) — the server is the sole authority on ADD/REMOVE/CHANGE,
     * decided entirely from the DB's current state, never from a
     * client-declared intent.
     */
    public String toggle(Connection conn, int messageId, int accountId, String emoji) throws SQLException {
        String current = findEmoji(conn, messageId, accountId);

        if (emoji.equals(current)) {
            delete(conn, messageId, accountId);
            return null;
        }

        if (current != null) {
            int updated = update(conn, messageId, accountId, emoji);
            if (updated > 0) {
                return emoji;
            }
            // Row disappeared between the SELECT and this UPDATE (a
            // concurrent un-react) — fall through to insert-with-retry.
        }

        try {
            insert(conn, messageId, accountId, emoji);
            return emoji;
        } catch (SQLException e) {
            if (!isUniqueViolation(e)) {
                throw e;
            }
            // Another request inserted the row between our check and this
            // INSERT — it exists now, so finish as an UPDATE instead.
            int updated = update(conn, messageId, accountId, emoji);
            if (updated == 0) {
                // Extremely rare second race (a concurrent un-react right
                // after that concurrent insert) — one more attempt; any
                // further failure is left to surface rather than loop.
                insert(conn, messageId, accountId, emoji);
            }
            return emoji;
        }
    }

    private boolean isUniqueViolation(SQLException e) {
        return e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY;
    }

    private String findEmoji(Connection conn, int messageId, int accountId) throws SQLException {
        String sql = "SELECT emoji FROM dbo.MessageReactions WHERE message_id = ? AND account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("emoji") : null;
            }
        }
    }

    private void insert(Connection conn, int messageId, int accountId, String emoji) throws SQLException {
        String sql = "INSERT INTO dbo.MessageReactions (message_id, account_id, emoji) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, accountId);
            ps.setString(3, emoji);
            ps.executeUpdate();
        }
    }

    private int update(Connection conn, int messageId, int accountId, String emoji) throws SQLException {
        String sql = "UPDATE dbo.MessageReactions SET emoji = ? WHERE message_id = ? AND account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, emoji);
            ps.setInt(2, messageId);
            ps.setInt(3, accountId);
            return ps.executeUpdate();
        }
    }

    /** Scoped to (message_id, account_id) — 0 rows affected is a normal, silent no-op. */
    private int delete(Connection conn, int messageId, int accountId) throws SQLException {
        String sql = "DELETE FROM dbo.MessageReactions WHERE message_id = ? AND account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        }
    }

    /**
     * Aggregated reaction counts for one message, grouped by emoji — used
     * to build the shared {@code reactions} list of a REACTION_UPDATED
     * broadcast right after a toggle. Runs on the same transaction
     * Connection as {@link #toggle} so it sees the just-committed change
     * consistently (called before commit, within the same transaction).
     */
    public List<MessageReactionSummary> aggregateForMessage(Connection conn, int messageId) throws SQLException {
        String sql = "SELECT emoji, COUNT(*) AS cnt FROM dbo.MessageReactions "
                + "WHERE message_id = ? GROUP BY emoji ORDER BY MIN(message_reaction_id)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MessageReactionSummary> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new MessageReactionSummary(rs.getString("emoji"), rs.getInt("cnt")));
                }
                return results;
            }
        }
    }

    /** One account's current reaction on one message, or null — standalone connection. */
    public String findEmoji(int messageId, int accountId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findEmoji(conn, messageId, accountId);
        }
    }

    /**
     * Batch aggregation for a page of messages (Chat Detail SSR load) — one
     * query instead of N. Empty input never executes a query (an empty SQL
     * {@code IN ()} is invalid) and simply returns an empty map.
     */
    public Map<Integer, List<MessageReactionSummary>> aggregateForMessages(List<Integer> messageIds) throws SQLException {
        Map<Integer, List<MessageReactionSummary>> result = new HashMap<>();
        if (messageIds == null || messageIds.isEmpty()) {
            return result;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT message_id, emoji, COUNT(*) AS cnt FROM dbo.MessageReactions WHERE message_id IN (");
        appendPlaceholders(sql, messageIds.size());
        sql.append(") GROUP BY message_id, emoji ORDER BY message_id, MIN(message_reaction_id)");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindIds(ps, messageIds, 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int messageId = rs.getInt("message_id");
                    result.computeIfAbsent(messageId, k -> new ArrayList<>())
                            .add(new MessageReactionSummary(rs.getString("emoji"), rs.getInt("cnt")));
                }
            }
        }
        return result;
    }

    /** Batch "my reaction per message" for a page of messages (Chat Detail SSR load) — one query instead of N. */
    public Map<Integer, String> myReactionForMessages(List<Integer> messageIds, int accountId) throws SQLException {
        Map<Integer, String> result = new HashMap<>();
        if (messageIds == null || messageIds.isEmpty()) {
            return result;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT message_id, emoji FROM dbo.MessageReactions WHERE account_id = ? AND message_id IN (");
        appendPlaceholders(sql, messageIds.size());
        sql.append(")");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, accountId);
            bindIds(ps, messageIds, 2);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("message_id"), rs.getString("emoji"));
                }
            }
        }
        return result;
    }

    private void appendPlaceholders(StringBuilder sql, int count) {
        for (int i = 0; i < count; i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
    }

    private void bindIds(PreparedStatement ps, List<Integer> ids, int startIndex) throws SQLException {
        for (int i = 0; i < ids.size(); i++) {
            ps.setInt(startIndex + i, ids.get(i));
        }
    }
}
