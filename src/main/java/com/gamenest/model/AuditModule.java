package com.gamenest.model;

/**
 * Mirrors the CK_AuditLogs_module CHECK constraint in the database. Only
 * modules with actual Admin/Moderator integrations belong here — Audit Log
 * currently covers Accounts, Games, Reports, Settings, Questions and
 * Answers.
 */
public final class AuditModule {

    public static final String ACCOUNTS = "ACCOUNTS";
    public static final String GAMES = "GAMES";
    public static final String REPORTS = "REPORTS";
    public static final String SETTINGS = "SETTINGS";
    public static final String QUESTIONS = "QUESTIONS";
    public static final String ANSWERS = "ANSWERS";

    private AuditModule() {
    }
}
