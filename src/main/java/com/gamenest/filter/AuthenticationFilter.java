package com.gamenest.filter;

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

/**
 * Server-side gate for pages that require an authenticated session.
 * Client-side checks (hidden links, JS) are never sufficient on their own.
 */
@WebFilter(urlPatterns = {
        "/account/home.jsp",
        "/account/profile",
        "/questions/new",
        "/questions/edit",
        "/questions/delete",
        "/answers/*"
})
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }

        chain.doFilter(req, res);
    }
}
