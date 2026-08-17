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
 * Marks one Notification as read without navigating away (task spec §11) —
 * distinct from {@link NotificationOpenServlet}, which also redirects to the
 * target. accountId always comes from the session; the DAO-level
 * {@code WHERE notification_id = ? AND recipient_account_id = ?} guard means
 * a request for another account's notification_id silently affects 0 rows.
 */
@WebServlet(name = "NotificationReadServlet", urlPatterns = {"/account/notifications/read"})
public class NotificationReadServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NotificationReadServlet.class.getName());

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
        int notificationId = parseId(request.getParameter("id"));

        try {
            notificationService.markAsRead(notificationId, accountId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while marking notification as read", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        response.sendRedirect(request.getContextPath() + "/account/notifications");
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
