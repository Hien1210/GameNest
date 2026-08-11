package com.gamenest.dto;

import java.io.Serializable;

/**
 * Holds a not-yet-persisted registration, kept only in the HTTP session
 * between the "submit form" step and the "confirm OTP" step. Never written
 * to the database until the OTP is verified.
 */
public class PendingRegistration implements Serializable {

    private final String username;
    private final String email;
    private final String passwordHash;
    private final String displayName;

    public PendingRegistration(String username, String email, String passwordHash, String displayName) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }
}
