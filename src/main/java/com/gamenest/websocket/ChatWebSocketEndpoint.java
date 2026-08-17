package com.gamenest.websocket;

import com.gamenest.exception.ConversationNotFoundException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.TeamNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Conversation;
import com.gamenest.model.Message;
import com.gamenest.model.PresenceStatus;
import com.gamenest.service.ChatService;
import com.gamenest.service.PresenceService;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REALTIME TRANSPORT LAYER ONLY (task spec §2) — this class never decides
 * whether a message is allowed. It authenticates the connection from the
 * HTTP session (never from anything the client sends), parses the incoming
 * event, and delegates every business decision — membership, Friend/Block,
 * Team ACTIVE, content validation, persistence — to
 * {@link ChatService#sendMessage}, exactly the same method
 * {@link com.gamenest.controller.ChatSendServlet} already calls over HTTP.
 * No SQL runs in this class; no Friend/Block/Team rule is re-implemented
 * here.
 * <p>
 * Also drives Presence (User Online Presence task): the SAME connection
 * lifecycle that registers a session for Chat delivery
 * ({@link ChatSessionRegistry}) also registers it with
 * {@link PresenceManager}, independently — Presence is not derived from
 * {@code SEND_MESSAGE} in any way, only from connect/disconnect, so a user
 * who never sends a message is still correctly tracked as ONLINE. No second
 * WebSocket endpoint/authentication mechanism was created for this.
 * <p>
 * Also carries Realtime Read/Seen (Read/Seen task): {@code MARK_READ}
 * delegates to the exact same {@link ChatService#markAsRead} that
 * {@link com.gamenest.controller.ChatReadServlet} already calls over HTTP —
 * the forward-only read ratchet and its guarded silent no-op on an
 * invalid/stale messageId are unchanged, only the transport is new.
 */
@ServerEndpoint(value = "/ws/chat", configurator = ChatHandshakeConfigurator.class)
public class ChatWebSocketEndpoint {

    private static final Logger LOGGER = Logger.getLogger(ChatWebSocketEndpoint.class.getName());
    private static final String ACCOUNT_ID_PROPERTY = "accountId";

    private final ChatService chatService = new ChatService();
    private final PresenceService presenceService = new PresenceService();

    /** Set once by {@link ChatHandshakeConfigurator#getEndpointInstance} right after this instance is created for this one connection, before {@code @OnOpen} runs. */
    private volatile HttpSession handshakeHttpSession;

    void assignHttpSession(HttpSession httpSession) {
        this.handshakeHttpSession = httpSession;
    }

    /**
     * accountId comes exclusively from the authenticated HTTP session
     * captured at handshake (task spec §5) — never from a query parameter,
     * never from anything the client sends. No authenticated accountId →
     * reject the connection outright; no anonymous WebSocket Chat.
     */
    @OnOpen
    public void onOpen(Session session) {
        Integer accountId = resolveAccountId();
        if (accountId == null) {
            closeQuietly(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthenticated");
            return;
        }
        session.getUserProperties().put(ACCOUNT_ID_PROPERTY, accountId);
        ChatSessionRegistry.register(accountId, session);
        boolean becameOnline = PresenceManager.connect(accountId, session);
        sendQuietly(session, ChatWsProtocol.connectionReady());
        // Only the OFFLINE→ONLINE transition broadcasts (task spec §9/§11)
        // — opening a 2nd/3rd tab for an already-ONLINE account must not.
        if (becameOnline) {
            broadcastPresenceChange(accountId, PresenceStatus.ONLINE);
        }
    }

    /**
     * Allowlist dispatch (task spec §26): only SEND_MESSAGE and MARK_READ
     * are valid client→server events. Everything else — including a client
     * trying to forge a server-only event like READ_UPDATED,
     * MESSAGE_CREATED, PRESENCE_CHANGED, or CONNECTION_READY — falls
     * through to the INVALID_MESSAGE branch and is discarded before it can
     * reach any business logic.
     */
    @OnMessage
    public void onMessage(Session session, String rawMessage) {
        Integer accountId = (Integer) session.getUserProperties().get(ACCOUNT_ID_PROPERTY);
        if (accountId == null) {
            closeQuietly(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthenticated");
            return;
        }

        Map<String, String> event = ChatWsJson.parseFlatObject(rawMessage);
        String type = event == null ? null : event.get("type");

        if ("SEND_MESSAGE".equals(type)) {
            dispatchSendMessage(session, accountId, event);
        } else if ("MARK_READ".equals(type)) {
            dispatchMarkRead(session, accountId, event);
        } else {
            sendQuietly(session, ChatWsProtocol.error("INVALID_MESSAGE", "Sự kiện không hợp lệ."));
        }
    }

    private void dispatchSendMessage(Session session, int accountId, Map<String, String> event) {
        Integer conversationId = parseIntOrNull(event.get("conversationId"));
        if (conversationId == null) {
            sendQuietly(session, ChatWsProtocol.error("INVALID_MESSAGE", "Thiếu hoặc sai định dạng conversationId."));
            return;
        }
        // event may also contain a client-supplied "senderAccountId" — it is
        // never read; accountId above (from the authenticated session) is
        // the only sender identity ever used (task spec §16/§20 TEST 9).
        String content = event.get("content");

        handleSendMessage(session, accountId, conversationId, content);
    }

    private void dispatchMarkRead(Session session, int accountId, Map<String, String> event) {
        Integer conversationId = parseIntOrNull(event.get("conversationId"));
        Integer messageId = parseIntOrNull(event.get("messageId"));
        if (conversationId == null || messageId == null) {
            sendQuietly(session, ChatWsProtocol.error("INVALID_MESSAGE", "Thiếu hoặc sai định dạng conversationId/messageId."));
            return;
        }
        // event may also contain a client-supplied "accountId" — it is
        // never read; accountId above (from the authenticated session) is
        // the only reader identity ever used (Read/Seen task spec §5/§26).
        handleMarkRead(session, accountId, conversationId, messageId);
    }

    @OnClose
    public void onClose(Session session) {
        unregisterQuietly(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOGGER.log(Level.WARNING, "Chat WebSocket error", throwable);
        unregisterQuietly(session);
    }

    // ---- Send flow — every decision delegated to ChatService (task spec §7/§8/§16) ----

    private void handleSendMessage(Session session, int accountId, int conversationId, String content) {
        try {
            Message message = chatService.sendMessage(conversationId, accountId, content);
            String payload = ChatWsProtocol.messageCreated(message);
            broadcast(resolveBroadcastTargets(conversationId, accountId), payload);

        } catch (ForbiddenException e) {
            sendQuietly(session, ChatWsProtocol.error("FORBIDDEN", e.getMessage()));

        } catch (ValidationException e) {
            sendQuietly(session, ChatWsProtocol.error("INVALID_MESSAGE", e.getMessage()));

        } catch (ConversationNotFoundException e) {
            sendQuietly(session, ChatWsProtocol.error("NOT_FOUND", e.getMessage()));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while sending WebSocket chat message", e);
            sendQuietly(session, ChatWsProtocol.error("SERVER_ERROR", "Đã có lỗi xảy ra, vui lòng thử lại sau."));
        }
    }

    /**
     * Who should receive a realtime event about this conversation — the
     * acting account's own other tabs plus every current recipient (Chat
     * Message task spec §9; reused as-is for Read/Seen task spec §11/§13 —
     * "DIRECT: conversation members", "TEAM: TeamService.listMembers", both
     * already exactly what {@link ChatService#getRecipientAccountIds}
     * resolves, so no second membership-resolution helper was written for
     * MARK_READ). The underlying DB write is already committed by the time
     * this runs, so if recipient resolution itself fails (a narrow, benign
     * race — e.g. the Team was deleted a moment later), the already-durable
     * state simply isn't pushed realtime this one time; it will still be
     * correct on the next HTTP load (task spec §26 — documented as a benign
     * race, not "fixed" with new architecture).
     */
    private Set<Integer> resolveBroadcastTargets(int conversationId, int accountId) {
        Set<Integer> targets = new HashSet<>();
        targets.add(accountId);
        try {
            Conversation conversation = chatService.getConversation(conversationId);
            targets.addAll(chatService.getRecipientAccountIds(conversation, accountId));
        } catch (ConversationNotFoundException | TeamNotFoundException | ForbiddenException | SQLException e) {
            LOGGER.log(Level.WARNING, "Could not resolve broadcast recipients for conversation " + conversationId, e);
        }
        return targets;
    }

    private void broadcast(Set<Integer> targetAccountIds, String payload) {
        for (int targetAccountId : targetAccountIds) {
            for (Session targetSession : ChatSessionRegistry.getSessions(targetAccountId)) {
                sendQuietly(targetSession, payload);
            }
        }
    }

    // ---- Mark-read flow — reuses ChatService.markAsRead, the SAME method
    // ChatReadServlet already calls over HTTP (task spec §7/§28); no SQL,
    // no Friend/Block/Team rule, and no membership-resolution logic is
    // duplicated in this class. ----

    private void handleMarkRead(Session session, int accountId, int conversationId, int messageId) {
        try {
            int updated = chatService.markAsRead(conversationId, accountId, messageId);
            if (updated > 0) {
                String payload = ChatWsProtocol.readUpdated(conversationId, messageId, accountId);
                broadcast(resolveBroadcastTargets(conversationId, accountId), payload);
            }
            // updated == 0: messageId doesn't belong to this conversation,
            // or the forward-only ratchet didn't advance (already read up
            // to this point or further) — a guarded silent no-op, exactly
            // matching the existing HTTP behavior (task spec §9/§24): no
            // ERROR event, no broadcast.

        } catch (ForbiddenException e) {
            sendQuietly(session, ChatWsProtocol.error("FORBIDDEN", e.getMessage()));

        } catch (ConversationNotFoundException e) {
            sendQuietly(session, ChatWsProtocol.error("NOT_FOUND", e.getMessage()));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while marking WebSocket chat read", e);
            sendQuietly(session, ChatWsProtocol.error("SERVER_ERROR", "Đã có lỗi xảy ra, vui lòng thử lại sau."));
        }
    }

    // ---- Helpers ----

    private Integer resolveAccountId() {
        HttpSession httpSession = this.handshakeHttpSession;
        if (httpSession == null) {
            return null;
        }
        Object accountId;
        try {
            accountId = httpSession.getAttribute(ACCOUNT_ID_PROPERTY);
        } catch (IllegalStateException e) {
            // HttpSession was invalidated (e.g. logout) between handshake and onOpen.
            return null;
        }
        return accountId instanceof Integer ? (Integer) accountId : null;
    }

    /**
     * Shared by {@code @OnClose} and {@code @OnError} (task spec §10 — both
     * must cleanup). Idempotent by construction: {@link PresenceManager#disconnect}
     * is backed by a {@link java.util.Set}, so calling this twice for the
     * same session (e.g. if a container ever fired both callbacks for one
     * connection) cannot double-decrement or broadcast OFFLINE twice.
     */
    private void unregisterQuietly(Session session) {
        Object accountId = session.getUserProperties().get(ACCOUNT_ID_PROPERTY);
        if (accountId instanceof Integer id) {
            ChatSessionRegistry.unregister(id, session);
            boolean becameOffline = PresenceManager.disconnect(id, session);
            if (becameOffline) {
                broadcastPresenceChange(id, PresenceStatus.OFFLINE);
            }
        }
    }

    /**
     * PRESENCE_CHANGED recipients (task spec §13/§15): accountId's Friends,
     * minus anyone currently Blocked (either direction) — reuses
     * {@link PresenceService#getPresenceBroadcastTargets} as-is, no
     * Friend/Block logic duplicated here. Delivered to every one of each
     * recipient's currently-open sessions (multi-tab), via
     * {@link PresenceManager#getSessions}, never via
     * {@link ChatSessionRegistry} (a different registry for a different
     * purpose — task spec §5).
     */
    private void broadcastPresenceChange(int accountId, String status) {
        String payload = ChatWsProtocol.presenceChanged(accountId, status);
        List<Integer> targets;
        try {
            targets = presenceService.getPresenceBroadcastTargets(accountId);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not resolve Presence broadcast targets for accountId " + accountId, e);
            return;
        }
        for (int targetAccountId : targets) {
            for (Session targetSession : PresenceManager.getSessions(targetAccountId)) {
                sendQuietly(targetSession, payload);
            }
        }
    }

    private void sendQuietly(Session session, String payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.getBasicRemote().sendText(payload);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to send WebSocket message (session likely closing/closed)", e);
        }
    }

    private void closeQuietly(Session session, CloseReason.CloseCodes code, String reason) {
        try {
            session.close(new CloseReason(code, reason));
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to close WebSocket session", e);
        }
    }

    private Integer parseIntOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
