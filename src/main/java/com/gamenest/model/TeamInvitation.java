package com.gamenest.model;

import java.time.LocalDateTime;

public class TeamInvitation {

    private int invitationId;
    private int teamId;
    private int inviterAccountId;
    private int inviteeAccountId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    // Populated only by DAO queries that join Teams/Accounts for display —
    // not persisted columns on this entity.
    private String teamName;
    private String inviterUsername;
    private String inviterDisplayName;
    private String inviteeUsername;
    private String inviteeDisplayName;
    private String inviteeAvatarUrl;

    public int getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(int invitationId) {
        this.invitationId = invitationId;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public int getInviterAccountId() {
        return inviterAccountId;
    }

    public void setInviterAccountId(int inviterAccountId) {
        this.inviterAccountId = inviterAccountId;
    }

    public int getInviteeAccountId() {
        return inviteeAccountId;
    }

    public void setInviteeAccountId(int inviteeAccountId) {
        this.inviteeAccountId = inviteeAccountId;
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

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getInviterUsername() {
        return inviterUsername;
    }

    public void setInviterUsername(String inviterUsername) {
        this.inviterUsername = inviterUsername;
    }

    public String getInviterDisplayName() {
        return inviterDisplayName;
    }

    public void setInviterDisplayName(String inviterDisplayName) {
        this.inviterDisplayName = inviterDisplayName;
    }

    public String getInviteeUsername() {
        return inviteeUsername;
    }

    public void setInviteeUsername(String inviteeUsername) {
        this.inviteeUsername = inviteeUsername;
    }

    public String getInviteeDisplayName() {
        return inviteeDisplayName;
    }

    public void setInviteeDisplayName(String inviteeDisplayName) {
        this.inviteeDisplayName = inviteeDisplayName;
    }

    public String getInviteeAvatarUrl() {
        return inviteeAvatarUrl;
    }

    public void setInviteeAvatarUrl(String inviteeAvatarUrl) {
        this.inviteeAvatarUrl = inviteeAvatarUrl;
    }
}
