package com.gamenest.controller;

import com.gamenest.model.Question;
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
 * Moderator Questions listing — read-only, DB-side filtered and paginated
 * across ALL games/statuses (unlike the public /questions list, which is
 * scoped to one game and ACTIVE-only). Access is gated by
 * {@link com.gamenest.filter.ModeratorAuthorizationFilter} on /moderator/*.
 */
@WebServlet(name = "ModeratorQuestionsServlet", urlPatterns = {"/moderator/questions"})
public class ModeratorQuestionsServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ModeratorQuestionsServlet.class.getName());
    private static final String VIEW = "/moderator/questions/list.jsp";

    private final QuestionService questionService = new QuestionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int page = parsePage(request.getParameter("page"));
        String status = request.getParameter("status");
        Integer gameId = parseOptionalId(request.getParameter("gameId"));
        String authorUsername = request.getParameter("authorUsername");
        String search = request.getParameter("q");

        try {
            List<Question> questions = questionService.searchForModeration(status, gameId, authorUsername,
                    search, page);
            int totalCount = questionService.countForModeration(status, gameId, authorUsername, search);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / questionService.getModerationPageSize()));

            request.setAttribute("questions", questions);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("status", status);
            request.setAttribute("gameId", request.getParameter("gameId"));
            request.setAttribute("authorUsername", authorUsername);
            request.setAttribute("q", search);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing questions for moderator", e);
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
