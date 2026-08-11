package com.gamenest.controller;

import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.QuestionNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Question;
import com.gamenest.service.QuestionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gated by {@link com.gamenest.filter.AuthenticationFilter} for
 * authentication. Editing is owner-only — ADMIN cannot edit another user's
 * question (it may only moderate via soft-delete) — enforced server-side in
 * {@link QuestionService}, never inferred from hidden fields or the UI.
 */
@WebServlet(name = "QuestionEditServlet", urlPatterns = {"/questions/edit"})
public class QuestionEditServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(QuestionEditServlet.class.getName());
    private static final String VIEW = "/questions/form.jsp";

    private final QuestionService questionService = new QuestionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int questionId = parseId(request.getParameter("id"));
        HttpSession session = request.getSession(false);
        int accountId = session == null || session.getAttribute("accountId") == null
                ? -1 : (int) session.getAttribute("accountId");

        try {
            Question question = questionService.getQuestion(questionId);
            if (question.getAccountId() != accountId) {
                request.getRequestDispatcher("/access-denied.jsp").forward(request, response);
                return;
            }

            request.setAttribute("mode", "edit");
            request.setAttribute("questionId", question.getQuestionId());
            request.setAttribute("gameId", question.getGameId());
            request.setAttribute("title", question.getTitle());
            request.setAttribute("content", question.getContent());

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (QuestionNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading question for edit", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);
        }
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

        int questionId = parseId(request.getParameter("id"));
        String title = request.getParameter("title");
        String content = request.getParameter("content");

        request.setAttribute("mode", "edit");
        request.setAttribute("questionId", questionId);
        request.setAttribute("title", title);
        request.setAttribute("content", content);

        try {
            Question question = questionService.updateQuestion(questionId, accountId, title, content);
            request.setAttribute("gameId", question.getGameId());
            response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + questionId);

        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (QuestionNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while updating question", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
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
