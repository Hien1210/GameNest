package com.gamenest.websocket;

import com.gamenest.model.Notification;

import jakarta.websocket.Session;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridges {@code NotificationService} (a different package) to realtime
 * delivery over the existing {@code /ws/chat} connection (Notification
 * Realtime task spec §3/§7) — {@link ChatSessionRegistry} is package-private
 * and {@link ChatWebSocketEndpoint}'s send/broadcast helpers are private
 * instance methods, so this narrow public entry point is the only legal way
 * for the Notification write path to reach session delivery without moving
 * SQL/business rules into the WebSocket package (task spec §3/§24) or
 * exposing {@link ChatSessionRegistry} itself more broadly than it already
 * is.
 * <p>
 * Contains no SQL and no Notification business rule — only session lookup
 * plus a best-effort send, mirroring
 * {@code ChatWebSocketEndpoint#sendQuietly} exactly, so a broadcast failure
 * (closed tab, network drop) can never surface as an exception to the caller
 * (task spec §17/§19 Case 6). The caller is expected to invoke this only
 * after the Notification has already been durably inserted (task spec §7/§19
 * Case 1/Case 5); this class does not itself verify that, since it has no
 * access to persistence at all.
 */
public final class NotificationBroadcaster {

    private static final Logger LOGGER = Logger.getLogger(NotificationBroadcaster.class.getName());

    private NotificationBroadcaster() {
    }

    /**
     * Delivers {@code NOTIFICATION_CREATED} to every currently-open
     * WebSocket session belonging to exactly {@code notification}'s own
     * recipient (task spec §8) — reuses {@link ChatSessionRegistry}, the
     * same registry every other realtime event uses, so multi-tab delivery
     * (task spec §9) is automatic and no second session registry was
     * created. If the recipient has no open session (offline, or no page
     * with the Notification WebSocket module loaded), this is a silent
     * no-op: the Notification already exists in the DB and is picked up
     * normally on the next HTTP load (task spec §16) — nothing is queued or
     * retried here.
     */
    public static void broadcastCreated(Notification notification) {
        String payload = ChatWsProtocol.notificationCreated(notification);
        for (Session session : ChatSessionRegistry.getSessions(notification.getRecipientAccountId())) {
            sendQuietly(session, payload);
        }
    }

    private static void sendQuietly(Session session, String payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.getBasicRemote().sendText(payload);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to send NOTIFICATION_CREATED (session likely closing/closed)", e);
        }
    }
}
