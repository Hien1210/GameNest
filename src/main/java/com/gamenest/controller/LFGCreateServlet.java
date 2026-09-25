package com.gamenest.controller;

import com.gamenest.exception.GameNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Game;
import com.gamenest.model.LFGPost;
import com.gamenest.service.GameService;
import com.gamenest.service.LFGService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. creator's
 * accountId always comes from the session, never the request (task spec
 * §6) — the form never submits an account_id/current_players/status field.
 */
@WebServlet(name = "LFGCreateServlet", urlPatterns = {"/lfg/new"})
public class LFGCreateServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LFGCreateServlet.class.getName());
    private static final String VIEW = "/lfg/form.jsp";

    private final LFGService lfgService = new LFGService();
    private final GameService gameService = new GameService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setAttribute("mode", "create");
        loadGames(request);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");

        int gameId = parseId(request.getParameter("gameId"));
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        int maxPlayers = parseId(request.getParameter("maxPlayers"));

        request.setAttribute("mode", "create");
        request.setAttribute("gameId", request.getParameter("gameId"));
        request.setAttribute("title", title);
        request.setAttribute("description", description);
        request.setAttribute("maxPlayers", request.getParameter("maxPlayers"));

        try {
            LFGPost created = lfgService.create(accountId, gameId, title, description, maxPlayers);
            response.sendRedirect(request.getContextPath() + "/lfg/detail?id=" + created.getLfgId());

        } catch (ValidationException | GameNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            loadGames(request);
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while creating LFG post", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            loadGames(request);
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private void loadGames(HttpServletRequest request) {
        try {
            List<Game> games = gameService.listAllActiveGames();
            request.setAttribute("allActiveGames", games);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading games for LFG form", e);
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
