package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.service.AccountFollowService;

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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. followerAccountId
 * always comes from the session (task spec §6); the DELETE is always scoped
 * to (follower_account_id = session accountId, following_account_id =
 * resolved target) — User A can never unfollow User B's relationships.
 * A relationship that no longer exists is a graceful no-op (task spec §5).
 */
@WebServlet(name = "AccountUnfollowServlet", urlPatterns = {"/account/unfollow"})
public class AccountUnfollowServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountUnfollowServlet.class.getName());

    private final AccountFollowService accountFollowService = new AccountFollowService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");
        String targetUsername = request.getParameter("username");

        try {
            accountFollowService.unfollow(accountId, targetUsername);

        } catch (AccountNotFoundException e) {
            // No flash needed — PublicProfileServlet already renders its own
            // "not found" message for this exact username on the next request.

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while unfollowing account", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        String encoded = targetUsername == null ? "" : URLEncoder.encode(targetUsername, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/account/view?username=" + encoded);
    }
}
