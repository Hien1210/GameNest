package com.gamenest.model;

/**
 * Mirrors the CK_Answers_status CHECK constraint in the database.
 */
public final class AnswerStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String HIDDEN = "HIDDEN";
    public static final String DELETED = "DELETED";

    private AnswerStatus() {
    }
}
