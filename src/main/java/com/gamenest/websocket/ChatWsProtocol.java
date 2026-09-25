package com.gamenest.websocket;

import com.gamenest.model.Message;
import com.gamenest.model.MessageReactionSummary;
import com.gamenest.model.Notification;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Server→client event builders for the Chat WebSocket protocol (task spec
 * §12/§13, extended with PRESENCE_CHANGED per the Presence task,
 * READ_UPDATED per the Realtime Read/Seen task, TYPING_STARTED/
 * TYPING_STOPPED per the Typing Indicator task, and NOTIFICATION_CREATED per
 * the Notification Realtime task). {@code createdAt} is pre-formatted
 * server-side with the exact same pattern {@code account/chat-detail.jsp}
 * already uses, so the client never has to parse/format a timestamp
 * itself.
 */
final class ChatWsProtocol {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final int REPLY_PREVIEW_MAX_LENGTH = 80;

    private ChatWsProtocol() {
    }

    static String connectionReady() {
        return "{\"type\":\"CONNECTION_READY\"}";
    }

    /**
     * Only fields the UI needs (task spec §13) — no password_hash, email,
     * role, or other account security data. {@code message} must already be
     * the fully-joined row (see {@code ChatService#sendMessage}'s
     * re-fetch-after-insert), not the bare insert result.
     * <p>
     * Reply feature: {@code replyToMessageId}/{@code replyToSenderLabel}/
     * {@code replyToPreview} are emitted as JSON {@code null} when the
     * message is not a reply, matching {@link #notificationCreated}'s own
     * null-field convention rather than omitting the keys. When the reply
     * target has been soft-deleted, {@code replyToDeleted} is {@code true}
     * and {@code replyToPreview} is {@code null} — the target's content is
     * never sent to the client once deleted; the client renders its own
     * fixed "deleted" placeholder text instead.
     * <p>
     * {@code replyToSenderLabel} is always the reply target's actual
     * display name/username — never "Bạn" ("You"), because a single
     * broadcast payload is delivered as-is to every recipient (all Team
     * members, or both DIRECT participants), so the server cannot bake in a
     * viewer-relative label. {@code replyToSenderAccountId} is included
     * (same precedent as the top-level {@code senderAccountId} already on
     * this payload) so each client can substitute "Bạn" locally by
     * comparing it against its own accountId.
     */
    static String messageCreated(Message message) {
        String senderDisplayName = message.getSenderDisplayName();
        String createdAt = message.getCreatedAt() != null ? message.getCreatedAt().format(TIME_FORMAT) : "";

        Integer replyToMessageId = message.getReplyToMessageId();
        Integer replyToSenderAccountId = message.getReplyToSenderAccountId();
        boolean replyToDeleted = message.getReplyToDeletedAt() != null;
        String replyToSenderLabel = null;
        String replyToPreview = null;
        if (replyToMessageId != null) {
            String label = message.getReplyToSenderDisplayName();
            replyToSenderLabel = (label != null && !label.isEmpty()) ? label : message.getReplyToSenderUsername();
            if (!replyToDeleted) {
                replyToPreview = truncate(message.getReplyToContent());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"MESSAGE_CREATED\",\"message\":{")
                .append("\"messageId\":").append(message.getMessageId()).append(',')
                .append("\"conversationId\":").append(message.getConversationId()).append(',')
                .append("\"senderAccountId\":").append(message.getSenderAccountId()).append(',')
                .append("\"senderUsername\":").append(ChatWsJson.quote(message.getSenderUsername())).append(',')
                .append("\"senderDisplayName\":").append(ChatWsJson.quote(senderDisplayName)).append(',')
                .append("\"content\":").append(ChatWsJson.quote(message.getContent())).append(',')
                .append("\"createdAt\":").append(ChatWsJson.quote(createdAt)).append(',')
                .append("\"replyToMessageId\":").append(replyToMessageId == null ? "null" : replyToMessageId).append(',')
                .append("\"replyToSenderAccountId\":").append(replyToSenderAccountId == null ? "null" : replyToSenderAccountId).append(',')
                .append("\"replyToSenderLabel\":").append(ChatWsJson.quote(replyToSenderLabel)).append(',')
                .append("\"replyToPreview\":").append(ChatWsJson.quote(replyToPreview)).append(',')
                .append("\"replyToDeleted\":").append(replyToDeleted)
                .append("}}");
        return sb.toString();
    }

    /** Short, fixed-length Reply Preview (task spec §8) — never the full message content. */
    private static String truncate(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= REPLY_PREVIEW_MAX_LENGTH) {
            return content;
        }
        return content.substring(0, REPLY_PREVIEW_MAX_LENGTH) + "…";
    }

