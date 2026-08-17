package com.gamenest.dao;

import com.gamenest.model.AccountBlock;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountBlocks is a pure junction/restriction table (db/18_account_blocks.sql)
 * — no status, no soft-delete. Unblock is a real DELETE, always scoped by the
 * caller to (blocker_account_id, blocked_account_id) so a User can never
 * affect another account's block row (see AccountBlockService).
 * <p>
 * {@link #listBlocked} selects only the public profile columns (account_id,
 * username, display_name, avatar_url) — never email/password_hash/status/
 * role — matching the same minimal-column pattern AccountFollowDAO/
 * AccountFriendshipDAO already use for their own list queries.
 */
public class AccountBlockDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

    /**
     * Inserts (blockerAccountId → blockedAccountId) if it doesn't already
     * exist; returns {@code false} instead of throwing when it does, so
     * AccountBlockService can treat a duplicate Block as a plain idempotent
     * no-op (task spec §6) rather than exception-driven control flow —
     * still race-safe: the UNIQUE constraint is the actual guard, this just
     * absorbs the resulting unique-violation instead of surfacing it.
     * Runs on the caller-supplied Connection so it shares AccountBlockService's
     * transaction with the Follow/Friendship cleanup (task spec §8).
     */
    public boolean insertIfAbsent(Connection conn, int blockerAccountId, int blockedAccountId) throws SQLException {
        String sql = "INSERT INTO dbo.AccountBlocks (blocker_account_id, blocked_account_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, blockerAccountId);
            ps.setInt(2, blockedAccountId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                return false;
            }
            throw e;
        }
    }

    /** Scoped to (blocker_account_id, blocked_account_id) — 0 rows affected is a normal, silent no-op. */
    public int delete(int blockerAccountId, int blockedAccountId) throws SQLException {
        String sql = "DELETE FROM dbo.AccountBlocks WHERE blocker_account_id = ? AND blocked_account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, blockerAccountId);
            ps.setInt(2, blockedAccountId);
            return ps.executeUpdate();
        }
    }

    /** Exact direction only: does blockerAccountId block blockedAccountId? */
    public boolean exists(int blockerAccountId, int blockedAccountId) throws SQLException {
        String sql = "SELECT 1 FROM dbo.AccountBlocks WHERE blocker_account_id = ? AND blocked_account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, blockerAccountId);
            ps.setInt(2, blockedAccountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Either direction: is there a block between accountA and accountB at
     * all? Used as the two-sided restriction check by Follow/Friend (task
     * spec §10/§11/§12) — the database still only ever stores the
     * one-directional row(s), this just checks both.
     */
    public boolean existsEitherDirection(int accountA, int accountB) throws SQLException {
        String sql = "SELECT 1 FROM dbo.AccountBlocks WHERE "
                + "(blocker_account_id = ? AND blocked_account_id = ?) "
                + "OR (blocker_account_id = ? AND blocked_account_id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountA);
            ps.setInt(2, accountB);
            ps.setInt(3, accountB);
            ps.setInt(4, accountA);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Accounts blockerAccountId has blocked — newest block first, DB-side paginated. */
    public List<AccountBlock> listBlocked(int blockerAccountId, int offset, int limit) throws SQLException {
        String sql = "SELECT b.block_id, b.blocker_account_id, b.blocked_account_id, b.created_at, "
                + "a.username AS blocked_username, a.display_name AS blocked_display_name, a.avatar_url AS blocked_avatar_url "
                + "FROM dbo.AccountBlocks b JOIN dbo.Accounts a ON a.account_id = b.blocked_account_id "
                + "WHERE b.blocker_account_id = ? ORDER BY b.created_at DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, blockerAccountId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            List<AccountBlock> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        }
    }

    public int countBlocked(int blockerAccountId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.AccountBlocks WHERE blocker_account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, blockerAccountId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private AccountBlock mapRow(ResultSet rs) throws SQLException {
        AccountBlock b = new AccountBlock();
        b.setBlockId(rs.getInt("block_id"));
        b.setBlockerAccountId(rs.getInt("blocker_account_id"));
        b.setBlockedAccountId(rs.getInt("blocked_account_id"));
        b.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        b.setBlockedUsername(rs.getString("blocked_username"));
        b.setBlockedDisplayName(rs.getString("blocked_display_name"));
        b.setBlockedAvatarUrl(rs.getString("blocked_avatar_url"));
        return b;
    }
}
