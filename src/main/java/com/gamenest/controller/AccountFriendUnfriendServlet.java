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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. Either side of
 * an ACCEPTED friendship may Unfriend — enforced in
 * {@link AccountFriendService#unfriend} via a guarded UPDATE that checks
 * {@code requester_account_id = ? OR receiver_account_id = ?} against the
 * caller's own session accountId, so a third account can never affect
 * someone else's friendship (task spec §9/§19). No Notification is sent on
 * unfriend (task spec §9). Never a hard delete — only status becomes
 * UNFRIENDED.
 */
@WebServlet(name = "AccountFriendUnfriendServlet", urlPatterns = {"/account/friend/unfriend"})
public class AccountFriendUnfriendServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountFriendUnfriendServlet.class.getName());

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
            accountFriendService.unfriend(friendshipId, accountId);

        } catch (FriendRequestNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while unfriending", e);
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
