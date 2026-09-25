package com.gamenest.controller;

import com.gamenest.dto.PendingEmailChange;
import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.DuplicateAccountException;
import com.gamenest.exception.OtpException;
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
 * Step 2 of Đổi Email (Account Settings): confirms the OTP sent to the
 * pending new email, then updates Accounts.email. The database is only
 * touched after OTP verification succeeds. Also handles "resend OTP" for
 * the same pending email, mirroring RegisterVerifyServlet.
 */
@WebServlet(name = "ChangeEmailVerifyServlet", urlPatterns = {"/account/settings/email/verify"})
public class ChangeEmailVerifyServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChangeEmailVerifyServlet.class.getName());
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

        PendingEmailChange pending = (PendingEmailChange) session.getAttribute("pendingEmailChange");
        if (pending == null || pending.getAccountId() != accountId) {
            response.sendRedirect(request.getContextPath() + "/account/settings");
            return;
        }

        if ("1".equals(request.getParameter("resend"))) {
            try {
                otpService.generateAndSend(pending.getNewEmail(), OtpPurpose.CHANGE_EMAIL);
                request.setAttribute("emailInfo", "Đã gửi lại mã OTP.");
            } catch (OtpException e) {
                request.setAttribute("emailError", e.getMessage());
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error while resending email-change OTP", e);
                request.setAttribute("emailError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            } catch (MessagingException e) {
                LOGGER.log(Level.SEVERE, "Failed to resend email-change OTP", e);
                request.setAttribute("emailError", "Không thể gửi email xác thực, vui lòng thử lại sau.");
            }
            renderPending(request, response, accountId, pending.getNewEmail());
            return;
        }

        String otp = request.getParameter("otp");

        try {
            otpService.verify(pending.getNewEmail(), OtpPurpose.CHANGE_EMAIL, otp);
            accountService.confirmEmailChange(accountId, pending.getNewEmail());

            session.removeAttribute("pendingEmailChange");
            response.sendRedirect(request.getContextPath() + "/account/settings?emailChanged=1");

        } catch (OtpException e) {
            request.setAttribute("emailError", e.getMessage());
            renderPending(request, response, accountId, pending.getNewEmail());

        } catch (DuplicateAccountException e) {
            // Someone else took the new email while this OTP was pending.
            session.removeAttribute("pendingEmailChange");
            request.setAttribute("emailError", e.getMessage() + " Vui lòng thử lại với email khác.");
            renderSettings(request, response, accountId);

        } catch (AccountNotFoundException e) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while confirming email change", e);
            request.setAttribute("emailError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            renderPending(request, response, accountId, pending.getNewEmail());
        }
    }

    private void renderPending(HttpServletRequest request, HttpServletResponse response,
                                int accountId, String pendingNewEmail)
            throws ServletException, IOException {
        request.setAttribute("pendingNewEmail", pendingNewEmail);
        renderSettings(request, response, accountId);
    }

    private void renderSettings(HttpServletRequest request, HttpServletResponse response, int accountId)
            throws ServletException, IOException {
        try {
            request.setAttribute("account", accountService.getOwnProfile(accountId));
        } catch (AccountNotFoundException | SQLException ignored) {
            // Fall through with account == null; the JSP handles that case.
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
