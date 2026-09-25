package com.gamenest.controller;

import com.gamenest.dto.PendingEmailChange;
import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.model.Account;
import com.gamenest.service.AccountService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Account Settings — Đổi mật khẩu / Đổi email for the currently logged-in
 * account. Available to both USER and ADMIN, always self-service only: the
 * acting account is taken solely from the session ("accountId"), never from
 * a request parameter. Actual mutations happen in
 * {@link ChangePasswordServlet}, {@link ChangeEmailRequestServlet} and
 * {@link ChangeEmailVerifyServlet}; this servlet only renders the page.
 */
@WebServlet(name = "AccountSettingsServlet", urlPatterns = {"/account/settings"})
public class AccountSettingsServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountSettingsServlet.class.getName());
    private static final String VIEW = "/account/settings.jsp";

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");

        try {
            Account account = accountService.getOwnProfile(accountId);
            request.setAttribute("account", account);

            PendingEmailChange pending = (PendingEmailChange) session.getAttribute("pendingEmailChange");
            if (pending != null && pending.getAccountId() == accountId) {
                request.setAttribute("pendingNewEmail", pending.getNewEmail());
            }

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (AccountNotFoundException e) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading account settings", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }
}
