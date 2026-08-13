package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;
import com.gamenest.service.AccountService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Personal Profile — self-service view/edit of the currently logged-in
 * account. Gated by {@link com.gamenest.filter.AuthenticationFilter}, so
 * reaching here always implies a session exists. The acting account is
 * taken only from the session ("accountId"), never from a request
 * parameter, so a user can only ever see or edit their own data. Available
 * to both USER and ADMIN roles — managing OTHER accounts stays exclusively
 * under /admin/accounts.
 */
@WebServlet(name = "AccountProfileServlet", urlPatterns = {"/account/profile"})
@MultipartConfig(maxFileSize = 2_097_152)
public class AccountProfileServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountProfileServlet.class.getName());
    private static final String VIEW = "/account/profile.jsp";

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
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (AccountNotFoundException e) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading profile", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");

        String displayName = request.getParameter("displayName");

        try {
            accountService.updateOwnDisplayName(accountId, displayName);

            Object usernameAttr = session.getAttribute("username");
            String finalName = (displayName != null && !displayName.trim().isEmpty())
                    ? displayName.trim()
                    : (usernameAttr != null ? (String) usernameAttr : null);
            session.setAttribute("displayName", finalName);

            Part avatarPart = request.getPart("avatarFile");
            if (avatarPart != null && avatarPart.getSize() > 0) {
                String avatarUrl = accountService.updateOwnAvatar(accountId, avatarPart);
                session.setAttribute("avatarUrl", avatarUrl);
            }

            response.sendRedirect(request.getContextPath() + "/account/profile?updated=1");

        } catch (ValidationException e) {
            renderWithError(request, response, accountId, e.getMessage());

        } catch (AccountNotFoundException e) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");

        } catch (IllegalStateException | IOException e) {
            renderWithError(request, response, accountId, "Ảnh đại diện không được vượt quá 2 MB.");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while updating profile", e);
            renderWithError(request, response, accountId, "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }
    }

    private void renderWithError(HttpServletRequest request, HttpServletResponse response,
                                  int accountId, String errorMessage)
            throws ServletException, IOException {

        request.setAttribute("error", errorMessage);
        try {
            request.setAttribute("account", accountService.getOwnProfile(accountId));
        } catch (AccountNotFoundException | SQLException ignored) {
            // Fall through with account == null; the JSP handles that case.
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
