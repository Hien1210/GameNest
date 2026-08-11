package com.gamenest.filter;

import com.gamenest.model.AccountRole;
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
 * Server-side gate for every /admin/* route. Client-side hiding of admin
 * links/buttons is never sufficient on its own — this filter is the actual
 * authorization boundary.
 */
@WebFilter(urlPatterns = {"/admin/*"})
public class AdminAuthorizationFilter implements Filter {

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

        if (!AccountRole.ADMIN.equals(session.getAttribute("role"))) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);
            return;
        }

        chain.doFilter(req, res);
    }
}
