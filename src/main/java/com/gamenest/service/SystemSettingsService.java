package com.gamenest.service;

import com.gamenest.dao.SystemSettingsDAO;
import com.gamenest.dto.SettingChange;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.SystemSetting;
import com.gamenest.model.SystemSettingKey;
import com.gamenest.model.SystemSettingType;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Business rules for System Settings (task spec §6). Read helpers
 * ({@link #getStringSetting}, {@link #getIntSetting}, {@link #getBooleanSetting})
 * are safe to call from anywhere in the app (e.g. OtpService) — on any
 * failure (missing row, DB error, bad format) they fall back to the
 * caller-supplied default rather than throwing, so a Settings problem can
 * never break an unrelated feature.
 */
public class SystemSettingsService {

    private static final Logger LOGGER = Logger.getLogger(SystemSettingsService.class.getName());
    private static final int STRING_VALUE_MAX_LENGTH = 500;

    private final SystemSettingsDAO settingsDAO;

    public SystemSettingsService() {
        this.settingsDAO = new SystemSettingsDAO();
    }

    public SystemSettingsService(SystemSettingsDAO settingsDAO) {
        this.settingsDAO = settingsDAO;
    }

    public List<SystemSetting> getAllSettings() throws SQLException {
        return settingsDAO.findAll();
    }

    public String getStringSetting(String key, String defaultValue) {
        try {
            return settingsDAO.findByKey(key).map(SystemSetting::getSettingValue).orElse(defaultValue);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to read system setting \"" + key + "\", using default", e);
            return defaultValue;
        }
    }

    public int getIntSetting(String key, int defaultValue) {
        String raw = getStringSetting(key, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        String raw = getStringSetting(key, null);
        if (raw == null) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(raw.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw.trim())) {
            return false;
        }
        return defaultValue;
    }

    /**
     * Validates and applies a batch of setting_key -> new value pairs.
     * Unknown keys (not present in the table) are silently ignored — the
     * table is pre-seeded and this method never creates a new setting_key.
     * Every submitted value is validated against its real setting_type
     * (read from the DB, not trusted from the caller) BEFORE anything is
     * written, so a single invalid field rejects the whole batch rather
     * than leaving settings partially updated. Actual UPDATEs run inside
     * one transaction (mirrors AnswerService#acceptAnswer), and only keys
     * whose value actually differs from the current one are written or
     * returned — the caller uses the result to write one Audit Log entry
     * per real change (task spec §10).
     */
    public List<SettingChange> updateSettings(Map<String, String> newValues, int adminAccountId)
            throws ValidationException, SQLException {

        List<SystemSetting> targets = new ArrayList<>();
        for (String key : newValues.keySet()) {
            settingsDAO.findByKey(key).ifPresent(targets::add);
        }

        Map<String, String> toApply = new LinkedHashMap<>();
        for (SystemSetting setting : targets) {
            String newValue = newValues.get(setting.getSettingKey());
            newValue = newValue == null ? "" : newValue.trim();
            validateValue(setting, newValue);
            if (!newValue.equals(setting.getSettingValue())) {
                toApply.put(setting.getSettingKey(), newValue);
            }
        }

        if (toApply.isEmpty()) {
            return List.of();
        }

        List<SettingChange> changes = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (SystemSetting setting : targets) {
                    String newValue = toApply.get(setting.getSettingKey());
                    if (newValue == null) {
                        continue;
                    }
                    int updated = settingsDAO.updateValue(conn, setting.getSettingKey(), newValue, adminAccountId);
                    if (updated > 0) {
                        changes.add(new SettingChange(setting.getSettingId(), setting.getSettingKey(),
                                setting.getSettingValue(), newValue));
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return changes;
    }

    private void validateValue(SystemSetting setting, String newValue) throws ValidationException {
        switch (setting.getSettingType()) {
            case SystemSettingType.BOOLEAN -> {
                if (!"true".equals(newValue) && !"false".equals(newValue)) {
                    throw new ValidationException(
                            "Giá trị của \"" + setting.getSettingKey() + "\" phải là true hoặc false.");
                }
            }
            case SystemSettingType.INTEGER -> {
                int value;
                try {
                    value = Integer.parseInt(newValue);
                } catch (NumberFormatException e) {
                    throw new ValidationException(
                            "Giá trị của \"" + setting.getSettingKey() + "\" phải là số nguyên dương.");
                }
                if (value <= 0) {
                    throw new ValidationException(
                            "Giá trị của \"" + setting.getSettingKey() + "\" phải là số nguyên dương.");
                }
            }
            case SystemSettingType.ENUM -> validateEnumValue(setting.getSettingKey(), newValue);
            default -> validateStringValue(setting.getSettingKey(), newValue);
        }
    }

    private void validateStringValue(String key, String value) throws ValidationException {
        if (SystemSettingKey.SITE_NAME.equals(key) && value.isEmpty()) {
            throw new ValidationException("Tên hệ thống không được để trống.");
        }
        if (value.length() > STRING_VALUE_MAX_LENGTH) {
            throw new ValidationException(
                    "Giá trị của \"" + key + "\" không được vượt quá " + STRING_VALUE_MAX_LENGTH + " ký tự.");
        }
    }

    private void validateEnumValue(String key, String value) throws ValidationException {
        if (SystemSettingKey.SYSTEM_STATUS.equals(key)
                && !"ONLINE".equals(value) && !"MAINTENANCE".equals(value)) {
            throw new ValidationException("Trạng thái hệ thống không hợp lệ.");
        }
    }
}
