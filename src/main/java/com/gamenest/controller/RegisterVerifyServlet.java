package com.gamenest.controller;

import com.gamenest.dto.PendingRegistration;
import com.gamenest.exception.DuplicateAccountException;
import com.gamenest.exception.OtpException;
import com.gamenest.model.Account;
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
 * Step 2 of registration: confirm the OTP sent to the pending email, then
 * create the account. The registration payload lives only in the session
 * between step 1 and this step — never persisted before OTP success.
 */
@WebServlet(name = "RegisterVerifyServlet", urlPatterns = {"/register/verify"})
public class RegisterVerifyServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RegisterVerifyServlet.class.getName());
    private static final String VIEW = "/account/register-verify.jsp";

    private final AccountService accountService = new AccountService();
    private final OtpService otpService = new OtpService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PendingRegistration pending = getPending(request);
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }
        request.setAttribute("email", pending.getEmail());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PendingRegistration pending = getPending(request);
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }
        request.setAttribute("email", pending.getEmail());

        if ("1".equals(request.getParameter("resend"))) {
            try {
                otpService.generateAndSend(pending.getEmail(), OtpPurpose.REGISTER);
                request.setAttribute("info", "Đã gửi lại mã OTP.");
            } catch (OtpException e) {
                request.setAttribute("error", e.getMessage());
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error while resending OTP", e);
                request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            } catch (MessagingException e) {
                LOGGER.log(Level.SEVERE, "Failed to resend registration OTP email", e);
                request.setAttribute("error", "Không thể gửi email xác thực, vui lòng thử lại sau.");
            }
            request.getRequestDispatcher(VIEW).forward(request, response);
            return;
        }

        String otp = request.getParameter("otp");

        try {
            otpService.verify(pending.getEmail(), OtpPurpose.REGISTER, otp);
            Account account = accountService.createAccount(pending);
            LOGGER.info(() -> "New account registered: id=" + account.getAccountId());

            request.getSession().removeAttribute("pendingRegistration");
            response.sendRedirect(request.getContextPath() + "/login?registered=1");

        } catch (OtpException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (DuplicateAccountException e) {
            // Someone else registered the same username/email while this OTP was pending.
            request.getSession().removeAttribute("pendingRegistration");
            request.setAttribute("error", e.getMessage() + " Vui lòng đăng ký lại.");
            request.getRequestDispatcher("/account/register.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while completing registration", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private PendingRegistration getPending(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (PendingRegistration) session.getAttribute("pendingRegistration");
    }
}
