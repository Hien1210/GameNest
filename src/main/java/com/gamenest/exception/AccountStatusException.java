package com.gamenest.exception;

/**
 * Thrown when credentials are valid but the account status does not
 * permit login (BANNED, SUSPENDED, DELETED).
 */
public class AccountStatusException extends Exception {
    public AccountStatusException(String message) {
        super(message);
    }
}
