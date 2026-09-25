package com.gamenest.model;

/**
 * Mirrors the CK_OtpVerifications_purpose CHECK constraint in the database.
 */
public final class OtpPurpose {

    public static final String REGISTER = "REGISTER";
    public static final String RESET_PASSWORD = "RESET_PASSWORD";
    public static final String CHANGE_EMAIL = "CHANGE_EMAIL";

    private OtpPurpose() {
    }
}
