package com.gamenest.controller;

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

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Step 1 of the forgot-password flow. Always responds with the same generic
 * message regardless of whether the email is registered, so this endpoint
 * cannot be used to enumerate accounts — the OTP is only actually sent when
 * a matching account exists.
 */
@WebServlet(name = "ForgotPasswordServlet", urlPatterns = {"/forgot-password"})
public class ForgotPasswordServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ForgotPasswordServlet.class.getName());
    private static final String VIEW = "/account/forgot-password.jsp";

    private final AccountService accountService = new AccountService();
    private final OtpService otpService = new OtpService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();

        try {
            if (normalizedEmail != null && !normalizedEmail.isEmpty()
                    && accountService.findByEmail(normalizedEmail).isPresent()) {
                try {
                    otpService.generateAndSend(normalizedEmail, OtpPurpose.RESET_PASSWORD);
                } catch (OtpException e) {
                    // Likely just the resend cooldown — a valid OTP was already sent recently.
                    LOGGER.info(() -> "Password reset OTP not resent: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during forgot-password lookup", e);
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send password reset OTP email", e);
        }

        request.setAttribute("email", email);
        request.setAttribute("info", "Nếu email tồn tại trong hệ thống, mã OTP đặt lại mật khẩu đã được gửi.");
        request.getRequestDispatcher("/account/reset-password.jsp").forward(request, response);
    }
}
