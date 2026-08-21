package com.gamenest.model;

import java.util.List;

/**
 * Return value of {@code ChatService#toggleReaction} — everything the HTTP
 * servlet and the WebSocket endpoint need to react to a toggle, without
 * either of them re-querying the DB themselves. {@code myReaction} here is
 * always the ACTING account's own resulting reaction (the emoji it now has,
 * or null if it just un-reacted); it is NOT the per-recipient value used in
 * the REACTION_UPDATED broadcast — each broadcast recipient's own
 * "myReaction" is resolved separately by the caller (see
 * ChatWebSocketEndpoint), since the acting account and a recipient may have
 * different current reactions on the same message.
 */
public class ReactionResult {

    private final int conversationId;
    private final int messageId;
    private final String myReaction;
    private final List<MessageReactionSummary> reactions;

    public ReactionResult(int conversationId, int messageId, String myReaction, List<MessageReactionSummary> reactions) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.myReaction = myReaction;
        this.reactions = reactions;
    }

    public int getConversationId() {
        return conversationId;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getMyReaction() {
        return myReaction;
    }

    public List<MessageReactionSummary> getReactions() {
        return reactions;
    }
}
