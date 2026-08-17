package com.gamenest.dao;

import com.gamenest.exception.DuplicateAccountFollowException;
import com.gamenest.model.Account;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountFollows is a pure junction table (db/16_account_follows.sql) — no
 * status, no soft-delete. Unfollow is a real DELETE, always scoped by the
 * caller to (follower_account_id, following_account_id) so a User can never
 * affect another account's relationship row (see AccountFollowService).
 * <p>
 * {@link #listFollowers}/{@link #listFollowing} select only the public
 * profile columns (account_id, username, display_name, avatar_url) — never
 * email/password_hash/status/role — so the returned {@link Account} objects
 * simply have no other field populated, matching the same minimal-column
 * pattern LFGMemberDAO already uses for its member list.
 */
public class AccountFollowDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

    public void insert(int followerAccountId, int followingAccountId)
            throws SQLException, DuplicateAccountFollowException {
        String sql = "INSERT INTO dbo.AccountFollows (follower_account_id, following_account_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, followerAccountId);
            ps.setInt(2, followingAccountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                throw new DuplicateAccountFollowException("Bạn đã Follow người dùng này rồi.");
            }
            throw e;
        }
    }

    /** Scoped to (follower_account_id, following_account_id) — 0 rows affected is a normal, silent no-op. */
    public int delete(int followerAccountId, int followingAccountId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, followerAccountId, followingAccountId);
        }
    }

    /**
     * Same as {@link #delete(int, int)} but reuses a caller-supplied
     * Connection — used by AccountBlockService so the Follow cleanup runs in
     * the same transaction as the Block INSERT (task spec §8).
     */
    public int delete(Connection conn, int followerAccountId, int followingAccountId) throws SQLException {
        String sql = "DELETE FROM dbo.AccountFollows WHERE follower_account_id = ? AND following_account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, followerAccountId);
            ps.setInt(2, followingAccountId);
            return ps.executeUpdate();
        }
    }

    public boolean exists(int followerAccountId, int followingAccountId) throws SQLException {
        String sql = "SELECT 1 FROM dbo.AccountFollows WHERE follower_account_id = ? AND following_account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, followerAccountId);
            ps.setInt(2, followingAccountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** "Ai đang Follow tôi?" */
    public int countFollowers(int accountId) throws SQLException {
        return count("following_account_id", accountId);
    }

    /** "Tôi đang Follow ai?" */
    public int countFollowing(int accountId) throws SQLException {
        return count("follower_account_id", accountId);
    }

    private int count(String column, int accountId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.AccountFollows WHERE " + column + " = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Accounts that follow accountId — newest follow first, DB-side paginated. */
    public List<Account> listFollowers(int accountId, int offset, int limit) throws SQLException {
        String sql = "SELECT a.account_id, a.username, a.display_name, a.avatar_url "
                + "FROM dbo.AccountFollows f JOIN dbo.Accounts a ON a.account_id = f.follower_account_id "
                + "WHERE f.following_account_id = ? ORDER BY f.created_at DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return listAccounts(sql, accountId, offset, limit);
    }

    /** Accounts accountId follows — newest follow first, DB-side paginated. */
    public List<Account> listFollowing(int accountId, int offset, int limit) throws SQLException {
        String sql = "SELECT a.account_id, a.username, a.display_name, a.avatar_url "
                + "FROM dbo.AccountFollows f JOIN dbo.Accounts a ON a.account_id = f.following_account_id "
                + "WHERE f.follower_account_id = ? ORDER BY f.created_at DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return listAccounts(sql, accountId, offset, limit);
    }

    private List<Account> listAccounts(String sql, int accountId, int offset, int limit) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            List<Account> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Account account = new Account();
                    account.setAccountId(rs.getInt("account_id"));
                    account.setUsername(rs.getString("username"));
                    account.setDisplayName(rs.getString("display_name"));
                    account.setAvatarUrl(rs.getString("avatar_url"));
                    results.add(account);
                }
            }
            return results;
        }
    }
}
