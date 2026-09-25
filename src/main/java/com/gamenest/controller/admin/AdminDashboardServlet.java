package com.gamenest.controller.admin;

import com.gamenest.dto.AdminDashboardStats;
import com.gamenest.service.AdminDashboardService;

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
 * Admin landing page. Access is gated by
 * {@link com.gamenest.filter.AdminAuthorizationFilter} on /admin/* — role
 * is read from the authenticated session there, never trusted from a
 * request parameter.
 */
@WebServlet(name = "AdminDashboardServlet", urlPatterns = {"/admin/dashboard"})
public class AdminDashboardServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminDashboardServlet.class.getName());
    private static final String VIEW = "/admin/dashboard.jsp";

    private final AdminDashboardService dashboardService = new AdminDashboardService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            AdminDashboardStats stats = dashboardService.getStats();
            request.setAttribute("stats", stats);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading admin dashboard stats", e);
            request.setAttribute("error", "Không thể tải thống kê lúc này, vui lòng thử lại sau.");
        }

        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
