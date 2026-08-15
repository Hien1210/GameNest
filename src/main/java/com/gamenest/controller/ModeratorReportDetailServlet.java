package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.AnswerNotFoundException;
import com.gamenest.exception.QuestionNotFoundException;
import com.gamenest.exception.ReportNotFoundException;
import com.gamenest.model.Account;
import com.gamenest.model.Answer;
import com.gamenest.model.Question;
import com.gamenest.model.Report;
import com.gamenest.model.ReportTargetType;
import com.gamenest.service.AccountService;
import com.gamenest.service.AnswerService;
import com.gamenest.service.QuestionService;
import com.gamenest.service.ReportService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Moderator Report detail — Report info + reported target inspection (task
 * spec §7). Access is gated by
 * {@link com.gamenest.filter.ModeratorAuthorizationFilter} on /moderator/*.
 * Reuses {@link ReportService#getReportForAdmin}, {@link AccountService#getAccountForAdmin},
 * {@link QuestionService#getQuestion} and {@link AnswerService#getAnswer}
 * as-is — all four are already generic (no status filtering, no role
 * check baked in), so no Service/DAO change was needed for this task.
 */
@WebServlet(name = "ModeratorReportDetailServlet", urlPatterns = {"/moderator/reports/detail"})
public class ModeratorReportDetailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ModeratorReportDetailServlet.class.getName());
    private static final String VIEW = "/moderator/reports/detail.jsp";
    private static final String LIST_VIEW = "/moderator/reports/list.jsp";

    private final ReportService reportService = new ReportService();
    private final AccountService accountService = new AccountService();
    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int reportId = parseId(request.getParameter("id"));

        try {
            Report report = reportService.getReportForAdmin(reportId);
            request.setAttribute("report", report);
            loadTarget(request, report);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (ReportNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading report detail for moderator", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
        }
    }

    /**
     * A missing/soft-deleted target must never hide the Report itself —
     * only "targetMissing" is set so the JSP can show a fallback message
     * ("Target không còn tồn tại hoặc đã bị xóa.", task spec §7).
     */
    private void loadTarget(HttpServletRequest request, Report report) throws SQLException {
        String targetType = report.getTargetType();
        int targetId = report.getTargetId();

        try {
            if (ReportTargetType.ACCOUNT.equals(targetType)) {
                Account account = accountService.getAccountForAdmin(targetId);
                request.setAttribute("targetAccount", account);
            } else if (ReportTargetType.QUESTION.equals(targetType)) {
                Question question = questionService.getQuestion(targetId);
                request.setAttribute("targetQuestion", question);
            } else if (ReportTargetType.ANSWER.equals(targetType)) {
                Answer answer = answerService.getAnswer(targetId);
                request.setAttribute("targetAnswer", answer);
            }
        } catch (AccountNotFoundException | QuestionNotFoundException | AnswerNotFoundException e) {
            request.setAttribute("targetMissing", true);
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
