package com.gamenest.model;

/**
 * Mirrors the CK_AccountGames_relationship_type CHECK constraint in the
 * database (db/14_user_foundation.sql).
 */
public final class AccountGameRelationshipType {

    public static final String PLAYING = "PLAYING";
    public static final String FAVORITE = "FAVORITE";

    private AccountGameRelationshipType() {
    }
}
