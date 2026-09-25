package com.gamenest.exception;

/**
 * Thrown when a friend request for the same (requester, receiver) direction
 * already exists — mirrors UQ_AccountFriendships_pair. Only reachable as a
 * race-condition guard: AccountFriendService already checks for an existing
 * row before inserting, so this is the DB-level backstop for the rare case
 * of two concurrent sendRequest calls.
 */
public class DuplicateFriendRequestException extends Exception {
    public DuplicateFriendRequestException(String message) {
        super(message);
    }
}
