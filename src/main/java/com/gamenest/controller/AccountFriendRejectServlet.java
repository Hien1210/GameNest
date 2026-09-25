package com.gamenest.controller;

import com.gamenest.exception.FriendRequestNotFoundException;
import com.gamenest.service.AccountFriendService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. Only the
 * request's receiver may Reject — enforced in
 * {@link AccountFriendService#rejectRequest} via a guarded UPDATE (task spec
 * §7/§14). No Notification is sent on reject (task spec §7).
 */
@WebServlet(name = "AccountFriendRejectServlet", urlPatterns = {"/account/friend/reject"})
public class AccountFriendRejectServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountFriendRejectServlet.class.getName());

    private final AccountFriendService accountFriendService = new AccountFriendService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");
        int friendshipId = parseId(request.getParameter("friendshipId"));
        String counterpartUsername = request.getParameter("username");

        try {
            accountFriendService.rejectRequest(friendshipId, accountId);

        } catch (FriendRequestNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while rejecting friend request", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        redirectBack(request, response, counterpartUsername);
    }

    private void redirectBack(HttpServletRequest request, HttpServletResponse response, String counterpartUsername)
            throws IOException {
        if (counterpartUsername == null || counterpartUsername.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/account/friends/requests");
            return;
        }
        String encoded = URLEncoder.encode(counterpartUsername, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/account/view?username=" + encoded);
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
