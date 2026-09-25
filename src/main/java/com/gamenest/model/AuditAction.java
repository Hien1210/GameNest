package com.gamenest.model;

/**
 * Mirrors the CK_AuditLogs_action CHECK constraint in the database. Only
 * actions that actually exist in Admin Accounts/Admin Games/Admin Reports
 * are listed — there is no DELETE action since none of those modules
 * support hard delete.
 */
public final class AuditAction {

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String STATUS_CHANGE = "STATUS_CHANGE";
    public static final String RESOLVE = "RESOLVE";
    public static final String REJECT = "REJECT";

    private AuditAction() {
    }
}
