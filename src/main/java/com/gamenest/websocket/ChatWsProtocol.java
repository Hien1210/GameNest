package com.gamenest.websocket;

import com.gamenest.model.Message;

import java.time.format.DateTimeFormatter;

/**
 * Server→client event builders for the Chat WebSocket protocol (task spec
 * §12/§13, extended with PRESENCE_CHANGED per the Presence task and
 * READ_UPDATED per the Realtime Read/Seen task). No TYPING/NOTIFICATION
 * event exists here, by scope. {@code createdAt} is pre-formatted
 * server-side with the exact same pattern {@code account/chat-detail.jsp}
 * already uses, so the client never has to parse/format a timestamp
 * itself.
 */
final class ChatWsProtocol {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

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
     */
    static String messageCreated(Message message) {
        String senderDisplayName = message.getSenderDisplayName();
        String createdAt = message.getCreatedAt() != null ? message.getCreatedAt().format(TIME_FORMAT) : "";

        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"MESSAGE_CREATED\",\"message\":{")
                .append("\"messageId\":").append(message.getMessageId()).append(',')
                .append("\"conversationId\":").append(message.getConversationId()).append(',')
                .append("\"senderAccountId\":").append(message.getSenderAccountId()).append(',')
                .append("\"senderUsername\":").append(ChatWsJson.quote(message.getSenderUsername())).append(',')
                .append("\"senderDisplayName\":").append(ChatWsJson.quote(senderDisplayName)).append(',')
                .append("\"content\":").append(ChatWsJson.quote(message.getContent())).append(',')
                .append("\"createdAt\":").append(ChatWsJson.quote(createdAt))
                .append("}}");
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
}
