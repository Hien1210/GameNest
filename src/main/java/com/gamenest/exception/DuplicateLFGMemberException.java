package com.gamenest.exception;

/**
 * Thrown when the account is already a member of the LFG group — mirrors
 * the UNIQUE(lfg_id, account_id) constraint on LFGMembers.
 */
public class DuplicateLFGMemberException extends Exception {
    public DuplicateLFGMemberException(String message) {
        super(message);
    }
}
