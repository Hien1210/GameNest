package com.gamenest.model;

import java.time.LocalDateTime;

public class AccountBlock {

    private int blockId;
    private int blockerAccountId;
    private int blockedAccountId;
    private LocalDateTime createdAt;

    // Populated only by DAO queries that join Accounts for display (Blocked
    // Users list) — not persisted columns on this entity.
    private String blockedUsername;
    private String blockedDisplayName;
    private String blockedAvatarUrl;

    public int getBlockId() {
        return blockId;
    }

    public void setBlockId(int blockId) {
        this.blockId = blockId;
    }

    public int getBlockerAccountId() {
        return blockerAccountId;
    }

    public void setBlockerAccountId(int blockerAccountId) {
        this.blockerAccountId = blockerAccountId;
    }

    public int getBlockedAccountId() {
        return blockedAccountId;
    }

    public void setBlockedAccountId(int blockedAccountId) {
        this.blockedAccountId = blockedAccountId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getBlockedUsername() {
        return blockedUsername;
    }

    public void setBlockedUsername(String blockedUsername) {
        this.blockedUsername = blockedUsername;
    }

    public String getBlockedDisplayName() {
        return blockedDisplayName;
    }

    public void setBlockedDisplayName(String blockedDisplayName) {
        this.blockedDisplayName = blockedDisplayName;
    }

    public String getBlockedAvatarUrl() {
        return blockedAvatarUrl;
    }

    public void setBlockedAvatarUrl(String blockedAvatarUrl) {
        this.blockedAvatarUrl = blockedAvatarUrl;
    }
}
