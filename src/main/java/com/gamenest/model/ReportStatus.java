package com.gamenest.model;

/**
 * Mirrors the CK_Reports_status CHECK constraint in the database. There is
 * no DELETED status — Reports are never hard-deleted or soft-deleted, only
 * transitioned PENDING -> RESOLVED / REJECTED (one-way, enforced at the
 * DAO's UPDATE ... WHERE status = 'PENDING' guard).
 */
public final class ReportStatus {

    public static final String PENDING = "PENDING";
    public static final String RESOLVED = "RESOLVED";
    public static final String REJECTED = "REJECTED";

    private ReportStatus() {
    }
}
