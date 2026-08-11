package com.gamenest.controller.admin;

import com.gamenest.model.Game;
import com.gamenest.service.GameService;

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
 * Admin game catalog listing (any status). Access is gated by
 * {@link com.gamenest.filter.AdminAuthorizationFilter} on /admin/*.
 */
@WebServlet(name = "AdminGamesServlet", urlPatterns = {"/admin/games"})
public class AdminGamesServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminGamesServlet.class.getName());
    private static final String VIEW = "/admin/games/list.jsp";

    private final GameService gameService = new GameService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int page = parsePage(request.getParameter("page"));

        try {
            List<Game> games = gameService.listAllGamesForAdmin(page);
            int totalCount = gameService.countAllGamesForAdmin();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / gameService.getAdminPageSize()));

            request.setAttribute("games", games);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing games for admin", e);
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
