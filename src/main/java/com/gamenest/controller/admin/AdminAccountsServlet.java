package com.gamenest.controller.admin;

import com.gamenest.model.Account;
import com.gamenest.service.AccountService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin account listing (any status), with optional search by
 * username/email. Access is gated by
 * {@link com.gamenest.filter.AdminAuthorizationFilter} on /admin/*.
 */
@WebServlet(name = "AdminAccountsServlet", urlPatterns = {"/admin/accounts"})
public class AdminAccountsServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminAccountsServlet.class.getName());
    private static final String VIEW = "/admin/accounts/list.jsp";

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int page = parsePage(request.getParameter("page"));
        String query = request.getParameter("q");
        boolean hasQuery = query != null && !query.trim().isEmpty();

        try {
            List<Account> accounts;
            int totalCount;

            if (hasQuery) {
                accounts = accountService.searchAccountsForAdmin(query, page);
                totalCount = accountService.countSearchAccountsForAdmin(query);
            } else {
                accounts = accountService.listAllAccountsForAdmin(page);
                totalCount = accountService.countAllAccountsForAdmin();
            }

            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / accountService.getAdminPageSize()));

            request.setAttribute("accounts", accounts);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("q", query == null ? "" : query.trim());

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing accounts for admin", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private int parsePage(String raw) {
        try {
            return Math.max(Integer.parseInt(raw), 1);
        } catch (NumberFormatException | NullPointerException e) {
            return 1;
        }
    }
}
