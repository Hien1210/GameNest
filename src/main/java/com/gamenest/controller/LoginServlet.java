package com.gamenest.controller;

import com.gamenest.exception.AccountStatusException;
import com.gamenest.exception.AuthenticationException;
import com.gamenest.model.Account;
import com.gamenest.model.AccountRole;
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

@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private static final String VIEW = "/account/login.jsp";

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String identifier = request.getParameter("identifier");
        String password = request.getParameter("password");

        request.setAttribute("identifier", identifier);

        try {
            Account account = accountService.login(identifier, password);

            // Prevent session fixation: discard any pre-login session before issuing a new one.
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute("accountId", account.getAccountId());
            session.setAttribute("username", account.getUsername());
            session.setAttribute("displayName", account.getDisplayName());
            session.setAttribute("role", account.getRole());

            String destination = AccountRole.ADMIN.equals(account.getRole())
                    ? "/admin/dashboard"
                    : "/account/home.jsp";
            response.sendRedirect(request.getContextPath() + destination);

        } catch (AuthenticationException | AccountStatusException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during login", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }
}
