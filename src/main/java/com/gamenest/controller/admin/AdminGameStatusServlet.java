package com.gamenest.controller.admin;

import com.gamenest.exception.GameNotFoundException;
import com.gamenest.service.GameService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Activate/deactivate only — Games are core business data and must never be
 * hard-deleted (see CLAUDE.md §7.1/§7.2). There is no delete endpoint.
 */
@WebServlet(name = "AdminGameStatusServlet", urlPatterns = {"/admin/games/status"})
public class AdminGameStatusServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminGameStatusServlet.class.getName());

    private final GameService gameService = new GameService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int gameId = parseId(request.getParameter("id"));
        String action = request.getParameter("action");

        try {
            if ("activate".equals(action)) {
                gameService.activateGame(gameId);
            } else if ("deactivate".equals(action)) {
                gameService.deactivateGame(gameId);
            } else {
                request.setAttribute("error", "Hành động không hợp lệ.");
                request.getRequestDispatcher("/admin/games/list.jsp").forward(request, response);
                return;
            }

            response.sendRedirect(request.getContextPath() + "/admin/games");

        } catch (GameNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/admin/games/list.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while changing game status", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher("/admin/games/list.jsp").forward(request, response);
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
