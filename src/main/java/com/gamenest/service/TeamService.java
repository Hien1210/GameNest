package com.gamenest.service;

import com.gamenest.dao.ConversationDAO;
import com.gamenest.dao.ConversationMemberDAO;
import com.gamenest.dao.TeamDAO;
import com.gamenest.dao.TeamInvitationDAO;
import com.gamenest.dao.TeamMemberDAO;
import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.DuplicateConversationException;
import com.gamenest.exception.DuplicateTeamInvitationException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.TeamInvitationNotFoundException;
import com.gamenest.exception.TeamNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;
import com.gamenest.model.AccountFriendship;
import com.gamenest.model.Conversation;
import com.gamenest.model.FriendshipStatus;
import com.gamenest.model.Team;
import com.gamenest.model.TeamInvitation;
import com.gamenest.model.TeamInvitationStatus;
import com.gamenest.model.TeamMember;
import com.gamenest.model.TeamMemberRole;
import com.gamenest.model.TeamStatus;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Team is a private, invite-only membership space between Friends — distinct
 * from LFG's public "find a stranger" model (task spec §1). Every business
 * rule here is enforced server-side, never inferred from what a JSP shows:
 * Owner-only actions re-verify {@code team.getOwnerAccountId() == callerId}
 * on every call, and every state-transition UPDATE also carries its own SQL
 * guard so authorization can never be bypassed by calling the DAO directly.
 * <p>
 * Reuses {@link AccountFriendService}/{@link AccountBlockService} as-is for
 * the Friend/Block rules (task spec §25) — no duplicated relationship logic.
 */
public class TeamService {

    private static final int PAGE_SIZE = 20;
    private static final int NAME_MAX_LENGTH = 150;
    private static final int DESCRIPTION_MAX_LENGTH = 5000;

    private final TeamDAO teamDAO;
    private final TeamMemberDAO teamMemberDAO;
    private final TeamInvitationDAO teamInvitationDAO;
    private final AccountService accountService;
    private final AccountFriendService accountFriendService;
    private final AccountBlockService accountBlockService;
    private final NotificationService notificationService;
    // Chat integration (additive — Team + Chat task): every ACTIVE Team has
    // exactly one TEAM Conversation, created/maintained here alongside
    // TeamMembers so it can never drift out of sync (see createTeam,
    // acceptInvitation, leaveTeam, removeMember).
    private final ConversationDAO conversationDAO;
    private final ConversationMemberDAO conversationMemberDAO;

    public TeamService() {
        this.teamDAO = new TeamDAO();
        this.teamMemberDAO = new TeamMemberDAO();
        this.teamInvitationDAO = new TeamInvitationDAO();
        this.accountService = new AccountService();
        this.accountFriendService = new AccountFriendService();
        this.accountBlockService = new AccountBlockService();
        this.notificationService = new NotificationService();
        this.conversationDAO = new ConversationDAO();
        this.conversationMemberDAO = new ConversationMemberDAO();
    }

