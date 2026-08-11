package com.gamenest.controller;

import com.gamenest.exception.GameNotFoundException;
import com.gamenest.model.Game;
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

@WebServlet(name = "GameDetailServlet", urlPatterns = {"/games/detail"})
public class GameDetailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(GameDetailServlet.class.getName());

    private final GameService gameService = new GameService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int gameId = parseId(request.getParameter("id"));
        if (gameId <= 0) {
            request.setAttribute("error", "Game không tồn tại.");
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);
            return;
        }

        try {
            Game game = gameService.getActiveGameDetail(gameId);
            request.setAttribute("game", game);
            request.getRequestDispatcher("/games/detail.jsp").forward(request, response);

        } catch (GameNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading game detail", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);
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
