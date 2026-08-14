package com.gamenest.model;

/**
 * Mirrors the CK_AuditLogs_module CHECK constraint in the database. Only
 * modules with actual Admin integrations belong here — Audit Log currently
 * covers Accounts and Games only.
 */
public final class AuditModule {

    public static final String ACCOUNTS = "ACCOUNTS";
    public static final String GAMES = "GAMES";

    private AuditModule() {
    }
}
