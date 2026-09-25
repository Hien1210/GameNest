package com.gamenest.model;

import java.time.LocalDateTime;

public class AccountFriendship {

    private int friendshipId;
    private int requesterAccountId;
    private int receiverAccountId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    // Populated only by DAO queries that join Accounts for the "other side"
    // of the relationship (list Friends / list incoming Requests) — not
    // persisted columns on this entity. Which account is "other" depends on
    // the query: for listFriends it's whichever side isn't the caller, for
    // listIncomingRequests it's always the requester.
    private String otherUsername;
    private String otherDisplayName;
    private String otherAvatarUrl;

    public int getFriendshipId() {
        return friendshipId;
    }

    public void setFriendshipId(int friendshipId) {
        this.friendshipId = friendshipId;
    }

    public int getRequesterAccountId() {
        return requesterAccountId;
    }

    public void setRequesterAccountId(int requesterAccountId) {
        this.requesterAccountId = requesterAccountId;
    }

    public int getReceiverAccountId() {
        return receiverAccountId;
    }

    public void setReceiverAccountId(int receiverAccountId) {
        this.receiverAccountId = receiverAccountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getOtherUsername() {
        return otherUsername;
    }

    public void setOtherUsername(String otherUsername) {
        this.otherUsername = otherUsername;
    }

    public String getOtherDisplayName() {
        return otherDisplayName;
    }

    public void setOtherDisplayName(String otherDisplayName) {
        this.otherDisplayName = otherDisplayName;
    }

    public String getOtherAvatarUrl() {
        return otherAvatarUrl;
    }

    public void setOtherAvatarUrl(String otherAvatarUrl) {
        this.otherAvatarUrl = otherAvatarUrl;
    }
}
