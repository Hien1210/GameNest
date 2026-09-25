package com.gamenest.model;

/**
 * Mirrors CK_Conversations_status in db/20_chat.sql. Only ACTIVE exists in
 * this phase — a Team Conversation's real accessibility is derived from
 * Teams.status at access time (see ChatService), not a second status flag
 * here that could drift out of sync.
 */
public final class ConversationStatus {

    public static final String ACTIVE = "ACTIVE";

    private ConversationStatus() {
    }
}
