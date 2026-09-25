package com.gamenest.controller;

import com.gamenest.dto.PendingEmailChange;
import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.DuplicateAccountException;
import com.gamenest.exception.OtpException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.OtpPurpose;
import com.gamenest.service.AccountService;
import com.gamenest.service.OtpService;
import jakarta.mail.MessagingException;

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
 * Step 1 of Đổi Email (Account Settings): validates the requested new
 * email, then sends an OTP to it. Accounts.email is NOT updated here — the
 * database only changes after {@link ChangeEmailVerifyServlet} confirms the
 * OTP.
 */
@WebServlet(name = "ChangeEmailRequestServlet", urlPatterns = {"/account/settings/email"})
public class ChangeEmailRequestServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChangeEmailRequestServlet.class.getName());
    private static final String VIEW = "/account/settings.jsp";

    private final AccountService accountService = new AccountService();
    private final OtpService otpService = new OtpService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");

        String newEmail = request.getParameter("newEmail");

        try {
            String normalized = accountService.requestEmailChange(accountId, newEmail);
            otpService.generateAndSend(normalized, OtpPurpose.CHANGE_EMAIL);

            session.setAttribute("pendingEmailChange", new PendingEmailChange(accountId, normalized));
            response.sendRedirect(request.getContextPath() + "/account/settings");

        } catch (ValidationException | DuplicateAccountException | OtpException e) {
            renderWithError(request, response, accountId, e.getMessage());

        } catch (AccountNotFoundException e) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while requesting email change", e);
            renderWithError(request, response, accountId, "Đã có lỗi xảy ra, vui lòng thử lại sau.");

        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send email-change OTP", e);
            renderWithError(request, response, accountId, "Không thể gửi email xác thực, vui lòng thử lại sau.");
        }
    }

    private void renderWithError(HttpServletRequest request, HttpServletResponse response,
                                  int accountId, String errorMessage)
            throws ServletException, IOException {

        request.setAttribute("emailError", errorMessage);
        try {
            request.setAttribute("account", accountService.getOwnProfile(accountId));
        } catch (AccountNotFoundException | SQLException ignored) {
            // Fall through with account == null; the JSP handles that case.
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
