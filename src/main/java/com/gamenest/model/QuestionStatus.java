package com.gamenest.model;

/**
 * Mirrors the CK_Questions_status CHECK constraint in the database.
 */
public final class QuestionStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String HIDDEN = "HIDDEN";
    public static final String DELETED = "DELETED";
    public static final String LOCKED = "LOCKED";

    private QuestionStatus() {
    }
}
