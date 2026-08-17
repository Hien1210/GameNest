package com.gamenest.controller;

import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.LFGNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.service.LFGService;

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
 * Creator-only: OPEN/FULL -> CLOSED. Gated by
 * {@link com.gamenest.filter.AuthenticationFilter}; ownership enforced
 * server-side in {@link LFGService#close}.
 */
@WebServlet(name = "LFGCloseServlet", urlPatterns = {"/lfg/close"})
public class LFGCloseServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LFGCloseServlet.class.getName());

    private final LFGService lfgService = new LFGService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");
        int lfgId = parseId(request.getParameter("id"));

        try {
            lfgService.close(lfgId, accountId);
            response.sendRedirect(request.getContextPath() + "/lfg/detail?id=" + lfgId);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (LFGNotFoundException | ValidationException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/lfg/detail?id=" + lfgId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while closing LFG post", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + "/lfg/detail?id=" + lfgId);
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
