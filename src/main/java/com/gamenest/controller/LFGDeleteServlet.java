package com.gamenest.controller;

import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.LFGNotFoundException;
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
 * Creator-only soft delete: OPEN/FULL/CLOSED -> DELETED. Never a hard
 * delete — no {@code DELETE FROM LFGPosts} anywhere (task spec §22). Gated
 * by {@link com.gamenest.filter.AuthenticationFilter}; ownership enforced
 * server-side in {@link LFGService#softDelete}.
 */
@WebServlet(name = "LFGDeleteServlet", urlPatterns = {"/lfg/delete"})
public class LFGDeleteServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LFGDeleteServlet.class.getName());

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
            lfgService.softDelete(lfgId, accountId);
            response.sendRedirect(request.getContextPath() + "/lfg?deleted=1");

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (LFGNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/lfg");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while deleting LFG post", e);
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
