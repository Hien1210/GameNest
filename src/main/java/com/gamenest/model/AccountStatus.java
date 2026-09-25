package com.gamenest.model;

/**
 * Mirrors the CK_Accounts_status CHECK constraint in the database.
 */
public final class AccountStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String BANNED = "BANNED";
    public static final String DELETED = "DELETED";
    public static final String SUSPENDED = "SUSPENDED";

    private AccountStatus() {
    }
}
