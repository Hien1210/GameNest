package com.gamenest.dto;

/**
 * One setting whose value actually changed during a
 * {@code SystemSettingsService.updateSettings} call — used by the caller to
 * write one Audit Log entry per changed key (task spec §10: no log entry
 * for a submitted value identical to the current one).
 */
public class SettingChange {

    private final int settingId;
    private final String key;
    private final String oldValue;
    private final String newValue;

    public SettingChange(int settingId, String key, String oldValue, String newValue) {
        this.settingId = settingId;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public int getSettingId() {
        return settingId;
    }

    public String getKey() {
        return key;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }
}
