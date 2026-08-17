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
 * Foundation endpoint for a future bell/dropdown UI (task spec §10) — plain
 * HTTP request/response, no WebSocket/polling wired up in this task. Returns
 * just the unread count for the current session's account as plain text.
 */
@WebServlet(name = "NotificationCountServlet", urlPatterns = {"/account/notifications/count"})
public class NotificationCountServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NotificationCountServlet.class.getName());

    private final NotificationService notificationService = new NotificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");

        try {
            int count = notificationService.countUnread(accountId);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(String.valueOf(count));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while counting unread notifications", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
