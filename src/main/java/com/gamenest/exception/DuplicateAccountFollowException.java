package com.gamenest.exception;

/**
 * Thrown when follower_account_id already follows following_account_id —
 * mirrors UQ_AccountFollows_pair on AccountFollows.
 */
public class DuplicateAccountFollowException extends Exception {
    public DuplicateAccountFollowException(String message) {
        super(message);
    }
}
