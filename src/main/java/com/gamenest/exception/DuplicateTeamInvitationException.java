package com.gamenest.exception;

/**
 * Thrown when a PENDING/ACCEPTED-blocking unique-violation occurs on
 * (team_id, invitee_account_id) — mirrors UQ_TeamInvitations_team_invitee.
 * Only reachable as a race-condition backstop: TeamService already checks
 * for an existing row before inserting.
 */
public class DuplicateTeamInvitationException extends Exception {
    public DuplicateTeamInvitationException(String message) {
        super(message);
    }
}
