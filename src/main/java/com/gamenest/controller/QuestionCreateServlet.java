package com.gamenest.controller;

import com.gamenest.exception.GameNotFoundException;
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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter} — reaching here
 * always implies a session exists. account_id is taken only from the
 * session, never from a request parameter.
 */
@WebServlet(name = "QuestionCreateServlet", urlPatterns = {"/questions/new"})
public class QuestionCreateServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(QuestionCreateServlet.class.getName());
    private static final String VIEW = "/questions/form.jsp";

    private final QuestionService questionService = new QuestionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int gameId = parseId(request.getParameter("gameId"));
        request.setAttribute("mode", "create");
        request.setAttribute("gameId", gameId);
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
        String content = request.getParameter("content");

        request.setAttribute("mode", "create");
        request.setAttribute("gameId", gameId);
        request.setAttribute("title", title);
        request.setAttribute("content", content);

        try {
            Question question = questionService.createQuestion(gameId, accountId, title, content);
            response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + question.getQuestionId());

        } catch (ValidationException | GameNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while creating question", e);
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
