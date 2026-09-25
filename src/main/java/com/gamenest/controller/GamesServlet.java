package com.gamenest.controller;

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
 * Public game list + search. Only ACTIVE games are ever shown here.
 */
@WebServlet(name = "GamesServlet", urlPatterns = {"/games"})
public class GamesServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(GamesServlet.class.getName());
    private static final String VIEW = "/games/list.jsp";

    private final GameService gameService = new GameService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String query = request.getParameter("q");
        int page = parsePage(request.getParameter("page"));

        try {
            List<Game> games;
            int totalCount;

            if (query != null && !query.trim().isEmpty()) {
                games = gameService.searchActiveGames(query, page);
                totalCount = gameService.countSearchActiveGames(query);
                request.setAttribute("query", query.trim());
            } else {
                games = gameService.listActiveGames(page);
                totalCount = gameService.countActiveGames();
            }

            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / gameService.getPageSize()));

            request.setAttribute("games", games);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing games", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private int parsePage(String raw) {
        try {
            int page = Integer.parseInt(raw);
            return Math.max(page, 1);
        } catch (NumberFormatException | NullPointerException e) {
            return 1;
        }
    }
}
