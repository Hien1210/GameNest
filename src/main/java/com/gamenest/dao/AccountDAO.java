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
import java.util.Optional;

/**
 * SQL Server unique-constraint violation error codes (used to detect
 * duplicate username/email races that slip past the service-layer pre-check).
 */
public class AccountDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

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