    /**
     * Server-generated only — never accepted as a client→server event (see
     * {@code ChatWebSocketEndpoint#onMessage}'s allowlist, which only ever
     * dispatches {@code TOGGLE_REACTION}, never {@code REACTION_UPDATED}).
     * Exact payload shape is a locked design decision (Message Reaction
     * task, Decision 3) — do not add/rename/remove fields without
     * re-confirming: {@code type}, {@code messageId}, {@code myReaction}
     * (single emoji or JSON {@code null}), {@code reactions} (array of
     * {@code {emoji,count}}, possibly empty, never {@code null}).
     * <p>
     * {@code myReaction} is deliberately a parameter here, not derived from
     * {@code reactions} — it is the CALLER's own current reaction for ONE
     * specific recipient, not a shared value. {@link ChatWebSocketEndpoint}
     * calls this once per broadcast target, passing that target's own
     * {@code ChatService#getMyReaction} result each time, so two recipients
     * of the exact same broadcast receive two different serialized strings
     * for the same {@code reactions} aggregate. No other builder in this
     * file has this per-recipient shape (every other event broadcasts one
     * identical string to every target).
     */
    static String reactionUpdated(int messageId, String myReaction, List<MessageReactionSummary> reactions) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"REACTION_UPDATED\",")
                .append("\"messageId\":").append(messageId).append(',')
                .append("\"myReaction\":").append(ChatWsJson.quote(myReaction)).append(',')
                .append("\"reactions\":[");
        for (int i = 0; i < reactions.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            MessageReactionSummary summary = reactions.get(i);
            sb.append("{\"emoji\":").append(ChatWsJson.quote(summary.getEmoji()))
                    .append(",\"count\":").append(summary.getCount())
                    .append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    /** Friendly error only — never a stack trace, SQL statement, or internal class/file name (task spec §11). */
    static String error(String code, String message) {
        return "{\"type\":\"ERROR\",\"code\":" + ChatWsJson.quote(code) + ",\"message\":" + ChatWsJson.quote(message) + "}";
    }

    /**
     * Server-generated only — never accepted as a client→server event (see
     * {@code ChatWebSocketEndpoint#onMessage}'s allowlist, which only ever
     * dispatches {@code SEND_MESSAGE}). No sensitive field: accountId +
     * ONLINE/OFFLINE only, never password/email/role/session/IP (Presence
     * task spec §12).
     */
    static String presenceChanged(int accountId, String status) {
        return "{\"type\":\"PRESENCE_CHANGED\",\"accountId\":" + accountId
                + ",\"status\":" + ChatWsJson.quote(status) + "}";
    }

    /**
     * Server-generated only — never accepted as a client→server event (see
     * {@code ChatWebSocketEndpoint#onMessage}'s allowlist, which only ever
     * dispatches SEND_MESSAGE/MARK_READ, never READ_UPDATED). {@code accountId}
     * here is always the account that actually performed the Read, taken
     * from the authenticated session server-side — never the client's own
     * claim (Read/Seen task spec §10/§26).
     */
    static String readUpdated(int conversationId, int messageId, int accountId) {
        return "{\"type\":\"READ_UPDATED\",\"conversationId\":" + conversationId
                + ",\"messageId\":" + messageId
                + ",\"accountId\":" + accountId + "}";
    }

    /**
     * Server-generated only — never accepted as a client→server event (see
     * {@code ChatWebSocketEndpoint#onMessage}'s allowlist, which only ever
     * dispatches TYPING_START/TYPING_STOP, never TYPING_STARTED). {@code
     * accountId} is always the typing account resolved server-side from its
     * own session — never a client-supplied identity (Typing Indicator task
     * spec §11). Delivered only to the single DIRECT counterpart, never
     * broadcast, and never to the typing account's own other sessions.
     */
    static String typingStarted(int conversationId, int accountId) {
        return "{\"type\":\"TYPING_STARTED\",\"conversationId\":" + conversationId
                + ",\"accountId\":" + accountId + "}";
    }

    /** Same delivery/authorization rules as {@link #typingStarted}, for the stop edge. */
    static String typingStopped(int conversationId, int accountId) {
        return "{\"type\":\"TYPING_STOPPED\",\"conversationId\":" + conversationId
                + ",\"accountId\":" + accountId + "}";
    }

    /**
     * Server-generated only — never accepted as a client→server event; there
     * is no client→server Notification event by design (task spec §5/§18).
     * Only fields that exist on {@link Notification} and that the existing
     * notification UI actually renders (task spec §6/§11): no
     * recipientAccountId (delivery already scopes this event to the
     * recipient's own sessions only, via {@code NotificationBroadcaster}),
     * no {@code read} flag (a just-created Notification is always unread by
     * construction). {@code message}/{@code targetId}/{@code targetType} are
     * emitted as JSON {@code null} when absent, matching the model's own
     * nullable fields exactly rather than inventing a placeholder.
     */
    static String notificationCreated(Notification notification) {
        String createdAt = notification.getCreatedAt() != null ? notification.getCreatedAt().format(TIME_FORMAT) : "";

        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"NOTIFICATION_CREATED\",\"notification\":{")
                .append("\"notificationId\":").append(notification.getNotificationId()).append(',')
                .append("\"type\":").append(ChatWsJson.quote(notification.getType())).append(',')
                .append("\"title\":").append(ChatWsJson.quote(notification.getTitle())).append(',')
                .append("\"message\":").append(ChatWsJson.quote(notification.getMessage())).append(',')
                .append("\"targetId\":").append(notification.getTargetId() == null ? "null" : notification.getTargetId()).append(',')
                .append("\"targetType\":").append(ChatWsJson.quote(notification.getTargetType())).append(',')
                .append("\"createdAt\":").append(ChatWsJson.quote(createdAt))
                .append("}}");
        return sb.toString();
    }
}
