package com.gamenest.controller;

import com.gamenest.exception.AnswerNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Answer;
import com.gamenest.model.AnswerStatus;
import com.gamenest.model.AuditAction;
import com.gamenest.model.AuditModule;
import com.gamenest.model.AuditTargetType;
import com.gamenest.service.AnswerService;
import com.gamenest.service.AuditLogService;

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
 * Moderator status-change action for Answers (ACTIVE/HIDDEN only — the
 * real CK_Answers_status values have no LOCKED, unlike Questions; see
 * {@link AnswerService#changeStatusForModeration}). Never touches
 * content/account_id/question_id. Moderator identity always comes from the
 * session, never the request.
 */
@WebServlet(name = "ModeratorAnswerStatusServlet", urlPatterns = {"/moderator/answers/status"})
public class ModeratorAnswerStatusServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ModeratorAnswerStatusServlet.class.getName());
    private static final String DETAIL_VIEW = "/moderator/answers/detail.jsp";
    private static final String LIST_VIEW = "/moderator/answers/list.jsp";

    private final AnswerService answerService = new AnswerService();
    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int answerId = parseId(request.getParameter("id"));
        String newStatus = mapAction(request.getParameter("action"));

        HttpSession session = request.getSession(false);
        Object moderatorIdAttr = session == null ? null : session.getAttribute("accountId");

        if (newStatus == null || !(moderatorIdAttr instanceof Integer)) {
            request.setAttribute("error", "Hành động không hợp lệ.");
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
            return;
        }

        try {
            Answer before = answerService.getAnswer(answerId);
            String oldStatus = before.getStatus();

            answerService.changeStatusForModeration(answerId, newStatus);

            String description = "đã thay đổi trạng thái Answer #" + answerId
                    + " từ " + oldStatus + " sang " + newStatus + ".";
            auditLogService.log(request, AuditModule.ANSWERS, AuditAction.STATUS_CHANGE,
                    answerId, AuditTargetType.ANSWER, description);

            response.sendRedirect(request.getContextPath() + "/moderator/answers/detail?id=" + answerId);

        } catch (AnswerNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);

        } catch (ValidationException e) {
            forwardToDetail(request, response, answerId, e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while changing answer status", e);
            forwardToDetail(request, response, answerId, "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }
    }

    private void forwardToDetail(HttpServletRequest request, HttpServletResponse response,
                                  int answerId, String errorMessage)
            throws ServletException, IOException {
        try {
            Answer answer = answerService.getAnswer(answerId);
            request.setAttribute("answer", answer);
            request.setAttribute("error", errorMessage);
            request.getRequestDispatcher(DETAIL_VIEW).forward(request, response);
        } catch (AnswerNotFoundException | SQLException e) {
            request.setAttribute("error", errorMessage);
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
        }
    }

    private String mapAction(String action) {
        if (action == null) {
            return null;
        }
        return switch (action) {
            case "activate" -> AnswerStatus.ACTIVE;
            case "hide" -> AnswerStatus.HIDDEN;
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
