package com.gamenest.controller.admin;

import com.gamenest.model.AuditLog;
import com.gamenest.service.AuditLogService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin Audit Log listing — read-only, DB-side filtered and paginated.
 * Access is gated by {@link com.gamenest.filter.AdminAuthorizationFilter}
 * on /admin/*. This page never mutates AuditLogs (append-only, task spec
 * §10/§11) — it only ever SELECTs.
 */
@WebServlet(name = "AdminAuditLogServlet", urlPatterns = {"/admin/audit-logs"})
public class AdminAuditLogServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminAuditLogServlet.class.getName());
    private static final String VIEW = "/admin/audit-logs/list.jsp";

    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int page = parsePage(request.getParameter("page"));
        String module = request.getParameter("module");
        String action = request.getParameter("action");
        String username = request.getParameter("username");
        String targetType = request.getParameter("targetType");
        LocalDate dateFrom = parseDate(request.getParameter("dateFrom"));
        LocalDate dateTo = parseDate(request.getParameter("dateTo"));

        try {
            List<AuditLog> logs = auditLogService.search(module, action, username, targetType,
                    dateFrom, dateTo, page);
            int totalCount = auditLogService.count(module, action, username, targetType, dateFrom, dateTo);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / auditLogService.getPageSize()));

            request.setAttribute("logs", logs);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("module", module);
            request.setAttribute("action", action);
            request.setAttribute("username", username);
            request.setAttribute("targetType", targetType);
            request.setAttribute("dateFrom", request.getParameter("dateFrom"));
            request.setAttribute("dateTo", request.getParameter("dateTo"));

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing audit logs", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private int parsePage(String raw) {
        try {
            return Math.max(Integer.parseInt(raw), 1);
        } catch (NumberFormatException | NullPointerException e) {
            return 1;
        }
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
