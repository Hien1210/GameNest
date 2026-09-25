package com.gamenest.controller;

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
 * Public LFG list (viewing does not require login — same posture as
 * /games and /questions; task spec §14). DB-side filtered and paginated,
 * newest first, DELETED always excluded.
 */
@WebServlet(name = "LFGListServlet", urlPatterns = {"/lfg"})
public class LFGListServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LFGListServlet.class.getName());
    private static final String VIEW = "/lfg/list.jsp";

    private final LFGService lfgService = new LFGService();
    private final GameService gameService = new GameService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int page = parsePage(request.getParameter("page"));
        Integer gameId = parseOptionalId(request.getParameter("gameId"));
        String status = request.getParameter("status");
        String keyword = request.getParameter("q");

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("flashError") != null) {
            request.setAttribute("error", session.getAttribute("flashError"));
            session.removeAttribute("flashError");
        }

        try {
            List<LFGPost> posts = lfgService.search(gameId, status, keyword, page);
            int totalCount = lfgService.count(gameId, status, keyword);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / lfgService.getPageSize()));

            request.setAttribute("posts", posts);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("gameId", request.getParameter("gameId"));
            request.setAttribute("status", status);
            request.setAttribute("q", keyword);

            try {
                List<Game> games = gameService.listAllActiveGames();
                request.setAttribute("allActiveGames", games);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to load games for LFG filter", e);
            }

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing LFG posts", e);
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

    private Integer parseOptionalId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
