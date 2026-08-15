package com.gamenest.controller;

import com.gamenest.exception.AnswerNotFoundException;
import com.gamenest.exception.QuestionNotFoundException;
import com.gamenest.model.Answer;
import com.gamenest.model.Question;
import com.gamenest.service.AnswerService;
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
 * Moderator Answer detail — Answer info + read-only Question context (task
 * spec §6). Reuses {@link AnswerService#getAnswer} and
 * {@link QuestionService#getQuestion} as-is — both are already generic (no
 * status filtering), so no Service/DAO change was needed for either read
 * path here.
 */
@WebServlet(name = "ModeratorAnswerDetailServlet", urlPatterns = {"/moderator/answers/detail"})
public class ModeratorAnswerDetailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ModeratorAnswerDetailServlet.class.getName());
    private static final String VIEW = "/moderator/answers/detail.jsp";
    private static final String LIST_VIEW = "/moderator/answers/list.jsp";

    private final AnswerService answerService = new AnswerService();
    private final QuestionService questionService = new QuestionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int answerId = parseId(request.getParameter("id"));

        try {
            Answer answer = answerService.getAnswer(answerId);
            request.setAttribute("answer", answer);
            loadQuestionContext(request, answer);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (AnswerNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading answer detail for moderator", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
        }
    }

    /**
     * A missing/soft-deleted Question must never hide the Answer itself —
     * only "questionMissing" is set so the JSP can show a fallback message
     * ("Question không còn tồn tại hoặc đã bị xóa.", task spec §6).
     */
    private void loadQuestionContext(HttpServletRequest request, Answer answer) throws SQLException {
        try {
            Question question = questionService.getQuestion(answer.getQuestionId());
            request.setAttribute("question", question);
        } catch (QuestionNotFoundException e) {
            request.setAttribute("questionMissing", true);
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
