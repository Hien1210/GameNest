package com.gamenest.model;

/** Mirrors CK_TeamInvitations_status in db/19_teams.sql. */
public final class TeamInvitationStatus {

    public static final String PENDING = "PENDING";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    private TeamInvitationStatus() {
    }
}
