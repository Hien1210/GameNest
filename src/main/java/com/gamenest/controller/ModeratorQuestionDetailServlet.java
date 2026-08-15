package com.gamenest.controller;

import com.gamenest.exception.QuestionNotFoundException;
import com.gamenest.model.Question;
import com.gamenest.service.QuestionService;

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
 * Moderator Question detail. Reuses {@link QuestionService#getQuestion}
 * as-is — it already returns any status (not ACTIVE-only) with
 * authorUsername/gameName pre-joined, so soft-deleted/hidden/locked
 * questions remain visible for moderation history (task spec).
 */
@WebServlet(name = "ModeratorQuestionDetailServlet", urlPatterns = {"/moderator/questions/detail"})
public class ModeratorQuestionDetailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ModeratorQuestionDetailServlet.class.getName());
    private static final String VIEW = "/moderator/questions/detail.jsp";
    private static final String LIST_VIEW = "/moderator/questions/list.jsp";

    private final QuestionService questionService = new QuestionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int questionId = parseId(request.getParameter("id"));

        try {
            Question question = questionService.getQuestion(questionId);
            request.setAttribute("question", question);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (QuestionNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading question detail for moderator", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
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
