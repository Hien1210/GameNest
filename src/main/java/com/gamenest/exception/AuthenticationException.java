package com.gamenest.exception;

/**
 * Thrown for both "account not found" and "wrong password" so callers
 * cannot use error type to enumerate valid usernames/emails.
 */
public class AuthenticationException extends Exception {
    public AuthenticationException(String message) {
        super(message);
    }
}
