package com.gamenest.model;

/**
 * Mirrors the CK_LFGPosts_status CHECK constraint in the database
 * (db/01_core_schema.sql) — verified directly, not assumed.
 */
public final class LFGStatus {

    public static final String OPEN = "OPEN";
    public static final String FULL = "FULL";
    public static final String CLOSED = "CLOSED";
    public static final String EXPIRED = "EXPIRED";
    public static final String DELETED = "DELETED";

    private LFGStatus() {
    }
}
