package com.gamenest.controller;

import com.gamenest.exception.DuplicatePendingReportException;
import com.gamenest.exception.ReportTargetInvalidException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.ReportTargetType;
import com.gamenest.service.ReportService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User-facing "Báo cáo" (Report) creation — ACCOUNT/QUESTION/ANSWER only.
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}; the reporter's
 * accountId is always read from the session, never from a request
 * parameter (task spec §4/§15).
 */
@WebServlet(name = "ReportCreateServlet", urlPatterns = {"/reports/create"})
public class ReportCreateServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ReportCreateServlet.class.getName());
    private static final String VIEW = "/reports/create.jsp";
    private static final Set<String> VALID_TARGET_TYPES = Set.of(
            ReportTargetType.ACCOUNT, ReportTargetType.QUESTION, ReportTargetType.ANSWER);

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }

        String targetType = request.getParameter("targetType");
        int targetId = parseId(request.getParameter("targetId"));

        if (targetType == null || !VALID_TARGET_TYPES.contains(targetType) || targetId <= 0) {
            request.setAttribute("error", "Đối tượng báo cáo không hợp lệ.");
            request.getRequestDispatcher(VIEW).forward(request, response);
            return;
        }

        request.setAttribute("targetType", targetType);
        request.setAttribute("targetId", targetId);
        request.setAttribute("targetLabel", reportService.getTargetLabel(targetType, targetId));
        request.setAttribute("success", request.getParameter("success") != null);

        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int reporterAccountId = (int) session.getAttribute("accountId");

        String targetType = request.getParameter("targetType");
        int targetId = parseId(request.getParameter("targetId"));
        String reason = request.getParameter("reason");
        String description = request.getParameter("description");

        try {
            reportService.createReport(reporterAccountId, targetType, targetId, reason, description);

            response.sendRedirect(request.getContextPath() + "/reports/create?targetType=" + targetType
                    + "&targetId=" + targetId + "&success=1");

        } catch (ValidationException | ReportTargetInvalidException | DuplicatePendingReportException e) {
            renderWithError(request, response, targetType, targetId, e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while creating report", e);
            renderWithError(request, response, targetType, targetId, "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }
    }

    private void renderWithError(HttpServletRequest request, HttpServletResponse response,
                                  String targetType, int targetId, String errorMessage)
            throws ServletException, IOException {

        request.setAttribute("error", errorMessage);
        request.setAttribute("targetType", targetType);
        request.setAttribute("targetId", targetId);
        if (targetType != null && targetId > 0) {
            request.setAttribute("targetLabel", reportService.getTargetLabel(targetType, targetId));
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
