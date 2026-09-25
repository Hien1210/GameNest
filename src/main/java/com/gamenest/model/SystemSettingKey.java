package com.gamenest.model;

import java.util.List;

/**
 * Known setting_key values pre-seeded by db/10_system_settings.sql. The
 * table is generic key/value, but the application only ever reads/writes
 * these known keys — unknown keys submitted from a tampered form are
 * silently ignored by SystemSettingsService.
 */
public final class SystemSettingKey {

    public static final String SITE_NAME = "site.name";
    public static final String SITE_DESCRIPTION = "site.description";
    public static final String SYSTEM_STATUS = "system.status";

    public static final String REGISTRATION_ENABLED = "registration.enabled";
    public static final String QUESTIONS_ENABLED = "questions.enabled";
    public static final String ANSWERS_ENABLED = "answers.enabled";
    public static final String REPORTS_ENABLED = "reports.enabled";

    public static final String OTP_EXPIRATION_MINUTES = "otp.expiration_minutes";
    public static final String OTP_RESEND_COOLDOWN_SECONDS = "otp.resend_cooldown_seconds";
    public static final String OTP_MAX_ATTEMPTS = "otp.max_attempts";

    public static final List<String> ALL_KEYS = List.of(
            SITE_NAME, SITE_DESCRIPTION, SYSTEM_STATUS,
            REGISTRATION_ENABLED, QUESTIONS_ENABLED, ANSWERS_ENABLED, REPORTS_ENABLED,
            OTP_EXPIRATION_MINUTES, OTP_RESEND_COOLDOWN_SECONDS, OTP_MAX_ATTEMPTS);

    private SystemSettingKey() {
    }
}
