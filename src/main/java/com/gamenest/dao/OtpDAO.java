package com.gamenest.dao;

import com.gamenest.model.OtpRecord;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class OtpDAO {

    public int insert(String email, String purpose, String otpHash, LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO dbo.OtpVerifications (email, purpose, otp_hash, expires_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, email);
            ps.setString(2, purpose);
            ps.setString(3, otpHash);
            ps.setTimestamp(4, Timestamp.valueOf(expiresAt));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Không lấy được otp_id vừa tạo.");
        }
    }

    /**
     * Most recent, still-usable (not used, not expired) OTP for the given
     * email + purpose. Used both for resend-cooldown checks and verification.
     */
    public Optional<OtpRecord> findLatestActive(String email, String purpose) throws SQLException {
        String sql = "SELECT TOP 1 otp_id, email, purpose, otp_hash, expires_at, is_used, attempt_count, created_at "
                + "FROM dbo.OtpVerifications "
                + "WHERE email = ? AND purpose = ? AND is_used = 0 AND expires_at > SYSUTCDATETIME() "
                + "ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, purpose);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    public void incrementAttempt(int otpId) throws SQLException {
        String sql = "UPDATE dbo.OtpVerifications SET attempt_count = attempt_count + 1 WHERE otp_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        }
    }

    public void markUsed(int otpId) throws SQLException {
        String sql = "UPDATE dbo.OtpVerifications SET is_used = 1 WHERE otp_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        }
    }

    private OtpRecord mapRow(ResultSet rs) throws SQLException {
        OtpRecord record = new OtpRecord();
        record.setOtpId(rs.getInt("otp_id"));
        record.setEmail(rs.getString("email"));
        record.setPurpose(rs.getString("purpose"));
        record.setOtpHash(rs.getString("otp_hash"));
        record.setExpiresAt(rs.getObject("expires_at", LocalDateTime.class));
        record.setUsed(rs.getBoolean("is_used"));
        record.setAttemptCount(rs.getInt("attempt_count"));
        record.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return record;
    }
}
