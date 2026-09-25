package com.gamenest.service;

import com.gamenest.dao.AuditLogDAO;
import com.gamenest.model.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single facade for writing and reading Audit Log entries, so no controller
 * has to duplicate {@code request.getRemoteAddr()} / session reads / INSERT
 * logic itself (task spec §14). Actor identity always comes from the
 * authenticated session — never from a request parameter (task spec §15).
 */
public class AuditLogService {

    private static final Logger LOGGER = Logger.getLogger(AuditLogService.class.getName());
    private static final int PAGE_SIZE = 20;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    private final AuditLogDAO auditLogDAO;

    public AuditLogService() {
        this.auditLogDAO = new AuditLogDAO();
    }

    public AuditLogService(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    /**
     * Records one admin action. Must only be called AFTER the business
     * operation has already committed successfully (task spec §19/§20) —
     * callers pass just the action-specific Vietnamese fragment
     * (e.g. {@code "đã tạo Game \"Valorant\"."}); this method prepends the
     * actor so the description text and the username/role_name snapshot
     * columns can never drift apart.
     * <p>
     * If the insert itself fails, the failure is logged server-side and
     * swallowed — an audit-write failure must never roll back or fail the
     * business operation that already succeeded (task spec §21/§22).
     */
    public void log(HttpServletRequest request, String module, String action,
                     Integer targetId, String targetType, String actionDescription) {

        HttpSession session = request.getSession(false);
        Object accountIdAttr = session == null ? null : session.getAttribute("accountId");
        Object usernameAttr = session == null ? null : session.getAttribute("username");
        Object roleAttr = session == null ? null : session.getAttribute("role");

        String actorUsername = usernameAttr != null ? usernameAttr.toString() : "unknown";

        AuditLog log = new AuditLog();
        log.setAccountId(accountIdAttr instanceof Integer ? (Integer) accountIdAttr : null);
        log.setUsername(actorUsername);
        log.setRoleName(roleAttr != null ? roleAttr.toString() : "UNKNOWN");
        log.setModule(module);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setTargetType(targetType);
        log.setDescription(truncate("Admin \"" + actorUsername + "\" " + actionDescription));
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));

        try {
            auditLogDAO.insert(log);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to write audit log (business operation already succeeded): "
                    + "module=" + module + " action=" + action
                    + " targetType=" + targetType + " targetId=" + targetId, e);
        }
    }

    // ---- Admin UI (read) ----

    public List<AuditLog> search(String module, String action, String username, String targetType,
                                  LocalDate dateFrom, LocalDate dateTo, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        LocalDateTime from = dateFrom == null ? null : dateFrom.atStartOfDay();
        LocalDateTime toExclusive = dateTo == null ? null : dateTo.plusDays(1).atStartOfDay();
        return auditLogDAO.search(blankToNull(module), blankToNull(action), blankToNull(username),
                blankToNull(targetType), from, toExclusive, offset, PAGE_SIZE);
    }

    public int count(String module, String action, String username, String targetType,
                      LocalDate dateFrom, LocalDate dateTo) throws SQLException {
        LocalDateTime from = dateFrom == null ? null : dateFrom.atStartOfDay();
        LocalDateTime toExclusive = dateTo == null ? null : dateTo.plusDays(1).atStartOfDay();
        return auditLogDAO.count(blankToNull(module), blankToNull(action), blankToNull(username),
                blankToNull(targetType), from, toExclusive);
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value) {
        return value.length() > DESCRIPTION_MAX_LENGTH ? value.substring(0, DESCRIPTION_MAX_LENGTH) : value;
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }
}
