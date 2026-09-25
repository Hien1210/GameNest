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
 * request's original requester may Cancel — enforced in
 * {@link AccountFriendService#cancelRequest} via a guarded UPDATE (task spec
 * §8/§14). No Notification is sent on cancel (task spec §8).
 */
@WebServlet(name = "AccountFriendCancelServlet", urlPatterns = {"/account/friend/cancel"})
public class AccountFriendCancelServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountFriendCancelServlet.class.getName());

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
            accountFriendService.cancelRequest(friendshipId, accountId);

        } catch (FriendRequestNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while cancelling friend request", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        String encoded = counterpartUsername == null ? "" : URLEncoder.encode(counterpartUsername, StandardCharsets.UTF_8);
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
