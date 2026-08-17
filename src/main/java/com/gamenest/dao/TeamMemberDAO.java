package com.gamenest.dao;

import com.gamenest.model.TeamMember;
import com.gamenest.model.TeamMemberRole;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TeamMembers is a pure membership junction table (db/19_teams.sql, like
 * LFGMembers) — Leave/Remove is a real DELETE, always scoped to
 * (team_id, account_id, role = MEMBER) so neither path can ever touch the
 * OWNER row (see TeamService). {@link #insert} deliberately does not
 * translate a unique-violation into a dedicated exception: the only two
 * call sites (Create Team's brand-new team_id, and Accept Invitation's
 * PENDING-guarded UPDATE) already make a duplicate practically unreachable
 * — UQ_TeamMembers_team_account remains as a pure DB-level backstop.
 */
public class TeamMemberDAO {

    private static final String SELECT_COLUMNS =
            "tm.team_member_id, tm.team_id, tm.account_id, tm.role, tm.joined_at, "
                    + "a.username, a.display_name, a.avatar_url ";
    private static final String BASE_SELECT =
            "SELECT " + SELECT_COLUMNS + "FROM dbo.TeamMembers tm JOIN dbo.Accounts a ON a.account_id = tm.account_id ";

    public void insert(Connection conn, int teamId, int accountId, String role) throws SQLException {
        String sql = "INSERT INTO dbo.TeamMembers (team_id, account_id, role) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, accountId);
            ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    /** Scoped to (team_id, account_id, role = MEMBER) — used by both Leave (self) and Remove Member (by Owner); never touches the OWNER row. */
    public int deleteMemberIfRole(int teamId, int accountId) throws SQLException {
        String sql = "DELETE FROM dbo.TeamMembers WHERE team_id = ? AND account_id = ? AND role = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, accountId);
            ps.setString(3, TeamMemberRole.MEMBER);
            return ps.executeUpdate();
        }
    }

    /**
     * Chat integration addition (purely additive — {@link #deleteMemberIfRole(int, int)}
     * above is unchanged): same guard, reusing a caller-supplied Connection
     * so Leave/Remove Member can delete the matching ConversationMembers
     * row in the same transaction (see TeamService).
     */
    public int deleteMemberIfRole(Connection conn, int teamId, int accountId) throws SQLException {
        String sql = "DELETE FROM dbo.TeamMembers WHERE team_id = ? AND account_id = ? AND role = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, accountId);
            ps.setString(3, TeamMemberRole.MEMBER);
            return ps.executeUpdate();
        }
    }

    public Optional<TeamMember> findMembership(int teamId, int accountId) throws SQLException {
        String sql = BASE_SELECT + "WHERE tm.team_id = ? AND tm.account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /** All members of a team, Owner first then by join date — teams are small (private friend rooms), so no pagination. */
    public List<TeamMember> listMembers(int teamId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return listMembers(conn, teamId);
        }
    }

    /**
     * Chat integration addition (purely additive — {@link #listMembers(int)}
     * above is unchanged): same query, reusing a caller-supplied Connection
     * so ChatService's Team Chat self-heal (backfilling ConversationMembers
     * for a legacy Team's current members) reads within the same
     * transaction it writes in.
     */
    public List<TeamMember> listMembers(Connection conn, int teamId) throws SQLException {
        String sql = BASE_SELECT + "WHERE tm.team_id = ? ORDER BY CASE WHEN tm.role = ? THEN 0 ELSE 1 END, tm.joined_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setString(2, TeamMemberRole.OWNER);
            List<TeamMember> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        }
    }

    /**
     * Transfer Ownership step 1 (must run before {@link #setOwnerRole} in
     * the same transaction): demotes the current owner. Guarded by
     * role = OWNER so it only succeeds if accountId really is the current
     * owner — 0 rows means a race (someone else already transferred), and
     * the caller must roll back.
     */
    public int setMemberRole(Connection conn, int teamId, int accountId) throws SQLException {
        String sql = "UPDATE dbo.TeamMembers SET role = ? WHERE team_id = ? AND account_id = ? AND role = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, TeamMemberRole.MEMBER);
            ps.setInt(2, teamId);
            ps.setInt(3, accountId);
            ps.setString(4, TeamMemberRole.OWNER);
            return ps.executeUpdate();
        }
    }

    /**
     * Transfer Ownership step 2: promotes the target. Guarded by
     * role = MEMBER so it only succeeds if the target really is a current
     * member — never fires if the target already left/was removed
     * concurrently. Must run after {@link #setMemberRole} so the
     * UQ_TeamMembers_team_owner filtered unique index (at most 1 OWNER row
     * per team) is never violated mid-transaction.
     */
    public int setOwnerRole(Connection conn, int teamId, int accountId) throws SQLException {
        String sql = "UPDATE dbo.TeamMembers SET role = ? WHERE team_id = ? AND account_id = ? AND role = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, TeamMemberRole.OWNER);
            ps.setInt(2, teamId);
            ps.setInt(3, accountId);
            ps.setString(4, TeamMemberRole.MEMBER);
            return ps.executeUpdate();
        }
    }

    private TeamMember mapRow(ResultSet rs) throws SQLException {
        TeamMember member = new TeamMember();
        member.setTeamMemberId(rs.getInt("team_member_id"));
        member.setTeamId(rs.getInt("team_id"));
        member.setAccountId(rs.getInt("account_id"));
        member.setRole(rs.getString("role"));
        member.setJoinedAt(rs.getObject("joined_at", LocalDateTime.class));
        member.setUsername(rs.getString("username"));
        member.setDisplayName(rs.getString("display_name"));
        member.setAvatarUrl(rs.getString("avatar_url"));
        return member;
    }
}
