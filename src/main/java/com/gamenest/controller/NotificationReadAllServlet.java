package com.gamenest.controller;

import com.gamenest.service.NotificationService;

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
 * Marks every unread Notification of the current session's account as read
 * (task spec §12) — a single UPDATE scoped by recipient_account_id, never a
 * full-table UPDATE.
 */
@WebServlet(name = "NotificationReadAllServlet", urlPatterns = {"/account/notifications/read-all"})
public class NotificationReadAllServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NotificationReadAllServlet.class.getName());

    private final NotificationService notificationService = new NotificationService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");

        try {
            notificationService.markAllAsRead(accountId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while marking all notifications as read", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        response.sendRedirect(request.getContextPath() + "/account/notifications");
    }
}
