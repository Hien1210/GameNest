package com.gamenest.controller;

import com.gamenest.exception.AnswerNotFoundException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.model.AccountRole;
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
 * Soft-delete only — never DELETE FROM Answers (CLAUDE.md §7/§15).
 */
@WebServlet(name = "AnswerDeleteServlet", urlPatterns = {"/answers/delete"})
public class AnswerDeleteServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AnswerDeleteServlet.class.getName());

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
        boolean isAdmin = AccountRole.ADMIN.equals(session.getAttribute("role"));

        int answerId = parseId(request.getParameter("id"));
        int questionId = parseId(request.getParameter("questionId"));

        try {
            answerService.softDeleteAnswer(answerId, accountId, isAdmin);
            response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + questionId);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (AnswerNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + questionId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while deleting answer", e);
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
