package com.gamenest.model;

import java.time.LocalDateTime;

public class LFGMember {

    private int lfgMemberId;
    private int lfgId;
    private int accountId;
    private LocalDateTime joinedAt;

    // Populated only by DAO queries that join Accounts for display — not
    // persisted columns on this entity.
    private String username;
    private String displayName;
    private String avatarUrl;

    public int getLfgMemberId() {
        return lfgMemberId;
    }

    public void setLfgMemberId(int lfgMemberId) {
        this.lfgMemberId = lfgMemberId;
    }

    public int getLfgId() {
        return lfgId;
    }

    public void setLfgId(int lfgId) {
        this.lfgId = lfgId;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
