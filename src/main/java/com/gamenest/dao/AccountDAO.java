package com.gamenest.dao;

import com.gamenest.exception.DuplicateAccountException;
import com.gamenest.model.Account;
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
 * SQL Server unique-constraint violation error codes (used to detect
 * duplicate username/email races that slip past the service-layer pre-check).
 */
public class AccountDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

    private static final String SELECT_COLUMNS =
            "account_id, username, email, password_hash, display_name, avatar_url, "
                    + "status, role, created_at, updated_at ";

    public Account insert(Account account) throws SQLException, DuplicateAccountException {
        String sql = "INSERT INTO dbo.Accounts (username, email, password_hash, display_name, status) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, account.getUsername());
            ps.setString(2, account.getEmail());
            ps.setString(3, account.getPasswordHash());
            ps.setString(4, account.getDisplayName());
            ps.setString(5, account.getStatus());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    account.setAccountId(keys.getInt(1));
                }
            }
            return account;

        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                throw new DuplicateAccountException("Username hoặc email đã tồn tại.");
            }
            throw e;
        }
    }

    public void updatePasswordByEmail(String email, String newPasswordHash) throws SQLException {
        String sql = "UPDATE dbo.Accounts SET password_hash = ?, updated_at = SYSUTCDATETIME() WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setString(2, email);
            ps.executeUpdate();
        }
    }

    public Optional<Account> findByUsername(String username) throws SQLException {
        String sql = "SELECT account_id, username, email, password_hash, display_name, avatar_url, "
                + "status, role, created_at, updated_at FROM dbo.Accounts WHERE username = ?";
        return findOne(sql, username);
    }

    public Optional<Account> findByEmail(String email) throws SQLException {
        String sql = "SELECT account_id, username, email, password_hash, display_name, avatar_url, "
                + "status, role, created_at, updated_at FROM dbo.Accounts WHERE email = ?";
        return findOne(sql, email);
    }

    public Optional<Account> findByUsernameOrEmail(String identifier) throws SQLException {
        String sql = "SELECT account_id, username, email, password_hash, display_name, avatar_url, "
                + "status, role, created_at, updated_at FROM dbo.Accounts WHERE username = ? OR email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, identifier);
            ps.setString(2, identifier);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    // ---- Admin ----

    public Optional<Account> findById(int accountId) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + "FROM dbo.Accounts WHERE account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    public List<Account> findAllForAdmin(int offset, int limit) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + "FROM dbo.Accounts ORDER BY created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, offset);
            ps.setInt(2, limit);
            return mapList(ps);
        }
    }

    public int countAllForAdmin() throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Accounts";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<Account> searchForAdmin(String query, int offset, int limit) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + "FROM dbo.Accounts WHERE username LIKE ? ESCAPE '\\' OR email LIKE ? ESCAPE '\\' "
                + "ORDER BY created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = likePattern(query);
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setInt(3, offset);
            ps.setInt(4, limit);
            return mapList(ps);
        }
    }

    public int countSearchForAdmin(String query) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Accounts WHERE username LIKE ? ESCAPE '\\' OR email LIKE ? ESCAPE '\\'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = likePattern(query);
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Status is the only lifecycle field an admin can change here — Accounts
     * are core business data and must never be hard-deleted (CLAUDE.md §7.1).
     */
    public int updateStatus(int accountId, String status) throws SQLException {
        String sql = "UPDATE dbo.Accounts SET status = ?, updated_at = SYSUTCDATETIME() WHERE account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        }
    }

    /**
     * Self-service profile edit — updates only display_name for the given
     * account. No other column is touched by this method.
     */
    public int updateDisplayName(int accountId, String displayName) throws SQLException {
        String sql = "UPDATE dbo.Accounts SET display_name = ?, updated_at = SYSUTCDATETIME() WHERE account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, displayName);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        }
    }

    /**
     * Self-service avatar update — updates only avatar_url for the given
     * account. No other column is touched by this method.
     */
    public int updateAvatarUrl(int accountId, String avatarUrl) throws SQLException {
        String sql = "UPDATE dbo.Accounts SET avatar_url = ?, updated_at = SYSUTCDATETIME() WHERE account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, avatarUrl);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        }
    }

    private String likePattern(String query) {
        String escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private List<Account> mapList(PreparedStatement ps) throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                accounts.add(mapRow(rs));
            }
        }
        return accounts;
    }

    private Optional<Account> findOne(String sql, String param) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, param);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setAccountId(rs.getInt("account_id"));
        account.setUsername(rs.getString("username"));
        account.setEmail(rs.getString("email"));
        account.setPasswordHash(rs.getString("password_hash"));
        account.setDisplayName(rs.getString("display_name"));
        account.setAvatarUrl(rs.getString("avatar_url"));
        account.setStatus(rs.getString("status"));
        account.setRole(rs.getString("role"));
        account.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        account.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return account;
    }
}
