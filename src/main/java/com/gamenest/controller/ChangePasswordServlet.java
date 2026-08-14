package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.AuthenticationException;
import com.gamenest.exception.ValidationException;
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
 * Đổi mật khẩu (Account Settings). On success the credential has changed,
 * so the current session is invalidated immediately and the user is sent
 * back to Login — never auto-logged back in with the new password.
 */
@WebServlet(name = "ChangePasswordServlet", urlPatterns = {"/account/settings/password"})
public class ChangePasswordServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChangePasswordServlet.class.getName());
    private static final String VIEW = "/account/settings.jsp";

    private final AccountService accountService = new AccountService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");

        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        try {
            accountService.changePassword(accountId, currentPassword, newPassword, confirmPassword);

            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/account/login.jsp?passwordChanged=1");

        } catch (AuthenticationException | ValidationException e) {
            renderWithError(request, response, accountId, e.getMessage());

        } catch (AccountNotFoundException e) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while changing password", e);
            renderWithError(request, response, accountId, "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }
    }

    private void renderWithError(HttpServletRequest request, HttpServletResponse response,
                                  int accountId, String errorMessage)
            throws ServletException, IOException {

        request.setAttribute("passwordError", errorMessage);
        try {
            request.setAttribute("account", accountService.getOwnProfile(accountId));
        } catch (AccountNotFoundException | SQLException ignored) {
            // Fall through with account == null; the JSP handles that case.
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
