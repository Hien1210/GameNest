package com.gamenest.controller;

import com.gamenest.exception.GameNotFoundException;
import com.gamenest.model.Game;
import com.gamenest.model.Question;
import com.gamenest.service.GameService;
import com.gamenest.service.QuestionService;

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
 * Public question list for one game (viewing does not require login, same
 * as the Games module).
 */
@WebServlet(name = "QuestionsServlet", urlPatterns = {"/questions"})
public class QuestionsServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(QuestionsServlet.class.getName());
    private static final String VIEW = "/questions/list.jsp";

    private final QuestionService questionService = new QuestionService();
    private final GameService gameService = new GameService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int gameId = parseId(request.getParameter("gameId"));
        String query = request.getParameter("q");
        int page = parsePage(request.getParameter("page"));

        if (gameId <= 0) {
            request.setAttribute("error", "Game không tồn tại.");
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);
            return;
        }

        try {
            Game game = gameService.getActiveGameDetail(gameId);

            List<Question> questions;
            int totalCount;
            if (query != null && !query.trim().isEmpty()) {
                questions = questionService.searchActiveByGame(gameId, query, page);
                totalCount = questionService.countSearchActiveByGame(gameId, query);
                request.setAttribute("query", query.trim());
            } else {
                questions = questionService.listActiveByGame(gameId, page);
                totalCount = questionService.countActiveByGame(gameId);
            }

            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / questionService.getPageSize()));

            request.setAttribute("game", game);
            request.setAttribute("questions", questions);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (GameNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing questions", e);
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

    private int parsePage(String raw) {
        try {
            return Math.max(Integer.parseInt(raw), 1);
        } catch (NumberFormatException | NullPointerException e) {
            return 1;
        }
    }
}
