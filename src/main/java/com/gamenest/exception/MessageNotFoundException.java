package com.gamenest.exception;

/**
 * Thrown when a guarded status-transition UPDATE on Messages (edit/delete)
 * affects 0 rows — covers "message_id doesn't exist", "doesn't belong to
 * the caller", and "already deleted" under one message, mirroring
 * FriendRequestNotFoundException/TeamInvitationNotFoundException.
 */
public class MessageNotFoundException extends Exception {
    public MessageNotFoundException(String message) {
        super(message);
    }
}
