package com.gamenest.model;

/**
 * One aggregated row of {@code emoji -> count} for a single message —
 * display-only, never persisted itself (backed by GROUP BY over
 * MessageReactions, see MessageReactionDAO). Not the per-account reaction
 * row; see {@code Message#getMyReaction()} for the caller's own reaction.
 */
public class MessageReactionSummary {

    private final String emoji;
    private final int count;

    public MessageReactionSummary(String emoji, int count) {
        this.emoji = emoji;
        this.count = count;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getCount() {
        return count;
    }
}
