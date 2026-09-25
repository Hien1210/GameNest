package com.gamenest.service;

import com.gamenest.dao.NotificationDAO;
import com.gamenest.model.Notification;
import com.gamenest.model.NotificationTargetType;
import com.gamenest.model.NotificationType;
import com.gamenest.websocket.NotificationBroadcaster;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Facade for the Notifications module (task spec). Two distinct halves:
 * <p>
 * 1. WRITE side ({@code notifyXxx}) — called by other Services (Answer,
 * Report) right after a business operation has already committed
 * successfully. These are best-effort side effects: any failure is logged
 * and swallowed here, never propagated, so a Notification INSERT failure
 * can never roll back or fail a business operation that already succeeded
 * (task spec §17) — mirrors {@link AuditLogService#log}. Each successful
 * INSERT is additionally, and only afterward, pushed over the existing Chat
 * WebSocket connection via {@link NotificationBroadcaster} (Notification
 * Realtime task spec §3/§7) — realtime delivery is strictly an additional
 * transport on top of the DB write, never a replacement for it, and a
 * broadcast failure can no more fail this method than an INSERT failure
 * can fail the caller.
 * <p>
 * 2. READ side (list/count/mark-as-read) — called directly from User-facing
 * Servlets with an accountId that must always come from the session, never
 * the request (task spec §5). These behave like any other Service method
 * and let SQLException propagate to the Servlet.
 */
public class NotificationService {

    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());

    private static final int PAGE_SIZE = 20;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final int MESSAGE_MAX_LENGTH = 500;

    private final NotificationDAO notificationDAO;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
    }

    public NotificationService(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    // ---- Write side: business-event notifications (best-effort) ----

    /** Recipient is the Answer's author — never taken from the request (task spec §4.1). */
    public void notifyAnswerAccepted(int recipientAccountId, int answerId, String questionTitle) {
        create(recipientAccountId, NotificationType.ANSWER_ACCEPTED,
                "Câu trả lời của bạn đã được chấp nhận",
                "Câu trả lời của bạn cho câu hỏi \"" + questionTitle + "\" đã được chọn là câu trả lời hay nhất.",
                answerId, NotificationTargetType.ANSWER);
    }

    /**
     * Recipient is the Question's author. Callers must skip this entirely
     * when the author replies to their own Question (task spec §4.2) — this
     * method does not repeat that check.
     */
    public void notifyAnswerReply(int recipientAccountId, int questionId, String questionTitle) {
        create(recipientAccountId, NotificationType.ANSWER_REPLY,
                "Câu hỏi của bạn có câu trả lời mới",
                "Câu hỏi \"" + questionTitle + "\" của bạn vừa có một câu trả lời mới.",
                questionId, NotificationTargetType.QUESTION);
    }

    /** Recipient is the Report's reporter — never taken from the request (task spec §4.3). */
    public void notifyReportResolved(int recipientAccountId, int reportId) {
        create(recipientAccountId, NotificationType.REPORT_RESOLVED,
                "Báo cáo của bạn đã được xử lý",
                "Báo cáo #" + reportId + " của bạn đã được xem xét và xử lý.",
                reportId, NotificationTargetType.REPORT);
    }

    /** Recipient is the Report's reporter — never taken from the request (task spec §4.4). */
    public void notifyReportRejected(int recipientAccountId, int reportId, String resolutionNote) {
        StringBuilder message = new StringBuilder("Báo cáo #" + reportId + " của bạn đã bị từ chối.");
        if (resolutionNote != null && !resolutionNote.isEmpty()) {
            message.append(" Lý do: ").append(resolutionNote);
        }
        create(recipientAccountId, NotificationType.REPORT_REJECTED,
                "Báo cáo của bạn đã bị từ chối", message.toString(),
                reportId, NotificationTargetType.REPORT);
    }

    /**
     * Recipient is the followed account (following_account_id) — never the
     * follower. target_type is ACCOUNT / target_id is the follower's
     * account_id, so clicking the notification can lead back to the
     * follower's own Public Profile (task spec §4/§10).
     */
    public void notifyFollow(int recipientAccountId, int followerAccountId, String followerLabel) {
        create(recipientAccountId, NotificationType.FOLLOW,
                "Bạn có người theo dõi mới",
                followerLabel + " đã Follow bạn.",
                followerAccountId, NotificationTargetType.ACCOUNT);
    }

    /** Recipient is the receiver of the friend request — never the requester (task spec §15). */
    public void notifyFriendRequest(int recipientAccountId, int requesterAccountId, String requesterLabel) {
        create(recipientAccountId, NotificationType.FRIEND_REQUEST,
                "Bạn có lời mời kết bạn mới",
                requesterLabel + " đã gửi cho bạn một lời mời kết bạn.",
                requesterAccountId, NotificationTargetType.ACCOUNT);
    }

    /** Recipient is the original requester — never the person who just accepted (task spec §15). */
    public void notifyFriendAccepted(int recipientAccountId, int accepterAccountId, String accepterLabel) {
        create(recipientAccountId, NotificationType.FRIEND_ACCEPTED,
                "Lời mời kết bạn đã được chấp nhận",
                accepterLabel + " đã chấp nhận lời mời kết bạn của bạn.",
                accepterAccountId, NotificationTargetType.ACCOUNT);
    }

    /**
     * Recipient is the invitee. target_type is ACCOUNT / target_id is the
     * inviter's account_id — {@link NotificationTargetType} has no TEAM
     * value and the task explicitly forbids adding one just for this, so
     * clicking routes to the inviter's Public Profile (same as FOLLOW/
     * FRIEND_REQUEST) rather than the team itself; the team name is only in
     * the message text.
     */
    public void notifyTeamInvite(int recipientAccountId, int inviterAccountId, String inviterLabel, String teamName) {
        create(recipientAccountId, NotificationType.TEAM_INVITE,
                "Bạn có lời mời vào nhóm mới",
                inviterLabel + " đã mời bạn tham gia nhóm \"" + teamName + "\".",
                inviterAccountId, NotificationTargetType.ACCOUNT);
    }

    /**
     * Generic create — supports {@link NotificationType#SYSTEM} at the
     * service/schema level as required, but no Admin UI calls this in this
     * task (task spec §4.5).
     */
    private void create(int recipientAccountId, String type, String title, String message,
                         Integer targetId, String targetType) {
        try {
            Notification notification = new Notification();
            notification.setRecipientAccountId(recipientAccountId);
            notification.setType(type);
            notification.setTitle(truncate(title, TITLE_MAX_LENGTH));
            notification.setMessage(message == null ? null : truncate(message, MESSAGE_MAX_LENGTH));
            notification.setTargetId(targetId);
            notification.setTargetType(targetType);
            notification.setCreatedAt(LocalDateTime.now());
            notificationDAO.insert(notification);

            // Realtime delivery only ever runs after the INSERT above has
            // returned successfully, i.e. already committed (DBConnection
            // has no explicit transaction; each insert auto-commits on
            // return — task spec §7/§19 Case 1/Case 5: never broadcast
            // before commit, never broadcast a Notification that doesn't
            // exist in the DB). Any broadcast failure is contained entirely
            // inside NotificationBroadcaster/ChatWebSocketEndpoint's
            // send-quietly pattern and can never reach this catch block as
            // a reason to treat notification creation as failed.
            broadcastRealtime(notification);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to create notification (business operation already succeeded): "
                    + "type=" + type + " recipientAccountId=" + recipientAccountId, e);
        }
    }

    /**
     * Isolated in its own method, wrapped in its own try/catch, so that an
     * unexpected runtime failure in the realtime transport layer can never
     * propagate out of {@link #create} and be mistaken for a Notification
     * creation failure (task spec §17/§19 Case 6) — the INSERT above has
     * already succeeded by the time this runs.
     */
    private void broadcastRealtime(Notification notification) {
        try {
            NotificationBroadcaster.broadcastCreated(notification);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Notification realtime broadcast failed (notification already persisted): "
                    + "notificationId=" + notification.getNotificationId(), e);
        }
    }

    private String truncate(String value, int maxLength) {
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    // ---- Read side: current User's own notifications ----

    public List<Notification> list(int accountId, boolean unreadOnly, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return notificationDAO.findByRecipient(accountId, unreadOnly, offset, PAGE_SIZE);
    }

    public int count(int accountId, boolean unreadOnly) throws SQLException {
        return notificationDAO.countByRecipient(accountId, unreadOnly);
    }

    public int countUnread(int accountId) throws SQLException {
        return notificationDAO.countByRecipient(accountId, true);
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    /** No-op (0 rows affected) if the notification does not belong to accountId — never throws for that case. */
    public void markAsRead(int notificationId, int accountId) throws SQLException {
        notificationDAO.markAsRead(notificationId, accountId);
    }

    public void markAllAsRead(int accountId) throws SQLException {
        notificationDAO.markAllAsRead(accountId);
    }

    /** Scoped lookup for the click-to-open flow — returns empty if the notification is not the caller's own. */
    public Optional<Notification> findOwned(int notificationId, int accountId) throws SQLException {
        return notificationDAO.findByIdForAccount(notificationId, accountId);
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }
}
