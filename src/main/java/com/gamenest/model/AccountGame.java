package com.gamenest.model;

import java.time.LocalDateTime;

public class AccountGame {

    private int accountGameId;
    private int accountId;
    private int gameId;
    private String relationshipType;
    private LocalDateTime createdAt;

    // Populated only by DAO queries that join Games for display — not
    // persisted columns on this entity.
    private String gameName;
    private String gameCoverImageUrl;
    private String gameStatus;

    public int getAccountGameId() {
        return accountGameId;
    }

    public void setAccountGameId(int accountGameId) {
        this.accountGameId = accountGameId;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getGameCoverImageUrl() {
        return gameCoverImageUrl;
    }

    public void setGameCoverImageUrl(String gameCoverImageUrl) {
        this.gameCoverImageUrl = gameCoverImageUrl;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(String gameStatus) {
        this.gameStatus = gameStatus;
    }
}
