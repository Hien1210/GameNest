package com.gamenest.exception;

/** Thrown when a conversation_id doesn't resolve to an ACTIVE Conversations row — mirrors TeamNotFoundException/LFGNotFoundException. */
public class ConversationNotFoundException extends Exception {
    public ConversationNotFoundException(String message) {
        super(message);
    }
}
