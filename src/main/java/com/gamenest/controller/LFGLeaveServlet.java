package com.gamenest.controller;

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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. accountId
 * always comes from the session — {@link LFGService#leave} deletes only
 * the caller's own LFGMembers row (task spec §11). The creator cannot
 * Leave through this flow.
 */
@WebServlet(name = "LFGLeaveServlet", urlPatterns = {"/lfg/leave"})
public class LFGLeaveServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LFGLeaveServlet.class.getName());

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
            lfgService.leave(lfgId, accountId);

        } catch (LFGNotFoundException | ValidationException e) {
            session.setAttribute("flashError", e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while leaving LFG post", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        response.sendRedirect(request.getContextPath() + "/lfg/detail?id=" + lfgId);
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
