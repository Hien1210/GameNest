package com.gamenest.exception;

/**
 * Thrown when a Report does not exist, or (for review operations) exists
 * but is no longer PENDING — the DAO's UPDATE ... WHERE status = 'PENDING'
 * guard cannot distinguish the two, and neither case should let an admin
 * silently overwrite a resolution.
 */
public class ReportNotFoundException extends Exception {
    public ReportNotFoundException(String message) {
        super(message);
    }
}
