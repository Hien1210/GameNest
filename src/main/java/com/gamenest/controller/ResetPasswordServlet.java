package com.gamenest.controller;

import com.gamenest.exception.OtpException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.OtpPurpose;
import com.gamenest.service.AccountService;
import com.gamenest.service.OtpService;

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
 * Step 2 of the forgot-password flow: consumes the OTP and sets a new
 * password. The OTP itself is the only proof of email ownership required —
 * there is no separate account-status gate here, since resetting a password
 * does not by itself grant login (LoginServlet re-checks status).
 */
@WebServlet(name = "ResetPasswordServlet", urlPatterns = {"/reset-password"})
public class ResetPasswordServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ResetPasswordServlet.class.getName());
    private static final String VIEW = "/account/reset-password.jsp";

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
        String otp = request.getParameter("otp");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        request.setAttribute("email", email);

        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập email.");
            request.getRequestDispatcher(VIEW).forward(request, response);
            return;
        }

        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            request.setAttribute("error", "Mật khẩu xác nhận không khớp.");
            request.getRequestDispatcher(VIEW).forward(request, response);
            return;
        }

        try {
            otpService.verify(normalizedEmail, OtpPurpose.RESET_PASSWORD, otp);
            accountService.resetPassword(normalizedEmail, newPassword);

            response.sendRedirect(request.getContextPath() + "/login?reset=1");

        } catch (OtpException | ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while resetting password", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }
}
