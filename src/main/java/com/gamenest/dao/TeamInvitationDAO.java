package com.gamenest.dao;

import com.gamenest.exception.DuplicateTeamInvitationException;
import com.gamenest.model.TeamInvitation;
import com.gamenest.model.TeamInvitationStatus;
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
 * TeamInvitations is a state-machine table (db/19_teams.sql, like
 * AccountFriendships) — no hard delete, every transition is a guarded
 * conditional UPDATE. Every status-transition UPDATE includes an ownership
 * predicate (inviter_account_id/invitee_account_id = caller) AND a
 * {@code status = 'PENDING'} guard, so double-processing and cross-account
 * tampering both fail safely with 0 rows affected (see TeamService).
 */
public class TeamInvitationDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

    private static final String SELECT_COLUMNS =
            "invitation_id, team_id, inviter_account_id, invitee_account_id, status, created_at, responded_at ";
    private static final String BASE_SELECT = "SELECT " + SELECT_COLUMNS + "FROM dbo.TeamInvitations ";

    /** Only used when no row exists yet for this exact (team, invitee) pair. */
    public TeamInvitation insert(int teamId, int inviterAccountId, int inviteeAccountId)
            throws SQLException, DuplicateTeamInvitationException {
        String sql = "INSERT INTO dbo.TeamInvitations (team_id, inviter_account_id, invitee_account_id, status) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, teamId);
            ps.setInt(2, inviterAccountId);
            ps.setInt(3, inviteeAccountId);
            ps.setString(4, TeamInvitationStatus.PENDING);
            ps.executeUpdate();

            TeamInvitation invitation = new TeamInvitation();
            invitation.setTeamId(teamId);
            invitation.setInviterAccountId(inviterAccountId);
            invitation.setInviteeAccountId(inviteeAccountId);
            invitation.setStatus(TeamInvitationStatus.PENDING);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    invitation.setInvitationId(keys.getInt(1));
                }
            }
            return invitation;

        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                throw new DuplicateTeamInvitationException("Đã tồn tại lời mời cho người này trong nhóm.");
            }
            throw e;
        }
    }

    /** The row for this exact (team, invitee) pair, any status — at most one, per UQ_TeamInvitations_team_invitee. */
    public Optional<TeamInvitation> findByPair(int teamId, int inviteeAccountId) throws SQLException {
        String sql = BASE_SELECT + "WHERE team_id = ? AND invitee_account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, inviteeAccountId);
            return findOne(ps);
        }
    }

    public Optional<TeamInvitation> findById(int invitationId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, invitationId);
        }
    }

    /** Same as {@link #findById(int)} but reuses a caller-supplied Connection — used inside acceptInvitation's transaction. */
    public Optional<TeamInvitation> findById(Connection conn, int invitationId) throws SQLException {
        String sql = BASE_SELECT + "WHERE invitation_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invitationId);
            return findOne(ps);
        }
    }

    /**
     * Re-invite after REJECTED/CANCELLED: reuses the existing row instead of
     * inserting a new one (would violate UQ_TeamInvitations_team_invitee).
     * inviter_account_id is refreshed too — the new invite could come from a
     * different (now current) Owner after a Transfer Ownership.
     */
    public int reactivateAsPending(int invitationId, int newInviterAccountId) throws SQLException {
        String sql = "UPDATE dbo.TeamInvitations SET status = ?, inviter_account_id = ?, responded_at = NULL, "
                + "created_at = SYSUTCDATETIME() WHERE invitation_id = ? AND status IN (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, TeamInvitationStatus.PENDING);
            ps.setInt(2, newInviterAccountId);
            ps.setInt(3, invitationId);
            ps.setString(4, TeamInvitationStatus.REJECTED);
            ps.setString(5, TeamInvitationStatus.CANCELLED);
            return ps.executeUpdate();
        }
    }

    /**
     * Runs on the caller-supplied Connection so Accept shares a transaction
     * with the TeamMembers INSERT (see TeamService#acceptInvitation). Also
     * guarded by the parent Team still being ACTIVE (audit fix): without
     * this, a Team soft-deleted between Invite and Accept could still gain
     * a new TeamMembers row via this UPDATE succeeding on the invitation
     * alone — the EXISTS check makes "Team must be ACTIVE" part of the same
     * atomic guard instead of a separate check-then-act step.
     */
    public int acceptIfPending(Connection conn, int invitationId, int inviteeAccountId) throws SQLException {
        String sql = "UPDATE dbo.TeamInvitations SET status = ?, responded_at = SYSUTCDATETIME() "
                + "WHERE invitation_id = ? AND invitee_account_id = ? AND status = ? "
                + "AND EXISTS (SELECT 1 FROM dbo.Teams t WHERE t.team_id = dbo.TeamInvitations.team_id AND t.status = ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, TeamInvitationStatus.ACCEPTED);
            ps.setInt(2, invitationId);
            ps.setInt(3, inviteeAccountId);
            ps.setString(4, TeamInvitationStatus.PENDING);
            ps.setString(5, TeamStatus.ACTIVE);
            return ps.executeUpdate();
        }
    }

    public int rejectIfPending(int invitationId, int inviteeAccountId) throws SQLException {
        return transitionIfPending(invitationId, "invitee_account_id", inviteeAccountId, TeamInvitationStatus.REJECTED);
    }

    public int cancelIfPending(int invitationId, int inviterAccountId) throws SQLException {
        return transitionIfPending(invitationId, "inviter_account_id", inviterAccountId, TeamInvitationStatus.CANCELLED);
    }

    private int transitionIfPending(int invitationId, String ownerColumn, int ownerAccountId, String newStatus)
            throws SQLException {
        String sql = "UPDATE dbo.TeamInvitations SET status = ?, responded_at = SYSUTCDATETIME() "
                + "WHERE invitation_id = ? AND " + ownerColumn + " = ? AND status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, invitationId);
            ps.setInt(3, ownerAccountId);
            ps.setString(4, TeamInvitationStatus.PENDING);
            return ps.executeUpdate();
        }
    }

    /**
     * PENDING invitations where accountId is the invitee — "my incoming Team
     * invitations" (task spec §18). Excludes invitations whose Team was
     * soft-deleted after the invite was sent (audit fix): such an
     * invitation can no longer actually be accepted (see
     * {@link #acceptIfPending}'s Team-ACTIVE guard), so it must not be shown
     * as if it still could be.
     */
    public List<TeamInvitation> listIncomingInvitations(int accountId, int offset, int limit) throws SQLException {
        String sql = "SELECT i.invitation_id, i.team_id, i.inviter_account_id, i.invitee_account_id, i.status, "
                + "i.created_at, i.responded_at, "
                + "t.name AS team_name, u.username AS inviter_username, u.display_name AS inviter_display_name "
                + "FROM dbo.TeamInvitations i "
                + "JOIN dbo.Teams t ON t.team_id = i.team_id "
                + "JOIN dbo.Accounts u ON u.account_id = i.inviter_account_id "
                + "WHERE i.invitee_account_id = ? AND i.status = ? AND t.status = ? "
                + "ORDER BY i.created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, TeamInvitationStatus.PENDING);
            ps.setString(3, TeamStatus.ACTIVE);
            ps.setInt(4, offset);
            ps.setInt(5, limit);
            List<TeamInvitation> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TeamInvitation invitation = mapRow(rs);
                    invitation.setTeamName(rs.getString("team_name"));
                    invitation.setInviterUsername(rs.getString("inviter_username"));
                    invitation.setInviterDisplayName(rs.getString("inviter_display_name"));
                    results.add(invitation);
                }
            }
            return results;
        }
    }

    /** Same Team-ACTIVE exclusion as {@link #listIncomingInvitations} — keeps the badge count consistent with what's actually listed. */
    public int countIncomingInvitations(int accountId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.TeamInvitations i JOIN dbo.Teams t ON t.team_id = i.team_id "
                + "WHERE i.invitee_account_id = ? AND i.status = ? AND t.status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, TeamInvitationStatus.PENDING);
            ps.setString(3, TeamStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** PENDING invitations sent by a team — for the Owner's "pending invitations" section on Team Detail, so they can Cancel. */
    public List<TeamInvitation> listPendingForTeam(int teamId) throws SQLException {
        String sql = "SELECT i.invitation_id, i.team_id, i.inviter_account_id, i.invitee_account_id, i.status, "
                + "i.created_at, i.responded_at, "
                + "a.username AS invitee_username, a.display_name AS invitee_display_name, a.avatar_url AS invitee_avatar_url "
                + "FROM dbo.TeamInvitations i "
                + "JOIN dbo.Accounts a ON a.account_id = i.invitee_account_id "
                + "WHERE i.team_id = ? AND i.status = ? "
                + "ORDER BY i.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setString(2, TeamInvitationStatus.PENDING);
            List<TeamInvitation> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TeamInvitation invitation = mapRow(rs);
                    invitation.setInviteeUsername(rs.getString("invitee_username"));
                    invitation.setInviteeDisplayName(rs.getString("invitee_display_name"));
                    invitation.setInviteeAvatarUrl(rs.getString("invitee_avatar_url"));
                    results.add(invitation);
                }
            }
            return results;
        }
    }

    private Optional<TeamInvitation> findOne(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        }
    }

    private TeamInvitation mapRow(ResultSet rs) throws SQLException {
        TeamInvitation invitation = new TeamInvitation();
        invitation.setInvitationId(rs.getInt("invitation_id"));
        invitation.setTeamId(rs.getInt("team_id"));
        invitation.setInviterAccountId(rs.getInt("inviter_account_id"));
        invitation.setInviteeAccountId(rs.getInt("invitee_account_id"));
        invitation.setStatus(rs.getString("status"));
        invitation.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        invitation.setRespondedAt(rs.getObject("responded_at", LocalDateTime.class));
        return invitation;
    }
}
