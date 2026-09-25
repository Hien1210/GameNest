package com.gamenest.dao;

import com.gamenest.model.SystemSetting;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SystemSettings is configuration data, not user content — the table is
 * pre-seeded by db/10_system_settings.sql and the application only ever
 * reads rows or updates their value/updated_by/updated_at. There is
 * intentionally no insert or delete method here (task spec §12).
 */
public class SystemSettingsDAO {

    private static final String SELECT_COLUMNS =
            "s.setting_id, s.setting_key, s.setting_value, s.setting_type, s.description, "
                    + "s.updated_by, u.username AS updated_by_username, s.updated_at ";
    private static final String BASE_SELECT =
            "SELECT " + SELECT_COLUMNS + "FROM dbo.SystemSettings s "
                    + "LEFT JOIN dbo.Accounts u ON u.account_id = s.updated_by ";

    public List<SystemSetting> findAll() throws SQLException {
        String sql = BASE_SELECT + "ORDER BY s.setting_key";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<SystemSetting> settings = new ArrayList<>();
            while (rs.next()) {
                settings.add(mapRow(rs));
            }
            return settings;
        }
    }

    public Optional<SystemSetting> findByKey(String settingKey) throws SQLException {
        String sql = BASE_SELECT + "WHERE s.setting_key = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, settingKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Runs on the caller-supplied Connection so multiple settings can be
     * updated in a single transaction (see
     * SystemSettingsService#updateSettings, mirroring
     * AnswerService#acceptAnswer's transaction pattern). Only updates a row
     * that already exists — this DAO never inserts a new setting_key.
     */
    public int updateValue(Connection conn, String settingKey, String newValue, int updatedByAccountId)
            throws SQLException {
        String sql = "UPDATE dbo.SystemSettings SET setting_value = ?, updated_by = ?, "
                + "updated_at = SYSUTCDATETIME() WHERE setting_key = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newValue);
            ps.setInt(2, updatedByAccountId);
            ps.setString(3, settingKey);
            return ps.executeUpdate();
        }
    }

    private SystemSetting mapRow(ResultSet rs) throws SQLException {
        SystemSetting setting = new SystemSetting();
        setting.setSettingId(rs.getInt("setting_id"));
        setting.setSettingKey(rs.getString("setting_key"));
        setting.setSettingValue(rs.getString("setting_value"));
        setting.setSettingType(rs.getString("setting_type"));
        setting.setDescription(rs.getString("description"));

        int updatedBy = rs.getInt("updated_by");
        setting.setUpdatedBy(rs.wasNull() ? null : updatedBy);
        setting.setUpdatedByUsername(rs.getString("updated_by_username"));
        setting.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return setting;
    }
}
