package com.gamenest.controller.admin;

import com.gamenest.model.Report;
import com.gamenest.service.ReportService;

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
 * Admin Reports listing — read-only, DB-side filtered and paginated.
 * Access is gated by {@link com.gamenest.filter.AdminAuthorizationFilter}
 * on /admin/*.
 */
@WebServlet(name = "AdminReportsServlet", urlPatterns = {"/admin/reports"})
public class AdminReportsServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminReportsServlet.class.getName());
    private static final String VIEW = "/admin/reports/list.jsp";

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int page = parsePage(request.getParameter("page"));
        String status = request.getParameter("status");
        String reason = request.getParameter("reason");
        String targetType = request.getParameter("targetType");
        String reporterUsername = request.getParameter("reporterUsername");
        LocalDate dateFrom = parseDate(request.getParameter("dateFrom"));
        LocalDate dateTo = parseDate(request.getParameter("dateTo"));

        try {
            List<Report> reports = reportService.search(status, reason, targetType, reporterUsername,
                    dateFrom, dateTo, page);
            int totalCount = reportService.count(status, reason, targetType, reporterUsername, dateFrom, dateTo);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / reportService.getPageSize()));

            request.setAttribute("reports", reports);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("status", status);
            request.setAttribute("reason", reason);
            request.setAttribute("targetType", targetType);
            request.setAttribute("reporterUsername", reporterUsername);
            request.setAttribute("dateFrom", request.getParameter("dateFrom"));
            request.setAttribute("dateTo", request.getParameter("dateTo"));

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing reports", e);
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
