package com.gamenest.model;

/**
 * Mirrors the CK_AuditLogs_action CHECK constraint in the database. Only
 * actions that actually exist in Admin Accounts/Admin Games are listed —
 * there is no DELETE action since neither module supports hard delete.
 */
public final class AuditAction {

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String STATUS_CHANGE = "STATUS_CHANGE";

    private AuditAction() {
    }
}
