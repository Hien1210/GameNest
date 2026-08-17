package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.service.AccountBlockService;

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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. blockerAccountId
 * always comes from the session (task spec §4); the DELETE is always scoped
 * to (blocker_account_id = session accountId, blocked_account_id = resolved
 * target) — User A can never unblock User B's blocks. Never restores
 * Follow/Friendship (task spec §9/§25). Reachable both from Public Profile
 * and from the Blocked Users list — redirects back to whichever the form
 * came from via the "returnTo" hint, defaulting to the list.
 */
@WebServlet(name = "AccountUnblockServlet", urlPatterns = {"/account/unblock"})
public class AccountUnblockServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountUnblockServlet.class.getName());

    private final AccountBlockService accountBlockService = new AccountBlockService();

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
        boolean returnToProfile = "profile".equals(request.getParameter("returnTo"));

        try {
            accountBlockService.unblock(accountId, targetUsername);

        } catch (AccountNotFoundException e) {
            // No flash needed if returning to the profile — it already
            // renders its own "not found" message for this exact username.

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while unblocking account", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        if (returnToProfile) {
            String encoded = targetUsername == null ? "" : URLEncoder.encode(targetUsername, StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/account/view?username=" + encoded);
        } else {
            response.sendRedirect(request.getContextPath() + "/account/blocked");
        }
    }
}
