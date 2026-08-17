package com.gamenest.exception;

/** Thrown when a team_id doesn't resolve to an ACTIVE Teams row — mirrors LFGNotFoundException/ReportNotFoundException. */
public class TeamNotFoundException extends Exception {
    public TeamNotFoundException(String message) {
        super(message);
    }
}
