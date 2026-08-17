package com.gamenest.exception;

/**
 * Thrown when a guarded status-transition UPDATE on TeamInvitations affects
 * 0 rows — covers "invitation_id doesn't exist", "doesn't belong to the
 * caller", and "already in a different state" under one message, mirroring
 * FriendRequestNotFoundException. Reused for Accept, Reject, and Cancel.
 */
public class TeamInvitationNotFoundException extends Exception {
    public TeamInvitationNotFoundException(String message) {
        super(message);
    }
}
