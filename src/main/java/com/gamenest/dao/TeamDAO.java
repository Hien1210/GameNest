package com.gamenest.dao;

import com.gamenest.model.Team;
import com.gamenest.model.TeamStatus;
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
 * Teams is a soft-delete entity (db/19_teams.sql, CLAUDE.md §7.1 — a Team is
 * user-generated community content like Questions/LFGPosts) — there is no
 * DELETE anywhere in this DAO. owner_account_id is the CURRENT owner, not
 * just the creator; {@link #updateOwner} keeps it in sync with
 * TeamMembers.role during Transfer Ownership (see TeamService).
 */
public class TeamDAO {

    private static final String DETAIL_SELECT =
            "SELECT t.team_id, t.owner_account_id, t.name, t.description, t.status, t.created_at, t.updated_at, "
                    + "o.username AS owner_username, o.display_name AS owner_display_name, "
                    + "(SELECT COUNT(*) FROM dbo.TeamMembers tm2 WHERE tm2.team_id = t.team_id) AS member_count "
                    + "FROM dbo.Teams t JOIN dbo.Accounts o ON o.account_id = t.owner_account_id ";

    public Team insert(Connection conn, Team team) throws SQLException {
        String sql = "INSERT INTO dbo.Teams (owner_account_id, name, description, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, team.getOwnerAccountId());
            ps.setString(2, team.getName());
            ps.setString(3, team.getDescription());
            ps.setString(4, team.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    team.setTeamId(keys.getInt(1));
                }
            }
            return team;
        }
    }

    public Optional<Team> findById(int teamId) throws SQLException {
        String sql = DETAIL_SELECT + "WHERE t.team_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /** Guarded: only an ACTIVE team can be soft-deleted. Ownership is verified by the Service before calling this. */
    public int softDelete(int teamId) throws SQLException {
        String sql = "UPDATE dbo.Teams SET status = ?, updated_at = SYSUTCDATETIME() WHERE team_id = ? AND status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, TeamStatus.DELETED);
            ps.setInt(2, teamId);
            ps.setString(3, TeamStatus.ACTIVE);
            return ps.executeUpdate();
        }
    }

    /**
     * Transfer Ownership: guarded by the CURRENT owner_account_id, run on
     * the same Connection as the TeamMembers role swap (see
     * TeamService#transferOwnership) so both updates commit or roll back
     * together.
     */
    public int updateOwner(Connection conn, int teamId, int oldOwnerAccountId, int newOwnerAccountId) throws SQLException {
        String sql = "UPDATE dbo.Teams SET owner_account_id = ?, updated_at = SYSUTCDATETIME() "
                + "WHERE team_id = ? AND owner_account_id = ? AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newOwnerAccountId);
            ps.setInt(2, teamId);
            ps.setInt(3, oldOwnerAccountId);
            ps.setString(4, TeamStatus.ACTIVE);
            return ps.executeUpdate();
        }
    }

    /** Teams where accountId is currently a member, ACTIVE only — newest first, DB-side paginated. */
    public List<Team> listMyTeams(int accountId, int offset, int limit) throws SQLException {
        String sql = DETAIL_SELECT
                + "JOIN dbo.TeamMembers tm ON tm.team_id = t.team_id AND tm.account_id = ? "
                + "WHERE t.status = ? "
                + "ORDER BY t.created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, TeamStatus.ACTIVE);
            ps.setInt(3, offset);
            ps.setInt(4, limit);
            List<Team> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        }
    }

    public int countMyTeams(int accountId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Teams t "
                + "JOIN dbo.TeamMembers tm ON tm.team_id = t.team_id AND tm.account_id = ? "
                + "WHERE t.status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, TeamStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Team mapRow(ResultSet rs) throws SQLException {
        Team team = new Team();
        team.setTeamId(rs.getInt("team_id"));
        team.setOwnerAccountId(rs.getInt("owner_account_id"));
        team.setName(rs.getString("name"));
        team.setDescription(rs.getString("description"));
        team.setStatus(rs.getString("status"));
        team.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        team.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        team.setOwnerUsername(rs.getString("owner_username"));
        team.setOwnerDisplayName(rs.getString("owner_display_name"));
        team.setMemberCount(rs.getInt("member_count"));
        return team;
    }
}
