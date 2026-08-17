package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.DuplicateFriendRequestException;
import com.gamenest.exception.ValidationException;
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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. requesterAccountId
 * always comes from the session (task spec §5) — the form only ever submits
 * the target's username, matching {@link PublicProfileServlet}/Follow's
 * existing "identify by username" convention.
 */
@WebServlet(name = "AccountFriendRequestServlet", urlPatterns = {"/account/friend/request"})
public class AccountFriendRequestServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountFriendRequestServlet.class.getName());

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
        String displayName = (String) session.getAttribute("displayName");
        String username = (String) session.getAttribute("username");
        String requesterLabel = displayName != null && !displayName.isEmpty() ? displayName : username;

        String targetUsername = request.getParameter("username");

        try {
            accountFriendService.sendRequest(accountId, requesterLabel, targetUsername);

        } catch (ValidationException | DuplicateFriendRequestException e) {
            // Duplicate / reverse-pending / already-friends / self-request —
            // all friendly business states, not errors: flash so the Public
            // Profile can explain why nothing happened.
            session.setAttribute("flashError", e.getMessage());

        } catch (AccountNotFoundException e) {
            // No flash needed — PublicProfileServlet already renders its own
            // "not found" message for this exact username on the next request.

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while sending friend request", e);
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
