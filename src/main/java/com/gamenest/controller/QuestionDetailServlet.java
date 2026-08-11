package com.gamenest.controller;

import com.gamenest.exception.QuestionNotFoundException;
import com.gamenest.model.AccountRole;
import com.gamenest.model.Answer;
import com.gamenest.model.Question;
import com.gamenest.service.AnswerService;
import com.gamenest.service.QuestionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "QuestionDetailServlet", urlPatterns = {"/questions/detail"})
public class QuestionDetailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(QuestionDetailServlet.class.getName());
    private static final String VIEW = "/questions/detail.jsp";

    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int questionId = parseId(request.getParameter("id"));
        if (questionId <= 0) {
            request.setAttribute("error", "Câu hỏi không tồn tại.");
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);
            return;
        }

        try {
            Question question = questionService.getQuestion(questionId);
            List<Answer> answers = answerService.listActiveByQuestion(questionId);

            HttpSession session = request.getSession(false);
            Object accountIdAttr = session == null ? null : session.getAttribute("accountId");
            boolean isAdmin = session != null && AccountRole.ADMIN.equals(session.getAttribute("role"));
            boolean isOwner = accountIdAttr != null && ((int) accountIdAttr) == question.getAccountId();

            request.setAttribute("question", question);
            request.setAttribute("answers", answers);
            request.setAttribute("isOwner", isOwner);
            request.setAttribute("isAdmin", isAdmin);
            request.setAttribute("isLoggedIn", accountIdAttr != null);

            if (session != null && session.getAttribute("flashError") != null) {
                request.setAttribute("error", session.getAttribute("flashError"));
                session.removeAttribute("flashError");
            }

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (QuestionNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/games/list.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading question detail", e);
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
