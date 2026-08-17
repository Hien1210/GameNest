package com.gamenest.dao;

import com.gamenest.model.Notification;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Every read/update here is scoped by recipient_account_id — a User must
 * never be able to see or mark another account's Notification (task spec
 * §5/§21). Client input never selects the ORDER BY or WHERE beyond what the
 * Service layer passes in.
 */
public class NotificationDAO {

    private static final String SELECT_COLUMNS =
            "SELECT notification_id, recipient_account_id, type, title, message, target_id, target_type, "
                    + "is_read, created_at FROM dbo.Notifications ";

    public Notification insert(Notification notification) throws SQLException {
        String sql = "INSERT INTO dbo.Notifications (recipient_account_id, type, title, message, target_id, target_type) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, notification.getRecipientAccountId());
            ps.setString(2, notification.getType());
            ps.setString(3, notification.getTitle());
            ps.setString(4, notification.getMessage());
            if (notification.getTargetId() != null) {
                ps.setInt(5, notification.getTargetId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, notification.getTargetType());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    notification.setNotificationId(keys.getInt(1));
                }
            }
            return notification;
        }
    }

    /**
     * DB-side filtered + paginated, newest first (task spec §7/§9). No
     * client input reaches ORDER BY — {@code unreadOnly} only toggles a
     * fixed {@code is_read = 0} predicate.
     */
    public List<Notification> findByRecipient(int accountId, boolean unreadOnly, int offset, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS).append("WHERE recipient_account_id = ? ");
        if (unreadOnly) {
            sql.append("AND is_read = 0 ");
        }
        sql.append("ORDER BY created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, accountId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            List<Notification> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        }
    }

    public int countByRecipient(int accountId, boolean unreadOnly) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dbo.Notifications WHERE recipient_account_id = ? ");
        if (unreadOnly) {
            sql.append("AND is_read = 0 ");
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * WHERE notification_id = ? AND recipient_account_id = ? — a User can
     * never mark another account's Notification as read (task spec §7/§11).
     */
    public int markAsRead(int notificationId, int accountId) throws SQLException {
        String sql = "UPDATE dbo.Notifications SET is_read = 1 "
                + "WHERE notification_id = ? AND recipient_account_id = ? AND is_read = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        }
    }

    /** WHERE recipient_account_id = ? AND is_read = 0 only — never a full-table UPDATE (task spec §12). */
    public int markAllAsRead(int accountId) throws SQLException {
        String sql = "UPDATE dbo.Notifications SET is_read = 1 WHERE recipient_account_id = ? AND is_read = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            return ps.executeUpdate();
        }
    }

    /** Used by the click-to-open flow to resolve target_type/target_id, scoped to the owning account. */
    public Optional<Notification> findByIdForAccount(int notificationId, int accountId) throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE notification_id = ? AND recipient_account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getInt("notification_id"));
        n.setRecipientAccountId(rs.getInt("recipient_account_id"));
        n.setType(rs.getString("type"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        int targetId = rs.getInt("target_id");
        n.setTargetId(rs.wasNull() ? null : targetId);
        n.setTargetType(rs.getString("target_type"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return n;
    }
}
