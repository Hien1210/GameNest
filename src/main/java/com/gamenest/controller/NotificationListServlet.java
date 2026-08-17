package com.gamenest.controller;

import com.gamenest.model.Notification;
import com.gamenest.service.NotificationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. Lists only the
 * current session's own notifications, newest first, DB-side paginated —
 * accountId always comes from the session, never the request (task spec
 * §5/§9).
 */
@WebServlet(name = "NotificationListServlet", urlPatterns = {"/account/notifications"})
public class NotificationListServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NotificationListServlet.class.getName());
    private static final String VIEW = "/account/notifications.jsp";

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

        boolean unreadOnly = "unread".equals(request.getParameter("filter"));
        int page = parsePage(request.getParameter("page"));

        if (session.getAttribute("flashError") != null) {
            request.setAttribute("error", session.getAttribute("flashError"));
            session.removeAttribute("flashError");
        }

        try {
            List<Notification> notifications = notificationService.list(accountId, unreadOnly, page);
            int totalCount = notificationService.count(accountId, unreadOnly);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / notificationService.getPageSize()));
            int unreadCount = notificationService.countUnread(accountId);

            request.setAttribute("notifications", notifications);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("unreadOnly", unreadOnly);
            request.setAttribute("unreadCount", unreadCount);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing notifications", e);
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
}
