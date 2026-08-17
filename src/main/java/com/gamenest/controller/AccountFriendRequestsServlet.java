package com.gamenest.controller;

import com.gamenest.model.AccountFriendship;
import com.gamenest.service.AccountFriendService;

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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter} — private view
 * of the current session's own incoming PENDING requests (task spec §11).
 * Outgoing requests are intentionally not listed here — the task only
 * requires incoming, and an outgoing view would need its own UI beyond
 * what's needed to use the feature (kept out of scope, see final report).
 */
@WebServlet(name = "AccountFriendRequestsServlet", urlPatterns = {"/account/friends/requests"})
public class AccountFriendRequestsServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountFriendRequestsServlet.class.getName());
    private static final String VIEW = "/account/friend-requests.jsp";

    private final AccountFriendService accountFriendService = new AccountFriendService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");
        int page = parsePage(request.getParameter("page"));

        if (session.getAttribute("flashError") != null) {
            request.setAttribute("error", session.getAttribute("flashError"));
            session.removeAttribute("flashError");
        }

        try {
            List<AccountFriendship> requests = accountFriendService.listIncomingRequests(accountId, page);
            int totalCount = accountFriendService.countIncomingRequests(accountId);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / accountFriendService.getPageSize()));

            request.setAttribute("requests", requests);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing incoming friend requests", e);
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
