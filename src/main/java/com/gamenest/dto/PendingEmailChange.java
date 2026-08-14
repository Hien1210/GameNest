package com.gamenest.dto;

import java.io.Serializable;

/**
 * Holds a not-yet-confirmed email change, kept only in the HTTP session
 * between "request OTP" and "confirm OTP". The account's email column is
 * never touched until the OTP is verified. accountId is carried alongside
 * newEmail so a stale pending change cannot be confirmed against a
 * different account than the one that requested it.
 */
public class PendingEmailChange implements Serializable {

    private final int accountId;
    private final String newEmail;

    public PendingEmailChange(int accountId, String newEmail) {
        this.accountId = accountId;
        this.newEmail = newEmail;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getNewEmail() {
        return newEmail;
    }
}
