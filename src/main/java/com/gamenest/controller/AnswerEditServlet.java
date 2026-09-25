package com.gamenest.controller;

import com.gamenest.exception.AnswerNotFoundException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Answer;
import com.gamenest.service.AnswerService;

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
 * answer (it may only moderate via soft-delete) — enforced server-side in
 * {@link AnswerService}.
 */
@WebServlet(name = "AnswerEditServlet", urlPatterns = {"/answers/edit"})
public class AnswerEditServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AnswerEditServlet.class.getName());
    private static final String VIEW = "/answers/form.jsp";

    private final AnswerService answerService = new AnswerService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int answerId = parseId(request.getParameter("id"));
        HttpSession session = request.getSession(false);
        int accountId = session == null || session.getAttribute("accountId") == null
                ? -1 : (int) session.getAttribute("accountId");

        try {
            Answer answer = answerService.getAnswer(answerId);
            if (answer.getAccountId() != accountId) {
                request.getRequestDispatcher("/access-denied.jsp").forward(request, response);
                return;
            }

            request.setAttribute("answerId", answer.getAnswerId());
            request.setAttribute("questionId", answer.getQuestionId());
            request.setAttribute("content", answer.getContent());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (AnswerNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading answer for edit", e);
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

        int answerId = parseId(request.getParameter("id"));
        String content = request.getParameter("content");

        request.setAttribute("answerId", answerId);
        request.setAttribute("content", content);

        try {
            Answer answer = answerService.updateAnswer(answerId, accountId, content);
            response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + answer.getQuestionId());

        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (AnswerNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while updating answer", e);
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
