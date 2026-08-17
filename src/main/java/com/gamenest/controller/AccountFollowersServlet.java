package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.model.Account;
import com.gamenest.service.AccountFollowService;
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
 * "Ai đang Follow user này?" — public, same anonymous-browsing posture as
 * {@link PublicProfileServlet} (task spec §7 — a Public Profile viewer, even
 * anonymous, can still see it). Target resolved by username through
 * {@link AccountService#getPublicProfile}, so a followers list is only
 * reachable for an account whose Public Profile is itself visible.
 */
@WebServlet(name = "AccountFollowersServlet", urlPatterns = {"/account/followers"})
public class AccountFollowersServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountFollowersServlet.class.getName());
    private static final String VIEW = "/account/follow-list.jsp";

    private final AccountService accountService = new AccountService();
    private final AccountFollowService accountFollowService = new AccountFollowService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        int page = parsePage(request.getParameter("page"));

        try {
            Account target = accountService.getPublicProfile(username);

            List<Account> accounts = accountFollowService.listFollowers(target.getAccountId(), page);
            int totalCount = accountFollowService.countFollowers(target.getAccountId());
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / accountFollowService.getPageSize()));

            request.setAttribute("target", target);
            request.setAttribute("accounts", accounts);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("listTitle", "Followers");
            request.setAttribute("emptyMessage", "Chưa có ai Follow người dùng này.");

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (AccountNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing followers", e);
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
