package com.gamenest.dao;

import com.gamenest.exception.DuplicateLFGMemberException;
import com.gamenest.model.LFGMember;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * LFGMembers is a junction table (creator+member relationship) — real
 * DELETE is allowed when a member leaves (task spec §22; GameNestSvcLogin
 * already has GRANT DELETE ON dbo.LFGMembers from 06_gamenest_svc_login.sql,
 * confirmed, no new permission migration needed for this task).
 */
public class LFGMemberDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

    public void insert(Connection conn, int lfgId, int accountId)
            throws SQLException, DuplicateLFGMemberException {
        String sql = "INSERT INTO dbo.LFGMembers (lfg_id, account_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lfgId);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                throw new DuplicateLFGMemberException("Bạn đã tham gia nhóm này rồi.");
            }
            throw e;
        }
    }

    /**
     * Scoped strictly to (lfg_id, account_id) — always called with
     * account_id from the caller's own session (task spec §11), so a
     * member can only ever remove their OWN membership row.
     */
    public int delete(Connection conn, int lfgId, int accountId) throws SQLException {
        String sql = "DELETE FROM dbo.LFGMembers WHERE lfg_id = ? AND account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lfgId);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        }
    }

    public boolean isMember(int lfgId, int accountId) throws SQLException {
        String sql = "SELECT 1 FROM dbo.LFGMembers WHERE lfg_id = ? AND account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lfgId);
            ps.setInt(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<LFGMember> findByLfgId(int lfgId) throws SQLException {
        String sql = "SELECT m.lfg_member_id, m.lfg_id, m.account_id, m.joined_at, "
                + "a.username, a.display_name, a.avatar_url "
                + "FROM dbo.LFGMembers m JOIN dbo.Accounts a ON a.account_id = m.account_id "
                + "WHERE m.lfg_id = ? ORDER BY m.joined_at ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lfgId);
            List<LFGMember> members = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapRow(rs));
                }
            }
            return members;
        }
    }

    private LFGMember mapRow(ResultSet rs) throws SQLException {
        LFGMember member = new LFGMember();
        member.setLfgMemberId(rs.getInt("lfg_member_id"));
        member.setLfgId(rs.getInt("lfg_id"));
        member.setAccountId(rs.getInt("account_id"));
        member.setJoinedAt(rs.getObject("joined_at", LocalDateTime.class));
        member.setUsername(rs.getString("username"));
        member.setDisplayName(rs.getString("display_name"));
        member.setAvatarUrl(rs.getString("avatar_url"));
        return member;
    }
}
