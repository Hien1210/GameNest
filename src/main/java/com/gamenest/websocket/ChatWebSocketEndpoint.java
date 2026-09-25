package com.gamenest.websocket;

import com.gamenest.exception.ConversationNotFoundException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.MessageNotFoundException;
import com.gamenest.exception.TeamNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Conversation;
import com.gamenest.model.Message;
import com.gamenest.model.PresenceStatus;
import com.gamenest.model.ReactionResult;
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
 * <p>
 * Also carries Typing Indicator (Typing Indicator task, DIRECT only):
 * {@code TYPING_START}/{@code TYPING_STOP} delegate authorization to
 * {@link ChatService#getTypingRecipient}, the same membership/DIRECT/
 * Friend-Block chain every other Chat operation uses — no rule is
 * re-implemented here. Typing state itself lives only in {@link TypingManager}
 * (in-memory, per-connection, never persisted); this class only decides
 * *whether* to broadcast, based on the 0→1/1→0 transitions
 * {@link TypingManager} reports, and *who* to broadcast to (the resolved
 * DIRECT counterpart's sessions in {@link ChatSessionRegistry} — never the
 * sender's own sessions, never a HTTP fallback, since Typing is explicitly
 * WebSocket-only and ephemeral).
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
     * Allowlist dispatch (task spec §26, extended by the Typing Indicator
     * task with TYPING_START/TYPING_STOP, and by the Message Reaction task
     * with TOGGLE_REACTION): only these values are valid client→server
     * events. Everything else — including a client trying to forge a
     * server-only event like READ_UPDATED, MESSAGE_CREATED,
     * PRESENCE_CHANGED, CONNECTION_READY, TYPING_STARTED, TYPING_STOPPED, or
     * REACTION_UPDATED — falls through to the INVALID_MESSAGE branch and is
     * discarded before it can reach any business logic. Note ADD_REACTION/
     * REMOVE_REACTION/CHANGE_REACTION are deliberately never accepted
     * either (Decision 2) — the server alone decides which of those a
     * TOGGLE_REACTION resolves to.
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
        } else if ("TYPING_START".equals(type)) {
            dispatchTypingStart(session, accountId, event);
        } else if ("TYPING_STOP".equals(type)) {
            dispatchTypingStop(session, accountId, event);
        } else if ("TOGGLE_REACTION".equals(type)) {
            dispatchToggleReaction(session, accountId, event);
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
        // Reply feature: optional, null when absent/invalid — not a reply,
        // never an error. Cross-conversation/nonexistent targets are
        // rejected inside ChatService.sendMessage, not here.
        Integer replyToMessageId = parseIntOrNull(event.get("replyToMessageId"));

        handleSendMessage(session, accountId, conversationId, content, replyToMessageId);
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

    private void dispatchTypingStart(Session session, int accountId, Map<String, String> event) {
        Integer conversationId = parseIntOrNull(event.get("conversationId"));
        if (conversationId == null) {
            sendQuietly(session, ChatWsProtocol.error("INVALID_MESSAGE", "Thiếu hoặc sai định dạng conversationId."));
            return;
        }
        // event may also contain a client-supplied "accountId" — never read,
        // same rule as every other client→server event (Typing Indicator
        // task spec §11).
        handleTypingStart(session, accountId, conversationId);
    }

    private void dispatchTypingStop(Session session, int accountId, Map<String, String> event) {
        Integer conversationId = parseIntOrNull(event.get("conversationId"));
        if (conversationId == null) {
            sendQuietly(session, ChatWsProtocol.error("INVALID_MESSAGE", "Thiếu hoặc sai định dạng conversationId."));
            return;
        }
        handleTypingStop(session, accountId, conversationId);
    }

    private void dispatchToggleReaction(Session session, int accountId, Map<String, String> event) {
        Integer messageId = parseIntOrNull(event.get("messageId"));
        String emoji = event.get("emoji");
        if (messageId == null || emoji == null) {
            sendQuietly(session, ChatWsProtocol.error("INVALID_MESSAGE", "Thiếu hoặc sai định dạng messageId/emoji."));
            return;
        }
        // event may also contain a client-supplied "accountId" — never read,
        // same rule as every other client→server event; the emoji allowlist
        // itself is re-validated inside ChatService.toggleReaction, not here.
        handleToggleReaction(session, accountId, messageId, emoji);
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

    private void handleSendMessage(Session session, int accountId, int conversationId, String content, Integer replyToMessageId) {
        try {
            Message message = chatService.sendMessage(conversationId, accountId, content, replyToMessageId);
            Set<Integer> targets = resolveBroadcastTargets(conversationId, accountId);
            broadcast(targets, ChatWsProtocol.messageCreated(message));

            // Sending a message clears the sender's own Typing state
            // (Typing Indicator task spec §12) independently of the
            // client's own JS-side clear, so a stale "đang nhập..." can
            // never survive on the recipient's screen if MESSAGE_CREATED
            // happens to arrive before an explicit TYPING_STOP. Reuses the
            // same target set already resolved for MESSAGE_CREATED (minus
            // the sender) instead of a second ChatService call —
            // TypingManager only ever holds an entry for a DIRECT
            // conversation whose recipient was already authorized at
            // TYPING_START time, so no re-authorization is needed here.
            if (TypingManager.clearAccount(conversationId, accountId)) {
                Set<Integer> typingStopTargets = new HashSet<>(targets);
                typingStopTargets.remove(accountId);
                broadcast(typingStopTargets, ChatWsProtocol.typingStopped(conversationId, accountId));
            }

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

    // ---- Reaction flow — reuses ChatService.toggleReaction, which itself
    // reuses the exact same authorization chain as sendMessage
    // (ChatService#requireSendAccess); no SQL, no Friend/Block/Team rule is
    // duplicated in this class (Message Reaction task, Decision 3/5). ----

    /**
     * REACTION_UPDATED is per-recipient by payload shape (Decision 3):
     * unlike every other broadcast in this class, the same
     * {@code reactions} aggregate is serialized once PER TARGET account,
     * each with that target's own current {@code myReaction} looked up
     * individually via {@link ChatService#getMyReaction}. This is why the
     * shared {@link #broadcast(Set, String)} helper (one string for every
     * target) cannot be reused unmodified for this one event type.
     */
    private void handleToggleReaction(Session session, int accountId, int messageId, String emoji) {
        try {
            ReactionResult result = chatService.toggleReaction(messageId, accountId, emoji);
            Set<Integer> targets = resolveBroadcastTargets(result.getConversationId(), accountId);
            for (int targetAccountId : targets) {
                String myReactionForTarget = targetAccountId == accountId
                        ? result.getMyReaction()
                        : chatService.getMyReaction(messageId, targetAccountId);
                String payload = ChatWsProtocol.reactionUpdated(messageId, myReactionForTarget, result.getReactions());
                for (Session targetSession : ChatSessionRegistry.getSessions(targetAccountId)) {
                    sendQuietly(targetSession, payload);
                }
            }

        } catch (MessageNotFoundException e) {
            sendQuietly(session, ChatWsProtocol.error("NOT_FOUND", e.getMessage()));

        } catch (ForbiddenException e) {
            sendQuietly(session, ChatWsProtocol.error("FORBIDDEN", e.getMessage()));

        } catch (ValidationException e) {
            sendQuietly(session, ChatWsProtocol.error("INVALID_MESSAGE", e.getMessage()));

        } catch (ConversationNotFoundException e) {
            sendQuietly(session, ChatWsProtocol.error("NOT_FOUND", e.getMessage()));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while toggling WebSocket chat reaction", e);
            sendQuietly(session, ChatWsProtocol.error("SERVER_ERROR", "Đã có lỗi xảy ra, vui lòng thử lại sau."));
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

    // ---- Typing Indicator flow (DIRECT only) — authorization delegated to
    // ChatService.getTypingRecipient (membership, DIRECT type, Friend/Block
    // — the exact same rules sendMessage uses), state transitions delegated
    // to TypingManager; this class only wires the two together and decides
    // who receives the resulting TYPING_STARTED/TYPING_STOPPED frame. ----

    /**
     * Typing events are ephemeral background signals, not user-initiated
     * actions (Typing Indicator task spec §17/§19) — unlike SEND_MESSAGE/
     * MARK_READ, a rejected authorization check here is never surfaced as an
     * ERROR frame (chat-realtime.js's existing handleError unconditionally
     * window.alert()s, which would be disruptive UX for a passive signal);
     * it is silently ignored instead. Only structurally malformed client
     * input (missing/non-numeric conversationId, handled in
     * {@link #dispatchTypingStart}) still gets INVALID_MESSAGE, matching the
     * same convention SEND_MESSAGE/MARK_READ already use for that case.
     */
    private void handleTypingStart(Session session, int accountId, int conversationId) {
        Integer recipientAccountId;
        try {
            recipientAccountId = chatService.getTypingRecipient(conversationId, accountId);
        } catch (ForbiddenException | ConversationNotFoundException | ValidationException e) {
            // Not a member, conversation missing/inactive, or not an
            // ACCEPTED-friend/blocked pair — safely ignored, no ERROR frame.
            return;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database error while resolving Typing Indicator recipient", e);
            return;
        }
        if (recipientAccountId == null) {
            // TEAM conversation, or no resolvable DIRECT counterpart — out
            // of scope for Typing Indicator by design; not an error.
            return;
        }
        if (TypingManager.start(conversationId, accountId, session)) {
            broadcastTyping(recipientAccountId, ChatWsProtocol.typingStarted(conversationId, accountId));
        }
        // TypingManager.start() returning false means another tab of this
        // same account is already recorded as typing here (task spec §10) —
        // correctly no re-broadcast.
    }

    /** Mirrors {@link #handleTypingStart}'s authorization/silence rules for the stop edge. */
    private void handleTypingStop(Session session, int accountId, int conversationId) {
        if (!TypingManager.stop(conversationId, accountId, session)) {
            // No 0-session transition for this account here (already
            // stopped, or another tab is still typing) — nothing to
            // broadcast, and no need to re-run authorization for a no-op.
            return;
        }
        try {
            Integer recipientAccountId = chatService.getTypingRecipient(conversationId, accountId);
            if (recipientAccountId != null) {
                broadcastTyping(recipientAccountId, ChatWsProtocol.typingStopped(conversationId, accountId));
            }
        } catch (ForbiddenException | ConversationNotFoundException | ValidationException e) {
            // Local typing state is already cleared regardless; if
            // authorization no longer holds (e.g. left the conversation
            // between START and STOP) there is simply nobody left to notify.
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database error while resolving Typing Indicator recipient", e);
        }
    }

    /**
     * Delivers a Typing frame to every open session of exactly one account
     * (Typing Indicator task spec §14) — never the typing account's own
     * sessions, never unrelated accounts, never the whole
     * {@link ChatSessionRegistry}/{@link PresenceManager} population. If the
     * recipient has no open session (offline / no tab on this page), this is
     * a silent no-op — Typing state is never queued or persisted for later
     * delivery (task spec §14).
     */
    private void broadcastTyping(int targetAccountId, String payload) {
        for (Session targetSession : ChatSessionRegistry.getSessions(targetAccountId)) {
            sendQuietly(targetSession, payload);
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
     * connection) cannot double-decrement or broadcast OFFLINE twice. Also
     * clears any Typing state this session held (Typing Indicator task spec
     * §13) — {@link TypingManager#disconnect} is likewise idempotent
     * (backed by a {@link java.util.Map#remove(Object)} keyed on the exact
     * session), so a duplicate call here is equally harmless.
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
        clearTypingOnDisconnect(session);
    }

    /**
     * A closed/errored connection must never leave the other side stuck
     * seeing a stale "đang nhập..." (Typing Indicator task spec §13).
     * {@link TypingManager#disconnect} reports the (conversationId,
     * accountId) pair only if this session's disconnect actually caused a
     * 1→0 transition for that account there (i.e. no other tab of the same
     * account is still typing) — a re-authorization check is still run
     * before broadcasting, mirroring {@link #handleTypingStop}, since the
     * connection could be closing precisely because access was revoked
     * (e.g. Block) a moment earlier.
     */
    private void clearTypingOnDisconnect(Session session) {
        int[] stopped = TypingManager.disconnect(session);
        if (stopped == null) {
            return;
        }
        int conversationId = stopped[0];
        int accountId = stopped[1];
        try {
            Integer recipientAccountId = chatService.getTypingRecipient(conversationId, accountId);
            if (recipientAccountId != null) {
                broadcastTyping(recipientAccountId, ChatWsProtocol.typingStopped(conversationId, accountId));
            }
        } catch (ForbiddenException | ConversationNotFoundException | ValidationException e) {
            // Local typing state is already cleared regardless; nothing left to notify.
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database error while resolving Typing Indicator recipient on disconnect", e);
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
