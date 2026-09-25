package com.gamenest.controller;

import com.gamenest.dto.PendingRegistration;
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
 * Step 1 of registration: validate input, check duplicates, then send an
 * OTP to the given email. The account itself is not created until
 * {@link RegisterVerifyServlet} confirms the OTP.
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RegisterServlet.class.getName());
    private static final String VIEW = "/account/register.jsp";

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

        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String displayName = request.getParameter("displayName");

        request.setAttribute("username", username);
        request.setAttribute("email", email);
        request.setAttribute("displayName", displayName);

        try {
            PendingRegistration pending = accountService.prepareRegistration(username, email, password, displayName);
            otpService.generateAndSend(pending.getEmail(), OtpPurpose.REGISTER);

            HttpSession session = request.getSession(true);
            session.setAttribute("pendingRegistration", pending);

            response.sendRedirect(request.getContextPath() + "/register/verify");

        } catch (ValidationException | DuplicateAccountException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (OtpException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during registration", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send registration OTP email", e);
            request.setAttribute("error", "Không thể gửi email xác thực, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }
}
