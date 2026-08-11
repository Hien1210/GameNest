package com.gamenest.controller;

import com.gamenest.exception.AnswerNotFoundException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.QuestionNotFoundException;
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
 * Only the question owner may accept/unaccept an answer for their own
 * question — enforced server-side in {@link AnswerService}, not by hiding
 * the button for other users.
 */
@WebServlet(name = "AnswerAcceptServlet", urlPatterns = {"/answers/accept"})
public class AnswerAcceptServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AnswerAcceptServlet.class.getName());

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
        int answerId = parseId(request.getParameter("answerId"));
        String action = request.getParameter("action");

        try {
            if ("unaccept".equals(action)) {
                answerService.unacceptAnswer(questionId, answerId, accountId);
            } else {
                answerService.acceptAnswer(questionId, answerId, accountId);
            }
            response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + questionId);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (QuestionNotFoundException | AnswerNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + questionId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while accepting answer", e);
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
