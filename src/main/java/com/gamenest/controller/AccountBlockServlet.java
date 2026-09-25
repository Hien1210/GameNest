package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.ValidationException;
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
 * always comes from the session (task spec §4) — the form only ever submits
 * the target's username, matching Follow/Friend's existing "identify by
 * username" convention.
 */
@WebServlet(name = "AccountBlockServlet", urlPatterns = {"/account/block"})
public class AccountBlockServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountBlockServlet.class.getName());

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

        try {
            accountBlockService.block(accountId, targetUsername);

        } catch (ValidationException e) {
            // Self-block — the button is never rendered for isSelf, so this
            // is only reachable by a tampered request; still handled gracefully.
            session.setAttribute("flashError", e.getMessage());

        } catch (AccountNotFoundException e) {
            // No flash needed — PublicProfileServlet already renders its own
            // "not found" message for this exact username on the next request.

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while blocking account", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        String encoded = targetUsername == null ? "" : URLEncoder.encode(targetUsername, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/account/view?username=" + encoded);
    }
}
