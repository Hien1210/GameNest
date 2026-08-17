package com.gamenest.model;

import java.time.LocalDateTime;

public class Conversation {

    private int conversationId;
    private String type;
    private Integer teamId;
    private String directKey;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Populated only by DAO queries that join Teams/Accounts/Messages for
    // display (Chat List) — not persisted columns on this entity.
    private String teamName;
    private String otherUsername;
    private String otherDisplayName;
    private String otherAvatarUrl;
    private String latestMessageContent;
    private LocalDateTime latestMessageCreatedAt;
    private Integer latestMessageSenderId;
    private boolean latestMessageDeleted;
    private int unreadCount;

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public String getDirectKey() {
        return directKey;
    }

    public void setDirectKey(String directKey) {
        this.directKey = directKey;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
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

    public String getLatestMessageContent() {
        return latestMessageContent;
    }

    public void setLatestMessageContent(String latestMessageContent) {
        this.latestMessageContent = latestMessageContent;
    }

    public LocalDateTime getLatestMessageCreatedAt() {
        return latestMessageCreatedAt;
    }

    public void setLatestMessageCreatedAt(LocalDateTime latestMessageCreatedAt) {
        this.latestMessageCreatedAt = latestMessageCreatedAt;
    }

    public Integer getLatestMessageSenderId() {
        return latestMessageSenderId;
    }

    public void setLatestMessageSenderId(Integer latestMessageSenderId) {
        this.latestMessageSenderId = latestMessageSenderId;
    }

    public boolean isLatestMessageDeleted() {
        return latestMessageDeleted;
    }

    public void setLatestMessageDeleted(boolean latestMessageDeleted) {
        this.latestMessageDeleted = latestMessageDeleted;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
