package com.gamenest.exception;

/**
 * Thrown when a guarded status-transition UPDATE on AccountFriendships
 * affects 0 rows — covers "friendship_id doesn't exist", "doesn't belong to
 * the caller", and "already in a different state" under one message,
 * mirroring how ReportNotFoundException/LFGNotFoundException already
 * collapse those cases for their own guarded UPDATEs. Reused for Accept,
 * Reject, Cancel, and Unfriend rather than creating a separate exception
 * type per action.
 */
public class FriendRequestNotFoundException extends Exception {
    public FriendRequestNotFoundException(String message) {
        super(message);
    }
}
