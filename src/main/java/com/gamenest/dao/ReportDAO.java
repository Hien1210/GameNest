package com.gamenest.dao;

import com.gamenest.exception.DuplicatePendingReportException;
import com.gamenest.model.Report;
import com.gamenest.model.ReportStatus;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reports is append-only-ish historical data: INSERT + SELECT + a single
 * guarded status-transition UPDATE (task spec §3/§11). There is
 * intentionally no delete method here.
 */
public class ReportDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

    private static final String DETAIL_SELECT =
            "SELECT r.report_id, r.reporter_account_id, reporter.username AS reporter_username, "
                    + "r.target_id, r.target_type, r.reason, r.description, r.status, "
                    + "r.reviewed_by, reviewer.username AS reviewed_by_username, r.reviewed_at, "
                    + "r.resolution_note, r.created_at, r.updated_at "
                    + "FROM dbo.Reports r "
                    + "JOIN dbo.Accounts reporter ON reporter.account_id = r.reporter_account_id "
                    + "LEFT JOIN dbo.Accounts reviewer ON reviewer.account_id = r.reviewed_by ";

    public Report insert(Report report) throws SQLException, DuplicatePendingReportException {
        String sql = "INSERT INTO dbo.Reports (reporter_account_id, target_id, target_type, reason, description, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, report.getReporterAccountId());
            ps.setInt(2, report.getTargetId());
            ps.setString(3, report.getTargetType());
            ps.setString(4, report.getReason());
            ps.setString(5, report.getDescription());
            ps.setString(6, report.getStatus());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    report.setReportId(keys.getInt(1));
                }
            }
            return report;

        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                throw new DuplicatePendingReportException("Bạn đã có một báo cáo đang chờ xử lý cho đối tượng này.");
            }
            throw e;
        }
    }

    public Optional<Report> findById(int reportId) throws SQLException {
        String sql = DETAIL_SELECT + "WHERE r.report_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Anti-spam pre-check (task spec §4): is there already a PENDING report
     * from this reporter for this exact target? The DB's filtered unique
     * index (UQ_Reports_pending_target) is the final safety net against a
     * concurrent race past this check.
     */
    public Optional<Report> findPendingByReporterAndTarget(int reporterAccountId, String targetType, int targetId)
            throws SQLException {
        String sql = DETAIL_SELECT
                + "WHERE r.reporter_account_id = ? AND r.target_type = ? AND r.target_id = ? AND r.status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reporterAccountId);
            ps.setString(2, targetType);
            ps.setInt(3, targetId);
            ps.setString(4, ReportStatus.PENDING);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    public List<Report> search(String status, String reason, String targetType, String reporterUsername,
                                LocalDateTime from, LocalDateTime toExclusive, int offset, int limit)
            throws SQLException {

        StringBuilder sql = new StringBuilder(DETAIL_SELECT).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, status, reason, targetType, reporterUsername, from, toExclusive);
        sql.append("ORDER BY r.created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            return mapList(ps);
        }
    }

    public int count(String status, String reason, String targetType, String reporterUsername,
                      LocalDateTime from, LocalDateTime toExclusive) throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dbo.Reports r "
                + "JOIN dbo.Accounts reporter ON reporter.account_id = r.reporter_account_id WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, status, reason, targetType, reporterUsername, from, toExclusive);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Transitions a report from PENDING to RESOLVED/REJECTED. The
     * {@code WHERE status = 'PENDING'} guard enforces the one-way state
     * machine (task spec §11) atomically at the DB level: a report that is
     * already RESOLVED/REJECTED, or does not exist, updates 0 rows.
     */
    public int updateResolution(int reportId, String newStatus, int reviewedByAccountId, String resolutionNote)
            throws SQLException {
        String sql = "UPDATE dbo.Reports SET status = ?, reviewed_by = ?, reviewed_at = SYSUTCDATETIME(), "
                + "resolution_note = ?, updated_at = SYSUTCDATETIME() WHERE report_id = ? AND status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, reviewedByAccountId);
            ps.setString(3, resolutionNote);
            ps.setInt(4, reportId);
            ps.setString(5, ReportStatus.PENDING);
            return ps.executeUpdate();
        }
    }

    private void appendFilters(StringBuilder sql, List<Object> params, String status, String reason,
                                String targetType, String reporterUsername, LocalDateTime from, LocalDateTime toExclusive) {
        if (status != null && !status.isEmpty()) {
            sql.append("AND r.status = ? ");
            params.add(status);
        }
        if (reason != null && !reason.isEmpty()) {
            sql.append("AND r.reason = ? ");
            params.add(reason);
        }
        if (targetType != null && !targetType.isEmpty()) {
            sql.append("AND r.target_type = ? ");
            params.add(targetType);
        }
        if (reporterUsername != null && !reporterUsername.isEmpty()) {
            sql.append("AND reporter.username LIKE ? ESCAPE '\\' ");
            params.add(likePattern(reporterUsername));
        }
        if (from != null) {
            sql.append("AND r.created_at >= ? ");
            params.add(Timestamp.valueOf(from));
        }
        if (toExclusive != null) {
            sql.append("AND r.created_at < ? ");
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

    private List<Report> mapList(PreparedStatement ps) throws SQLException {
        List<Report> reports = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reports.add(mapRow(rs));
            }
        }
        return reports;
    }

    private Report mapRow(ResultSet rs) throws SQLException {
        Report report = new Report();
        report.setReportId(rs.getInt("report_id"));
        report.setReporterAccountId(rs.getInt("reporter_account_id"));
        report.setReporterUsername(rs.getString("reporter_username"));
        report.setTargetId(rs.getInt("target_id"));
        report.setTargetType(rs.getString("target_type"));
        report.setReason(rs.getString("reason"));
        report.setDescription(rs.getString("description"));
        report.setStatus(rs.getString("status"));

        int reviewedBy = rs.getInt("reviewed_by");
        report.setReviewedBy(rs.wasNull() ? null : reviewedBy);
        report.setReviewedByUsername(rs.getString("reviewed_by_username"));
        report.setReviewedAt(rs.getObject("reviewed_at", LocalDateTime.class));
        report.setResolutionNote(rs.getString("resolution_note"));
        report.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        report.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return report;
    }
}
