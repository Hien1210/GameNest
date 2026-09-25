package com.gamenest.model;

/**
 * Mirrors the CK_Reports_target_type CHECK constraint in the database.
 * Reports do not support Game targets in this phase.
 */
public final class ReportTargetType {

    public static final String ACCOUNT = "ACCOUNT";
    public static final String QUESTION = "QUESTION";
    public static final String ANSWER = "ANSWER";

    private ReportTargetType() {
    }
}
