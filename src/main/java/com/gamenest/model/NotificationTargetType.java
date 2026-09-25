package com.gamenest.model;

/**
 * Mirrors CK_Notifications_target_type in db/14_user_foundation.sql —
 * polymorphic reference, same pattern as {@link ReportTargetType}. No SQL FK
 * to any single table since target_id can point at different tables
 * depending on target_type.
 */
public final class NotificationTargetType {

    public static final String ACCOUNT = "ACCOUNT";
    public static final String QUESTION = "QUESTION";
    public static final String ANSWER = "ANSWER";
    public static final String REPORT = "REPORT";

    private NotificationTargetType() {
    }
}
