package com.gamenest.controller;

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
 * Resolve/Reject a PENDING Report from the Moderator Portal (task spec
 * §8/§9). Business rules are identical to
 * {@link com.gamenest.controller.admin.AdminReportResolveServlet} — both
 * call the exact same {@link ReportService#resolveReport}/{@link ReportService#rejectReport},
 * whose DAO-level {@code WHERE status = 'PENDING'} guard already prevents
 * double-processing (task spec §11) regardless of which portal a moderator
 * or admin acts from. Only the actor's role differs, and that is read from
 * the session, never the request — resolving a Report only changes
 * Reports.status; it never touches the reported target (task spec §10).
 */
@WebServlet(name = "ModeratorReportResolveServlet", urlPatterns = {"/moderator/reports/resolve"})
public class ModeratorReportResolveServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ModeratorReportResolveServlet.class.getName());
    private static final String VIEW = "/moderator/reports/detail.jsp";
    private static final String LIST_VIEW = "/moderator/reports/list.jsp";

    private final ReportService reportService = new ReportService();
    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int reportId = parseId(request.getParameter("id"));
        String action = request.getParameter("action");
        String resolutionNote = request.getParameter("resolutionNote");

        HttpSession session = request.getSession(false);
        Object moderatorIdAttr = session == null ? null : session.getAttribute("accountId");

        if (!("resolve".equals(action) || "reject".equals(action)) || !(moderatorIdAttr instanceof Integer)) {
            request.setAttribute("error", "Hành động không hợp lệ.");
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
            return;
        }
        int moderatorAccountId = (Integer) moderatorIdAttr;

        try {
            Report before = reportService.getReportForAdmin(reportId);

            if ("resolve".equals(action)) {
                reportService.resolveReport(reportId, moderatorAccountId, resolutionNote);
            } else {
                reportService.rejectReport(reportId, moderatorAccountId, resolutionNote);
            }

            String auditAction = "resolve".equals(action) ? AuditAction.RESOLVE : AuditAction.REJECT;
            String verb = "resolve".equals(action) ? "đã resolve Report" : "đã reject Report";
            auditLogService.log(request, AuditModule.REPORTS, auditAction, reportId, AuditTargetType.REPORT,
                    verb + " #" + reportId + " (target " + before.getTargetType() + " #" + before.getTargetId() + ").");

            response.sendRedirect(request.getContextPath() + "/moderator/reports/detail?id=" + reportId);

        } catch (ReportNotFoundException e) {
            // Also the race-condition path (task spec §11): the DAO's
            // WHERE status = 'PENDING' guard already made the UPDATE a
            // no-op if someone else resolved/rejected this report first, so
            // reviewed_by/reviewed_at/resolution_note from that first
            // decision are never overwritten.
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
            request.setAttribute("error", errorMessage);
            request.getRequestDispatcher(VIEW).forward(request, response);
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
