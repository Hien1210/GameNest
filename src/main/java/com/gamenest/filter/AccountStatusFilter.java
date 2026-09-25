package com.gamenest.filter;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.model.Account;
import com.gamenest.model.AccountStatus;
import com.gamenest.service.AccountService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Re-validates the logged-in account's status against the database on every
 * request, since {@code status} is cached in the session at login and would
 * otherwise never be re-checked (a banned/suspended/deleted account would
 * stay fully privileged until the session naturally expired). Applied to
 * {@code /*} so it covers every authenticated route, not just the ones an
 * authorization filter's whitelist happens to list.
 */
@WebFilter(urlPatterns = {"/*"})
public class AccountStatusFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(AccountStatusFilter.class.getName());

    private final AccountService accountService = new AccountService();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("accountId") != null) {
            int accountId = (Integer) session.getAttribute("accountId");
            String contextPath = request.getContextPath();
            try {
                Account account = accountService.getOwnProfile(accountId);
                if (!AccountStatus.ACTIVE.equals(account.getStatus())) {
                    String message = AccountService.statusMessage(account.getStatus());
                    session.invalidate();
                    response.sendRedirect(contextPath + "/account/login.jsp?statusError="
                            + URLEncoder.encode(message, StandardCharsets.UTF_8));
                    return;
                }
            } catch (AccountNotFoundException e) {
                session.invalidate();
                response.sendRedirect(contextPath + "/account/login.jsp");
                return;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Khong the re-validate account status", e);
            }
        }

        chain.doFilter(req, res);
    }
}
