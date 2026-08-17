package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.AnswerNotFoundException;
import com.gamenest.model.Account;
import com.gamenest.model.Answer;
import com.gamenest.model.Notification;
import com.gamenest.model.NotificationTargetType;
import com.gamenest.service.AccountService;
import com.gamenest.service.AnswerService;
import com.gamenest.service.NotificationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * "Click a notification": marks it as read, then redirects to its target
 * (task spec §13). QUESTION, ANSWER, and ACCOUNT (FOLLOW notifications)
 * targets have a User-facing route today — an ANSWER notification only
 * stores the answer_id, so its parent question_id is resolved here via
 * {@link AnswerService}; an ACCOUNT notification only stores the follower's
 * account_id, so its username is resolved via {@link AccountService} to
 * build the Public Profile URL. REPORT (no User-facing Report detail page
 * exists — Report review pages are Moderator/Admin-only) and any other/
 * unknown target simply redirect back to the notification list; nothing
 * crashes and no Moderator-only route is ever exposed to a User through
 * this endpoint.
 */
@WebServlet(name = "NotificationOpenServlet", urlPatterns = {"/account/notifications/open"})
public class NotificationOpenServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NotificationOpenServlet.class.getName());
    private static final String LIST_VIEW = "/account/notifications";

    private final NotificationService notificationService = new NotificationService();
    private final AnswerService answerService = new AnswerService();
    private final AccountService accountService = new AccountService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");
        int notificationId = parseId(request.getParameter("id"));

        try {
            Optional<Notification> ownedNotification = notificationService.findOwned(notificationId, accountId);
            if (ownedNotification.isEmpty()) {
                response.sendRedirect(request.getContextPath() + LIST_VIEW);
                return;
            }
            Notification notification = ownedNotification.get();

            notificationService.markAsRead(notificationId, accountId);

            String targetType = notification.getTargetType();
            Integer targetId = notification.getTargetId();

            if (NotificationTargetType.QUESTION.equals(targetType) && targetId != null) {
                response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + targetId);
                return;
            }

            if (NotificationTargetType.ANSWER.equals(targetType) && targetId != null) {
                try {
                    Answer answer = answerService.getAnswer(targetId);
                    response.sendRedirect(request.getContextPath() + "/questions/detail?id=" + answer.getQuestionId());
                    return;
                } catch (AnswerNotFoundException e) {
                    session.setAttribute("flashError", "Nội dung của thông báo này không còn tồn tại.");
                    response.sendRedirect(request.getContextPath() + LIST_VIEW);
                    return;
                }
            }

            if (NotificationTargetType.ACCOUNT.equals(targetType) && targetId != null) {
                try {
                    // Reused despite the "ForAdmin" name — it is just a plain
                    // fetch-by-id (exists check only, no status filter), which
                    // is exactly what's needed here: PublicProfileServlet
                    // itself already re-applies the ACTIVE gate on the next
                    // request, so a since-deactivated follower still resolves
                    // gracefully there instead of being duplicated here.
                    Account follower = accountService.getAccountForAdmin(targetId);
                    String encodedUsername = URLEncoder.encode(follower.getUsername(), StandardCharsets.UTF_8);
                    response.sendRedirect(request.getContextPath() + "/account/view?username=" + encodedUsername);
                    return;
                } catch (AccountNotFoundException e) {
                    session.setAttribute("flashError", "Người dùng này không còn tồn tại.");
                    response.sendRedirect(request.getContextPath() + LIST_VIEW);
                    return;
                }
            }

            // REPORT / SYSTEM / no target: no User-facing route — already
            // marked as read above, just stay on the list.
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while opening notification", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + LIST_VIEW);
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
