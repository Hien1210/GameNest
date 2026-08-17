package com.gamenest.model;

import java.time.LocalDateTime;

public class ConversationMember {

    private int conversationMemberId;
    private int conversationId;
    private int accountId;
    private Integer lastReadMessageId;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    public int getConversationMemberId() {
        return conversationMemberId;
    }

    public void setConversationMemberId(int conversationMemberId) {
        this.conversationMemberId = conversationMemberId;
    }

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public Integer getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(Integer lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }
}
