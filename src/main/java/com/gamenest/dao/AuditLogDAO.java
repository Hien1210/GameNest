package com.gamenest.dao;

import com.gamenest.model.AuditLog;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditLogs is append-only historical data (CLAUDE.md §7.4, task spec §10):
 * this DAO intentionally exposes only insert + read methods — there is no
 * update/delete here, and the application's database login is DENY DELETE
 * at the schema level as a second layer of defense.
 */
public class AuditLogDAO {

    private static final String SELECT_COLUMNS =
            "audit_log_id, account_id, username, role_name, module, action, "
                    + "target_id, target_type, description, ip_address, user_agent, created_at ";

    public void insert(AuditLog log) throws SQLException {
        String sql = "INSERT INTO dbo.AuditLogs "
                + "(account_id, username, role_name, module, action, target_id, target_type, "
                + "description, ip_address, user_agent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (log.getAccountId() != null) {
                ps.setInt(1, log.getAccountId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, log.getUsername());
            ps.setString(3, log.getRoleName());
            ps.setString(4, log.getModule());
            ps.setString(5, log.getAction());
            if (log.getTargetId() != null) {
                ps.setInt(6, log.getTargetId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setString(7, log.getTargetType());
            ps.setString(8, log.getDescription());
            ps.setString(9, log.getIpAddress());
            ps.setString(10, log.getUserAgent());

            ps.executeUpdate();
        }
    }

    /**
     * DB-side filtered + paginated search, newest first. Any filter param
     * left {@code null}/blank is simply omitted from the WHERE clause. All
     * values are bound via PreparedStatement placeholders regardless of
     * source.
     */
    public List<AuditLog> search(String module, String action, String username, String targetType,
                                  LocalDateTime from, LocalDateTime toExclusive, int offset, int limit)
            throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT ").append(SELECT_COLUMNS)
                .append("FROM dbo.AuditLogs WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, module, action, username, targetType, from, toExclusive);
        sql.append("ORDER BY created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            return mapList(ps);
        }
    }

    public int count(String module, String action, String username, String targetType,
                      LocalDateTime from, LocalDateTime toExclusive) throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dbo.AuditLogs WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, module, action, username, targetType, from, toExclusive);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void appendFilters(StringBuilder sql, List<Object> params, String module, String action,
                                String username, String targetType, LocalDateTime from, LocalDateTime toExclusive) {
        if (module != null && !module.isEmpty()) {
            sql.append("AND module = ? ");
            params.add(module);
        }
        if (action != null && !action.isEmpty()) {
            sql.append("AND action = ? ");
            params.add(action);
        }
        if (username != null && !username.isEmpty()) {
            sql.append("AND username LIKE ? ESCAPE '\\' ");
            params.add(likePattern(username));
        }
        if (targetType != null && !targetType.isEmpty()) {
            sql.append("AND target_type = ? ");
            params.add(targetType);
        }
        if (from != null) {
            sql.append("AND created_at >= ? ");
            params.add(Timestamp.valueOf(from));
        }
        if (toExclusive != null) {
            sql.append("AND created_at < ? ");
            params.add(Timestamp.valueOf(toExclusive));
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private String likePattern(String query) {
        String escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private List<AuditLog> mapList(PreparedStatement ps) throws SQLException {
        List<AuditLog> logs = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                logs.add(mapRow(rs));
            }
        }
        return logs;
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setAuditLogId(rs.getInt("audit_log_id"));

        int accountId = rs.getInt("account_id");
        log.setAccountId(rs.wasNull() ? null : accountId);

        log.setUsername(rs.getString("username"));
        log.setRoleName(rs.getString("role_name"));
        log.setModule(rs.getString("module"));
        log.setAction(rs.getString("action"));

        int targetId = rs.getInt("target_id");
        log.setTargetId(rs.wasNull() ? null : targetId);

        log.setTargetType(rs.getString("target_type"));
        log.setDescription(rs.getString("description"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setUserAgent(rs.getString("user_agent"));
        log.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return log;
    }
}
