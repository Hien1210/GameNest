package com.gamenest.controller.admin;

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
 * Admin account detail view. Access is gated by
 * {@link com.gamenest.filter.AdminAuthorizationFilter} on /admin/*.
 */
@WebServlet(name = "AdminAccountDetailServlet", urlPatterns = {"/admin/accounts/detail"})
public class AdminAccountDetailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminAccountDetailServlet.class.getName());
    private static final String VIEW = "/admin/accounts/detail.jsp";
    private static final String LIST_VIEW = "/admin/accounts/list.jsp";

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int accountId = parseId(request.getParameter("id"));

        try {
            Account account = accountService.getAccountForAdmin(accountId);

            HttpSession session = request.getSession(false);
            Object actingAdminId = session == null ? null : session.getAttribute("accountId");
            boolean isSelf = actingAdminId instanceof Integer && (Integer) actingAdminId == account.getAccountId();

            request.setAttribute("account", account);
            request.setAttribute("isSelf", isSelf);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (AccountNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading account detail for admin", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
        }
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
