package com.gamenest.controller.admin;

import com.gamenest.exception.ReportNotFoundException;
import com.gamenest.model.Report;
import com.gamenest.service.ReportService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin Report detail view. Access is gated by
 * {@link com.gamenest.filter.AdminAuthorizationFilter} on /admin/*.
 */
@WebServlet(name = "AdminReportDetailServlet", urlPatterns = {"/admin/reports/detail"})
public class AdminReportDetailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminReportDetailServlet.class.getName());
    private static final String VIEW = "/admin/reports/detail.jsp";
    private static final String LIST_VIEW = "/admin/reports/list.jsp";

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int reportId = parseId(request.getParameter("id"));

        try {
            Report report = reportService.getReportForAdmin(reportId);
            request.setAttribute("report", report);
            request.setAttribute("targetLabel", reportService.getTargetLabel(report.getTargetType(), report.getTargetId()));

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (ReportNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading report detail for admin", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
        }
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
