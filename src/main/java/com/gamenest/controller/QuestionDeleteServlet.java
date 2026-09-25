package com.gamenest.controller;

import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.QuestionNotFoundException;
import com.gamenest.model.AccountRole;
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
 * Soft-delete only — never DELETE FROM Questions (CLAUDE.md §7/§15).
 */
@WebServlet(name = "QuestionDeleteServlet", urlPatterns = {"/questions/delete"})
public class QuestionDeleteServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(QuestionDeleteServlet.class.getName());

    private final QuestionService questionService = new QuestionService();

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

        int questionId = parseId(request.getParameter("id"));
        int gameId = parseId(request.getParameter("gameId"));

        try {
            questionService.softDeleteQuestion(questionId, accountId, isAdmin);
            response.sendRedirect(request.getContextPath() + "/questions?gameId=" + gameId);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (QuestionNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while deleting question", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);
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
