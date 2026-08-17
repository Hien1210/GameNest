package com.gamenest.model;

/**
 * Mirrors CK_Notifications_type in db/14_user_foundation.sql. Only types
 * mapped to a business event that already exists in the codebase — Follow/
 * Friend/Team/LFG notifications are out of scope until those modules exist.
 */
public final class NotificationType {

    public static final String ANSWER_ACCEPTED = "ANSWER_ACCEPTED";
    public static final String ANSWER_REPLY = "ANSWER_REPLY";
    public static final String REPORT_RESOLVED = "REPORT_RESOLVED";
    public static final String REPORT_REJECTED = "REPORT_REJECTED";
    public static final String SYSTEM = "SYSTEM";
    public static final String FOLLOW = "FOLLOW";
    public static final String FRIEND_REQUEST = "FRIEND_REQUEST";
    public static final String FRIEND_ACCEPTED = "FRIEND_ACCEPTED";
    public static final String TEAM_INVITE = "TEAM_INVITE";

    private NotificationType() {
    }
}
