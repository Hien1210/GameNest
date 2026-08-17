package com.gamenest.exception;

/**
 * Thrown on a unique-violation for a Direct Conversation's direct_key or a
 * Team Conversation's team_id (filtered unique index) — mirrors
 * DuplicateFriendRequestException/DuplicateTeamInvitationException. Only
 * reachable as a race-condition backstop.
 */
public class DuplicateConversationException extends Exception {
    public DuplicateConversationException(String message) {
        super(message);
    }
}
