package com.gamenest.exception;

/**
 * Thrown when an authenticated account attempts to modify content it does
 * not own (and is not ADMIN) — ownership/authorization must be checked
 * server-side, never inferred from the client.
 */
public class ForbiddenException extends Exception {
    public ForbiddenException(String message) {
        super(message);
    }
}