    /**
     * ownerAccountId always comes from the caller's session (task spec §5) —
     * the creator becomes OWNER immediately, in the same transaction as the
     * Team row itself, so a Team can never exist without its first member.
     * <p>
     * Chat integration (additive): the Team's TEAM Conversation and the
     * owner's ConversationMembers row are created in this same transaction
     * — a Team can never exist without its Team Chat (Chat task spec §6).
     */
    public Team createTeam(int ownerAccountId, String name, String description) throws ValidationException, SQLException {
        String normalizedName = name == null ? null : name.trim();
        String normalizedDescription = normalizeOptional(description);
        validateName(normalizedName);
        validateDescription(normalizedDescription);

        Team team = new Team();
        team.setOwnerAccountId(ownerAccountId);
        team.setName(normalizedName);
        team.setDescription(normalizedDescription);
        team.setStatus(TeamStatus.ACTIVE);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Team inserted = teamDAO.insert(conn, team);
                teamMemberDAO.insert(conn, inserted.getTeamId(), ownerAccountId, TeamMemberRole.OWNER);
                Conversation conversation = conversationDAO.insertTeam(conn, inserted.getTeamId());
                conversationMemberDAO.insert(conn, conversation.getConversationId(), ownerAccountId);
                conn.commit();
                return inserted;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (DuplicateConversationException e) {
                // Unreachable in practice (brand-new team_id can never
                // collide with UQ_Conversations_team) — treated as a DB
                // error rather than a business-rule failure, since it can
                // only mean the invariant was somehow already violated.
                conn.rollback();
                throw new SQLException("Failed to create Team Conversation", e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Excludes DELETED teams — a soft-deleted Team is NotFound from every caller's perspective (mirrors LFGService#getLfg). */
    public Team getTeam(int teamId) throws TeamNotFoundException, SQLException {
        Team team = teamDAO.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Nhóm không tồn tại."));
        if (TeamStatus.DELETED.equals(team.getStatus())) {
            throw new TeamNotFoundException("Nhóm không tồn tại.");
        }
        return team;
    }

    /** Team Detail is member-only — never public (task spec §17 implies members-only viewing; no anonymous/non-member Team browsing was ever requested). */
    public List<TeamMember> listMembers(int teamId, int requesterAccountId)
            throws TeamNotFoundException, ForbiddenException, SQLException {
        getTeam(teamId);
        requireMembership(teamId, requesterAccountId);
        return teamMemberDAO.listMembers(teamId);
    }

    public List<TeamInvitation> listPendingInvitationsForTeam(int teamId, int requesterAccountId)
            throws TeamNotFoundException, ForbiddenException, SQLException {
        Team team = getTeam(teamId);
        requireOwner(team, requesterAccountId);
        return teamInvitationDAO.listPendingForTeam(teamId);
    }

    /**
     * Invite flow (task spec §3/§4/§9), in order: team exists, caller is
     * OWNER, target exists+ACTIVE, caller != target, caller and target are
     * FRIENDS (status ACCEPTED, either direction — reuses
     * {@link AccountFriendService#findActiveBetween} as-is, no duplicated
     * query), no Block between them (either direction, reuses
     * {@link AccountBlockService#isBlockedBetween}), target not already a
     * member, no existing PENDING invitation. A REJECTED/CANCELLED row for
     * this exact (team, invitee) pair is reused rather than duplicated
     * (same reasoning as AccountFriendService's re-friend logic).
     */
    public void inviteFriend(int teamId, int ownerAccountId, String ownerLabel, String targetUsername)
            throws TeamNotFoundException, ForbiddenException, ValidationException, AccountNotFoundException,
            DuplicateTeamInvitationException, SQLException {

        Team team = getTeam(teamId);
        requireOwner(team, ownerAccountId);

        Account target = accountService.getPublicProfile(targetUsername);
        int targetAccountId = target.getAccountId();
        if (targetAccountId == ownerAccountId) {
            throw new ValidationException("Bạn không thể tự mời chính mình.");
        }

        Optional<AccountFriendship> friendship = accountFriendService.findActiveBetween(ownerAccountId, targetAccountId);
        if (friendship.isEmpty() || !FriendshipStatus.ACCEPTED.equals(friendship.get().getStatus())) {
            throw new ValidationException("Chỉ có thể mời bạn bè (Friend) vào nhóm.");
        }
        if (accountBlockService.isBlockedBetween(ownerAccountId, targetAccountId)) {
            throw new ValidationException("Không thể mời người dùng này.");
        }
        if (teamMemberDAO.findMembership(teamId, targetAccountId).isPresent()) {
            throw new ValidationException("Người này đã là thành viên của nhóm.");
        }

        Optional<TeamInvitation> existingPair = teamInvitationDAO.findByPair(teamId, targetAccountId);
        if (existingPair.isPresent()) {
            String existingStatus = existingPair.get().getStatus();
            if (TeamInvitationStatus.PENDING.equals(existingStatus)) {
                throw new ValidationException("Đã có lời mời đang chờ phản hồi cho người này.");
            }
            // REJECTED/CANCELLED — reuse the row instead of a new INSERT (UQ_TeamInvitations_team_invitee).
            int updated = teamInvitationDAO.reactivateAsPending(existingPair.get().getInvitationId(), ownerAccountId);
            if (updated == 0) {
                throw new ValidationException("Không thể gửi lời mời lúc này, vui lòng thử lại.");
            }
        } else {
            teamInvitationDAO.insert(teamId, ownerAccountId, targetAccountId);
        }

        notificationService.notifyTeamInvite(targetAccountId, ownerAccountId, ownerLabel, team.getName());
    }

    /**
     * Accept: guarded UPDATE (task spec §10) then the TeamMembers INSERT, in
     * one transaction — never leaves an ACCEPTED invitation without a
     * matching TeamMembers row, or vice versa.
     */
    /**
     * Chat integration (additive): the invitee's ConversationMembers row is
     * inserted in this same transaction, into the Team's existing
     * Conversation — never leaves TeamMembers without a matching Chat
     * membership (Chat task spec §6). If the Team's Conversation is somehow
     * missing (only possible for a Team created before Chat existed, and
     * not yet self-healed via ChatService#openTeamChat), the whole Accept
     * fails and rolls back rather than silently admitting a member with no
     * Chat access.
     */
    public int acceptInvitation(int invitationId, int inviteeAccountId) throws TeamInvitationNotFoundException, SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int updated = teamInvitationDAO.acceptIfPending(conn, invitationId, inviteeAccountId);
                if (updated == 0) {
                    conn.rollback();
                    throw new TeamInvitationNotFoundException(
                            "Lời mời không tồn tại, không thuộc về bạn, hoặc đã được xử lý.");
                }
                TeamInvitation invitation = teamInvitationDAO.findById(conn, invitationId)
                        .orElseThrow(() -> new TeamInvitationNotFoundException("Lời mời không tồn tại."));
                teamMemberDAO.insert(conn, invitation.getTeamId(), inviteeAccountId, TeamMemberRole.MEMBER);

                Conversation conversation = conversationDAO.findByTeamId(conn, invitation.getTeamId())
                        .orElseThrow(() -> new SQLException(
                                "Team Conversation missing for team_id=" + invitation.getTeamId()));
                conversationMemberDAO.insert(conn, conversation.getConversationId(), inviteeAccountId);

                conn.commit();
                return invitation.getTeamId();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (TeamInvitationNotFoundException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        // No Notification on accept — not requested (task spec §20 only defines TEAM_INVITE).
    }

    public void rejectInvitation(int invitationId, int inviteeAccountId) throws TeamInvitationNotFoundException, SQLException {
        int updated = teamInvitationDAO.rejectIfPending(invitationId, inviteeAccountId);
        if (updated == 0) {
            throw new TeamInvitationNotFoundException("Lời mời không tồn tại, không thuộc về bạn, hoặc đã được xử lý.");
        }
        // No notification on reject (task spec §11).
    }

    public void cancelInvitation(int invitationId, int inviterAccountId) throws TeamInvitationNotFoundException, SQLException {
        int updated = teamInvitationDAO.cancelIfPending(invitationId, inviterAccountId);
        if (updated == 0) {
            throw new TeamInvitationNotFoundException("Lời mời không tồn tại, không thuộc về bạn, hoặc đã được xử lý.");
        }
        // No notification on cancel (task spec §12).
    }

    /**
     * The OWNER cannot Leave through this path (task spec §13 decision —
     * see final report's Important Decisions): they must Transfer Ownership
     * or Delete the Team first. Checked explicitly here (not just via the
     * DAO's role=MEMBER guard) so the error message can tell the Owner what
     * to do instead of a generic "not a member" message.
     */
    public void leaveTeam(int teamId, int accountId) throws TeamNotFoundException, ValidationException, SQLException {
        Team team = getTeam(teamId);
        if (team.getOwnerAccountId() == accountId) {
            throw new ValidationException("Chủ nhóm phải chuyển quyền sở hữu hoặc xóa nhóm trước khi rời nhóm.");
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int deleted = teamMemberDAO.deleteMemberIfRole(conn, teamId, accountId);
                if (deleted == 0) {
                    conn.rollback();
                    throw new ValidationException("Bạn không phải thành viên của nhóm này.");
                }
                removeConversationMembershipIfPresent(conn, teamId, accountId);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (ValidationException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Owner-only (task spec §14); the Owner can never remove themself through this path — use Delete/Transfer instead. */
    public void removeMember(int teamId, int ownerAccountId, int targetAccountId)
            throws TeamNotFoundException, ForbiddenException, ValidationException, SQLException {
        Team team = getTeam(teamId);
        requireOwner(team, ownerAccountId);
        if (targetAccountId == ownerAccountId) {
            throw new ValidationException("Không thể tự loại chính mình — hãy dùng chức năng Rời nhóm hoặc Xóa nhóm.");
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int deleted = teamMemberDAO.deleteMemberIfRole(conn, teamId, targetAccountId);
                if (deleted == 0) {
                    conn.rollback();
                    throw new ValidationException("Người này không phải thành viên của nhóm.");
                }
                removeConversationMembershipIfPresent(conn, teamId, targetAccountId);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (ValidationException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Chat integration (additive): removes the matching ConversationMembers
     * row for a Leave/Remove Member, in the same transaction as the
     * TeamMembers delete. If the Team's Conversation is missing (only
     * possible for a Team created before Chat existed and never opened
     * since — see ChatService#openTeamChat's self-heal), this is a silent
     * no-op rather than a failure: blocking someone's ability to Leave/be
     * Removed just because an unrelated legacy-data gap exists in Chat
     * metadata would be a disproportionate regression to existing Team
     * behavior, which this task must not break (Chat task spec §34).
     */
    private void removeConversationMembershipIfPresent(Connection conn, int teamId, int accountId) throws SQLException {
        Optional<Conversation> conversation = conversationDAO.findByTeamId(conn, teamId);
        if (conversation.isPresent()) {
            conversationMemberDAO.delete(conn, conversation.get().getConversationId(), accountId);
        }
    }

    /**
     * Chat integration (additive) — used by {@link com.gamenest.service.ChatService#openTeamChat}
     * to confirm ACTIVE + membership before it resolves/self-heals the
     * Team's Conversation. Read-only, no behavior change to any existing
     * Team method.
     */
    public boolean isMember(int teamId, int accountId) throws SQLException {
        return teamMemberDAO.findMembership(teamId, accountId).isPresent();
    }

    /**
     * Chat integration (additive) — used by {@link com.gamenest.service.ChatService#openTeamChat}
     * to backfill ConversationMembers for a legacy Team's current members
     * when self-healing a missing Team Conversation, reading within
     * ChatService's own transaction (the Connection is transaction-
     * agnostic at the DAO level). Read-only, no behavior change to any
     * existing Team method.
     */
    public List<TeamMember> listMembersForChatBackfill(Connection conn, int teamId) throws SQLException {
        return teamMemberDAO.listMembers(conn, teamId);
    }

    /**
     * Transfer Ownership (task spec §15): demote-then-promote in one
     * transaction, in that exact order, so UQ_TeamMembers_team_owner (at
     * most 1 OWNER row per team) is never violated mid-transaction, and
     * Teams.owner_account_id is updated in the same transaction so it never
     * drifts from TeamMembers.role. Any step returning 0 rows rolls back
     * the whole transfer — never left half-done.
     */
    public void transferOwnership(int teamId, int currentOwnerAccountId, int targetAccountId)
            throws TeamNotFoundException, ForbiddenException, ValidationException, SQLException {

        Team team = getTeam(teamId);
        requireOwner(team, currentOwnerAccountId);

        if (targetAccountId == currentOwnerAccountId) {
            throw new ValidationException("Bạn đã là chủ nhóm.");
        }
        Optional<TeamMember> targetMembership = teamMemberDAO.findMembership(teamId, targetAccountId);
        if (targetMembership.isEmpty() || !TeamMemberRole.MEMBER.equals(targetMembership.get().getRole())) {
            throw new ValidationException("Người này chưa phải thành viên của nhóm.");
        }
        if (accountBlockService.isBlockedBetween(currentOwnerAccountId, targetAccountId)) {
            throw new ValidationException("Không thể chuyển quyền sở hữu cho người dùng này.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int demoted = teamMemberDAO.setMemberRole(conn, teamId, currentOwnerAccountId);
                if (demoted == 0) {
                    throw new SQLException("Owner role changed concurrently — aborting transfer.");
                }
                int promoted = teamMemberDAO.setOwnerRole(conn, teamId, targetAccountId);
                if (promoted == 0) {
                    throw new SQLException("Target is no longer a member — aborting transfer.");
                }
                int teamUpdated = teamDAO.updateOwner(conn, teamId, currentOwnerAccountId, targetAccountId);
                if (teamUpdated == 0) {
                    throw new SQLException("Team owner changed concurrently — aborting transfer.");
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Owner-only, soft delete (task spec §16 decision — see final report). TeamMembers/TeamInvitations rows are left as historical data, just no longer reachable through an ACTIVE team. */
    public void deleteTeam(int teamId, int ownerAccountId) throws TeamNotFoundException, ForbiddenException, SQLException {
        Team team = getTeam(teamId);
        requireOwner(team, ownerAccountId);
        teamDAO.softDelete(teamId);
    }

    public List<Team> listMyTeams(int accountId, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return teamDAO.listMyTeams(accountId, offset, PAGE_SIZE);
    }

    public int countMyTeams(int accountId) throws SQLException {
        return teamDAO.countMyTeams(accountId);
    }

    public List<TeamInvitation> listIncomingInvitations(int accountId, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return teamInvitationDAO.listIncomingInvitations(accountId, offset, PAGE_SIZE);
    }

    public int countIncomingInvitations(int accountId) throws SQLException {
        return teamInvitationDAO.countIncomingInvitations(accountId);
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    private void requireOwner(Team team, int requesterAccountId) throws ForbiddenException {
        if (team.getOwnerAccountId() != requesterAccountId) {
            throw new ForbiddenException("Chỉ chủ nhóm mới có thể thực hiện thao tác này.");
        }
    }

    private void requireMembership(int teamId, int accountId) throws ForbiddenException, SQLException {
        if (teamMemberDAO.findMembership(teamId, accountId).isEmpty()) {
            throw new ForbiddenException("Bạn không phải thành viên của nhóm này.");
        }
    }

    private void validateName(String name) throws ValidationException {
        if (name == null || name.isEmpty()) {
            throw new ValidationException("Tên nhóm không được để trống.");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new ValidationException("Tên nhóm không được vượt quá " + NAME_MAX_LENGTH + " ký tự.");
        }
    }

    private void validateDescription(String description) throws ValidationException {
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new ValidationException("Mô tả không được vượt quá " + DESCRIPTION_MAX_LENGTH + " ký tự.");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }
}
