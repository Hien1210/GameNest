package com.gamenest.model;

import java.time.LocalDateTime;

public class AccountFollow {

    private int followId;
    private int followerAccountId;
    private int followingAccountId;
    private LocalDateTime createdAt;

    public int getFollowId() {
        return followId;
    }

    public void setFollowId(int followId) {
        this.followId = followId;
    }

    public int getFollowerAccountId() {
        return followerAccountId;
    }

    public void setFollowerAccountId(int followerAccountId) {
        this.followerAccountId = followerAccountId;
    }

    public int getFollowingAccountId() {
        return followingAccountId;
    }

    public void setFollowingAccountId(int followingAccountId) {
        this.followingAccountId = followingAccountId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
