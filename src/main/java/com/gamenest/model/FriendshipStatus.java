package com.gamenest.model;

/** Mirrors CK_AccountFriendships_status in db/17_account_friendships.sql. */
public final class FriendshipStatus {

    public static final String PENDING = "PENDING";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String UNFRIENDED = "UNFRIENDED";

    private FriendshipStatus() {
    }
}
