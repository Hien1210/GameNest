package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.DuplicateAccountFollowException;
import com.gamenest.exception.ValidationException;
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
 * always comes from the session (task spec §6) — the form only ever submits
 * the target's username, matching {@link PublicProfileServlet}'s existing
 * "identify by username" convention.
 */
@WebServlet(name = "AccountFollowServlet", urlPatterns = {"/account/follow"})
public class AccountFollowServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountFollowServlet.class.getName());

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
        String displayName = (String) session.getAttribute("displayName");
        String username = (String) session.getAttribute("username");
        String followerLabel = displayName != null && !displayName.isEmpty() ? displayName : username;

        String targetUsername = request.getParameter("username");

        try {
            accountFollowService.follow(accountId, followerLabel, targetUsername);

        } catch (ValidationException e) {
            // Self-follow — the button is never rendered for isSelf, so this
            // is only reachable by a tampered request; still handled gracefully.
            session.setAttribute("flashError", e.getMessage());

        } catch (DuplicateAccountFollowException e) {
            // Already following: no error needed, the profile page will
            // simply keep showing "Following" (task spec §5 duplicate case).

        } catch (AccountNotFoundException e) {
            // No flash needed — PublicProfileServlet already renders its own
            // "not found" message for this exact username on the next request.

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while following account", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        redirectToProfile(request, response, targetUsername);
    }

    private void redirectToProfile(HttpServletRequest request, HttpServletResponse response, String targetUsername)
            throws IOException {
        String encoded = targetUsername == null ? "" : URLEncoder.encode(targetUsername, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/account/view?username=" + encoded);
    }
}
