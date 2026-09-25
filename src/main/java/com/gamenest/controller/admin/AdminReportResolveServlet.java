package com.gamenest.controller.admin;

import com.gamenest.exception.ReportNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.AuditAction;
import com.gamenest.model.AuditModule;
import com.gamenest.model.AuditTargetType;
import com.gamenest.model.Report;
import com.gamenest.service.AuditLogService;
import com.gamenest.service.ReportService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolve/Reject a PENDING Report (task spec §11). There is no delete
 * endpoint and no PENDING -> ... -> PENDING transition. Resolving a Report
 * only changes Reports.status — it never touches the reported target
 * (task spec §12): moderating the target itself is a separate, not-yet-
 * requested feature.
 */
@WebServlet(name = "AdminReportResolveServlet", urlPatterns = {"/admin/reports/resolve"})
public class AdminReportResolveServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminReportResolveServlet.class.getName());
    private static final String LIST_VIEW = "/admin/reports/list.jsp";

    private final ReportService reportService = new ReportService();
    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int reportId = parseId(request.getParameter("id"));
        String action = request.getParameter("action");
        String resolutionNote = request.getParameter("resolutionNote");

        HttpSession session = request.getSession(false);
        Object adminIdAttr = session == null ? null : session.getAttribute("accountId");

        if (!("resolve".equals(action) || "reject".equals(action)) || !(adminIdAttr instanceof Integer)) {
            request.setAttribute("error", "Hành động không hợp lệ.");
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
            return;
        }
        int adminAccountId = (Integer) adminIdAttr;

        try {
            Report before = reportService.getReportForAdmin(reportId);

            if ("resolve".equals(action)) {
                reportService.resolveReport(reportId, adminAccountId, resolutionNote);
            } else {
                reportService.rejectReport(reportId, adminAccountId, resolutionNote);
            }

            String auditAction = "resolve".equals(action) ? AuditAction.RESOLVE : AuditAction.REJECT;
            String verb = "resolve".equals(action) ? "đã resolve Report" : "đã reject Report";
            auditLogService.log(request, AuditModule.REPORTS, auditAction, reportId, AuditTargetType.REPORT,
                    verb + " #" + reportId + " (target " + before.getTargetType() + " #" + before.getTargetId() + ").");

            response.sendRedirect(request.getContextPath() + "/admin/reports/detail?id=" + reportId);

        } catch (ReportNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);

        } catch (ValidationException e) {
            forwardToDetail(request, response, reportId, e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while resolving report", e);
            forwardToDetail(request, response, reportId, "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }
    }

    private void forwardToDetail(HttpServletRequest request, HttpServletResponse response,
                                  int reportId, String errorMessage)
            throws ServletException, IOException {
        try {
            Report report = reportService.getReportForAdmin(reportId);
            request.setAttribute("report", report);
            request.setAttribute("targetLabel", reportService.getTargetLabel(report.getTargetType(), report.getTargetId()));
            request.setAttribute("error", errorMessage);
            request.getRequestDispatcher("/admin/reports/detail.jsp").forward(request, response);
        } catch (ReportNotFoundException | SQLException e) {
            request.setAttribute("error", errorMessage);
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
