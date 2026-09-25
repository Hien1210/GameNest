package com.gamenest.model;

/**
 * Mirrors the CK_Reports_reason CHECK constraint in the database.
 */
public final class ReportReason {

    public static final String SPAM = "SPAM";
    public static final String HARASSMENT = "HARASSMENT";
    public static final String INAPPROPRIATE_CONTENT = "INAPPROPRIATE_CONTENT";
    public static final String HATE_SPEECH = "HATE_SPEECH";
    public static final String MISINFORMATION = "MISINFORMATION";
    public static final String CHEATING = "CHEATING";
    public static final String OTHER = "OTHER";

    private ReportReason() {
    }
}
