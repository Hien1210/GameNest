package com.gamenest.websocket;

import jakarta.websocket.Session;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * accountId → currently-open WebSocket session(s) (task spec §6) — a pure
 * in-memory delivery registry for realtime broadcast, scoped to this one
 * Tomcat instance. It holds no persistent state and implies nothing about
 * "online/offline presence" (out of scope for this task) — it only ever
 * means "this session is currently connected to the Chat WebSocket
 * endpoint." A single account can have multiple concurrent sessions
 * (multiple tabs/browsers), so each account maps to a Set, never a single
 * Session.
 */
final class ChatSessionRegistry {

    private static final Map<Integer, Set<Session>> SESSIONS_BY_ACCOUNT = new ConcurrentHashMap<>();

    private ChatSessionRegistry() {
    }

    static void register(int accountId, Session session) {
        SESSIONS_BY_ACCOUNT
                .computeIfAbsent(accountId, id -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    /**
     * Uses {@link Map#compute} (atomic per-key under ConcurrentHashMap)
     * rather than a separate "remove from set, then remove the key if
     * empty" sequence — the two-step version would race with a concurrent
     * {@link #register} on the same account landing between the emptiness
     * check and the key removal, silently dropping a session that had just
     * been added a moment earlier.
     */
    static void unregister(int accountId, Session session) {
        SESSIONS_BY_ACCOUNT.compute(accountId, (id, sessions) -> {
            if (sessions == null) {
                return null;
            }
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    /** Snapshot copy — safe to iterate for broadcast even if the account's sessions change concurrently mid-loop (task spec §18). */
    static Set<Session> getSessions(int accountId) {
        Set<Session> sessions = SESSIONS_BY_ACCOUNT.get(accountId);
        return sessions == null ? Collections.emptySet() : Set.copyOf(sessions);
    }
}
