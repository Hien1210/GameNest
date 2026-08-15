package com.gamenest.controller;

import com.gamenest.exception.QuestionNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.AuditAction;
import com.gamenest.model.AuditModule;
import com.gamenest.model.AuditTargetType;
import com.gamenest.model.Question;
import com.gamenest.model.QuestionStatus;
import com.gamenest.service.AuditLogService;
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
 * Moderator status-change action for Questions (ACTIVE/HIDDEN/LOCKED only
 * — task scope is status moderation, not deletion; see
 * {@link QuestionService#changeStatusForModeration}). Never touches
 * title/content/account_id/game_id. Moderator identity always comes from
 * the session, never the request.
 */
@WebServlet(name = "ModeratorQuestionStatusServlet", urlPatterns = {"/moderator/questions/status"})
public class ModeratorQuestionStatusServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ModeratorQuestionStatusServlet.class.getName());
    private static final String DETAIL_VIEW = "/moderator/questions/detail.jsp";
    private static final String LIST_VIEW = "/moderator/questions/list.jsp";

    private final QuestionService questionService = new QuestionService();
    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int questionId = parseId(request.getParameter("id"));
        String newStatus = mapAction(request.getParameter("action"));

        HttpSession session = request.getSession(false);
        Object moderatorIdAttr = session == null ? null : session.getAttribute("accountId");

        if (newStatus == null || !(moderatorIdAttr instanceof Integer)) {
            request.setAttribute("error", "Hành động không hợp lệ.");
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
            return;
        }

        try {
            Question before = questionService.getQuestion(questionId);
            String oldStatus = before.getStatus();

            questionService.changeStatusForModeration(questionId, newStatus);

            String description = "đã thay đổi trạng thái Question #" + questionId + " (\"" + before.getTitle()
                    + "\") từ " + oldStatus + " sang " + newStatus + ".";
            auditLogService.log(request, AuditModule.QUESTIONS, AuditAction.STATUS_CHANGE,
                    questionId, AuditTargetType.QUESTION, description);

            response.sendRedirect(request.getContextPath() + "/moderator/questions/detail?id=" + questionId);

        } catch (QuestionNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);

        } catch (ValidationException e) {
            forwardToDetail(request, response, questionId, e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while changing question status", e);
            forwardToDetail(request, response, questionId, "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }
    }

    private void forwardToDetail(HttpServletRequest request, HttpServletResponse response,
                                  int questionId, String errorMessage)
            throws ServletException, IOException {
        try {
            Question question = questionService.getQuestion(questionId);
            request.setAttribute("question", question);
            request.setAttribute("error", errorMessage);
            request.getRequestDispatcher(DETAIL_VIEW).forward(request, response);
        } catch (QuestionNotFoundException | SQLException e) {
            request.setAttribute("error", errorMessage);
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
        }
    }

    private String mapAction(String action) {
        if (action == null) {
            return null;
        }
        return switch (action) {
            case "activate" -> QuestionStatus.ACTIVE;
            case "hide" -> QuestionStatus.HIDDEN;
            case "lock" -> QuestionStatus.LOCKED;
            default -> null;
        };
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
