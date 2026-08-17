package com.gamenest.model;

/** Mirrors CK_Teams_status in db/19_teams.sql. Soft delete only — Team is user-generated content (CLAUDE.md §7.1). */
public final class TeamStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String DELETED = "DELETED";

    private TeamStatus() {
    }
}
