package com.gamenest.model;

/**
 * Mirrors the CK_AuditLogs_target_type CHECK constraint in the database.
 * Restricted to the modules integrated so far; extending to other targets
 * (QUESTION, ANSWER, LFG, ...) requires an additive migration later.
 */
public final class AuditTargetType {

    public static final String ACCOUNT = "ACCOUNT";
    public static final String GAME = "GAME";

    private AuditTargetType() {
    }
}
