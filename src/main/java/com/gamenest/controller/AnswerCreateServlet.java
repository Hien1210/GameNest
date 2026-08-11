package com.gamenest.controller;

import com.gamenest.exception.QuestionNotFoundException;
import com.gamenest.exception.ValidationException;
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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. account_id
 * always comes from the session, never from the request.
 */
@WebServlet(name = "AnswerCreateServlet", urlPatterns = {"/answers/new"})
public class AnswerCreateServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AnswerCreateServlet.class.getName());

    private final AnswerService answerService = new AnswerService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");

        int questionId = parseId(request.getParameter("questionId"));
        String content = request.getParameter("content");

        try {
            answerService.createAnswer(questionId, accountId, content);
            response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + questionId);

        } catch (ValidationException | QuestionNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + questionId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while creating answer", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + questionId);
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
