package com.gamenest.websocket;

import jakarta.websocket.Session;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime Presence registry (task spec §6/§17) — {@code accountId → active
 * WebSocket sessions}, purely in-memory, lost on server restart by design
 * (no Database persistence, no Redis, no clustering). Deliberately kept
 * independent of {@link ChatSessionRegistry}: that registry exists purely
 * for Chat message delivery routing, this one exists purely to track
 * connection-count-based ONLINE/OFFLINE transitions (task spec §5 — "Presence
 * phải hoạt động độc lập với Chat message"). Both are updated from the same
 * {@code ChatWebSocketEndpoint} connection lifecycle, but neither reads the
 * other's state.
 * <p>
 * An account is ONLINE iff it has at least one registered session; it
 * becomes OFFLINE only when its last session is removed (task spec §7).
 * {@link #connect}/{@link #disconnect} report whether that specific call
 * caused the 0→1 or 1→0 transition, which is what the caller must use to
 * decide whether to broadcast {@code PRESENCE_CHANGED} — a same-status
 * connect/disconnect (e.g. opening a 2nd tab, or closing 1 of 3 tabs) must
 * never broadcast (task spec §9/§11).
 * <p>
 * A {@link Set} (not a counter) backs each account's sessions, so a
 * duplicate {@link #disconnect} call for the same {@link Session} is
 * naturally idempotent — {@code Set#remove} on an already-absent element is
 * a safe no-op, unlike a raw counter which could be decremented twice (task
 * spec §10/§23 Race 4).
 */
public final class PresenceManager {

    private static final Map<Integer, Set<Session>> SESSIONS_BY_ACCOUNT = new ConcurrentHashMap<>();

    private PresenceManager() {
    }

    /** Registers this session under accountId. Returns {@code true} only on the OFFLINE→ONLINE transition (this was the account's first active session) — the caller broadcasts PRESENCE_CHANGED(ONLINE) only when this returns {@code true}. */
    public static boolean connect(int accountId, Session session) {
        boolean[] becameOnline = new boolean[1];
        SESSIONS_BY_ACCOUNT.compute(accountId, (id, sessions) -> {
            Set<Session> set = sessions != null ? sessions : ConcurrentHashMap.newKeySet();
            becameOnline[0] = set.isEmpty();
            set.add(session);
            return set;
        });
        return becameOnline[0];
    }

    /**
     * Unregisters this session. Returns {@code true} only on the
     * ONLINE→OFFLINE transition (no sessions remain for accountId after this
     * call actually removed one) — the caller broadcasts
     * PRESENCE_CHANGED(OFFLINE) only when this returns {@code true}. Uses
     * {@link Map#compute} (atomic per-key) so a concurrent {@link #connect}
     * for the same account can never be lost between the emptiness check and
     * the key removal (task spec §23 Race 3).
     */
    public static boolean disconnect(int accountId, Session session) {
        boolean[] becameOffline = new boolean[1];
        SESSIONS_BY_ACCOUNT.compute(accountId, (id, sessions) -> {
            if (sessions == null) {
                becameOffline[0] = false;
                return null;
            }
            boolean removed = sessions.remove(session);
            if (sessions.isEmpty()) {
                becameOffline[0] = removed;
                return null;
            }
            becameOffline[0] = false;
            return sessions;
        });
        return becameOffline[0];
    }

    public static boolean isOnline(int accountId) {
        Set<Session> sessions = SESSIONS_BY_ACCOUNT.get(accountId);
        return sessions != null && !sessions.isEmpty();
    }

    /** Which of the given account ids are currently online (task spec §16 — initial-state snapshot for a list UI, e.g. Friend List on page load). */
    public static Set<Integer> getOnlineAccountIds(Collection<Integer> accountIds) {
        Set<Integer> online = new HashSet<>();
        for (int accountId : accountIds) {
            if (isOnline(accountId)) {
                online.add(accountId);
            }
        }
        return online;
    }

    /** Snapshot copy of accountId's currently registered sessions — for PRESENCE_CHANGED delivery, safe to iterate even if sessions register/unregister concurrently mid-broadcast (task spec §23 Race 5). */
    public static Set<Session> getSessions(int accountId) {
        Set<Session> sessions = SESSIONS_BY_ACCOUNT.get(accountId);
        return sessions == null ? Collections.emptySet() : Set.copyOf(sessions);
    }
}
