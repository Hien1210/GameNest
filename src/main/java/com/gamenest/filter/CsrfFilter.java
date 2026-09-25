package com.gamenest.filter;

import com.gamenest.util.CsrfTokenUtil;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Site-wide CSRF protection using the synchronizer token pattern. Every
 * request gets a session-bound token available to JSPs via
 * {@code ${sessionScope.csrfToken}}; every POST must submit it back matching.
 * Applied to {@code /*} instead of a maintained whitelist so newly added
 * POST endpoints are protected by default.
 */
@WebFilter(urlPatterns = {"/*"})
public class CsrfFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(CsrfFilter.class.getName());

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession(true);
        CsrfTokenUtil.getOrCreateToken(session);

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String submitted = request.getParameter("csrfToken");
            if (!CsrfTokenUtil.isValid(session, submitted)) {
                LOGGER.log(Level.WARNING, "CSRF token invalid or missing for POST {0}", request.getRequestURI());
                request.getRequestDispatcher("/access-denied.jsp").forward(request, response);
                return;
            }
        }

        chain.doFilter(req, res);
    }
}
