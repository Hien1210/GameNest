package com.gamenest.exception;

/**
 * Thrown when the account already has the given game under the same
 * relationship_type — mirrors the UNIQUE(account_id, game_id,
 * relationship_type) constraint on AccountGames.
 */
public class DuplicateAccountGameException extends Exception {
    public DuplicateAccountGameException(String message) {
        super(message);
    }
}
