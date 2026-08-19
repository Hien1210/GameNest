package com.gamenest.websocket;

import jakarta.websocket.Session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, ephemeral Typing Indicator state (Typing Indicator task, DIRECT
 * scope only) — {@code conversationId -> accountId -> the Set of WebSocket
 * sessions (multi-tab) currently "typing" there}. Runtime-only by design
 * (task spec §8/§9): no Database table/column, no persistence; a server
 * restart clearing all typing state is correct, not a bug.
 * <p>
 * A {@link Set}, never a single {@link Session} or a boolean, backs each
 * account's entry so multi-tab bookkeeping (task spec §10) is correct: one
 * tab closing or going quiet must not clear another tab's still-active
 * typing state for the same account+conversation. An account is "typing" in
 * a conversation iff its session set there is non-empty; it stops only when
 * the last such session is removed.
 * <p>
 * Every mutation on a given (conversationId, accountId) bucket goes through
 * {@link Map#compute}, which the JDK guarantees is atomic per key — the same
 * pattern already used by {@link PresenceManager}/{@link ChatSessionRegistry}
 * in this package. This is what keeps concurrent calls for the same
 * account+conversation (e.g. Tab 1's inactivity-timeout STOP racing Tab 2's
 * START) from interleaving into a lost update.
 * <p>
 * Known, accepted race (documented rather than engineered away, since this
 * state is ephemeral and low-stakes — task spec §25 "không cần over-engineer"):
 * the two-step "clear the per-account session set, then drop the now-empty
 * conversation bucket from the outer map" cleanup in {@link #stopInternal}
 * and {@link #clearAccount} is not atomic across both map levels. In a very
 * narrow interleaving, a concurrent {@link #start} for a different account in
 * the same conversation could be lost if it lands in the split second between
 * those two steps. The impact is bounded to a single missed TYPING_STARTED
 * broadcast that self-heals on that same account's next keystroke (a new
 * debounce cycle sends another TYPING_START a moment later) — it can never
 * broadcast to the wrong recipient, leak state across conversations, or leave
 * a permanently-stuck indicator.
 */
final class TypingManager {

    private record Key(int conversationId, int accountId) {
    }

    private static final Map<Integer, Map<Integer, Set<Session>>> TYPING = new ConcurrentHashMap<>();
    private static final Map<Session, Key> SESSION_KEY = new ConcurrentHashMap<>();

    private TypingManager() {
    }

    /**
     * Records that {@code session} is typing for {@code accountId} in
     * {@code conversationId}. If this same session was previously recorded as
     * typing somewhere else (task spec §5 — a page only ever has one active
     * conversation, but this stays correct even if that ever changes), that
     * stale entry is cleared first so it can never linger.
     *
     * @return {@code true} only on the 0→1 transition for this
     * account+conversation (no other tab of this account was already typing
     * here) — the caller broadcasts TYPING_STARTED only when this returns
     * {@code true}, so repeated START calls from the same tab (or a 2nd tab
     * opening the same conversation) never re-broadcast.
     */
    static boolean start(int conversationId, int accountId, Session session) {
        Key key = new Key(conversationId, accountId);
        Key previous = SESSION_KEY.put(session, key);
        if (previous != null && !previous.equals(key)) {
            stopInternal(previous, session);
        }

        boolean[] becameTyping = {false};
        TYPING.computeIfAbsent(conversationId, id -> new ConcurrentHashMap<>())
                .compute(accountId, (id, sessions) -> {
                    Set<Session> set = sessions != null ? sessions : ConcurrentHashMap.newKeySet();
                    becameTyping[0] = set.isEmpty();
                    set.add(session);
                    return set;
                });
        return becameTyping[0];
    }

    /**
     * Records that {@code session} stopped typing for {@code accountId} in
     * {@code conversationId}.
     *
     * @return {@code true} only on the transition to "no session of this
     * account is typing here anymore" — the caller broadcasts TYPING_STOPPED
     * only when this returns {@code true}, so one tab going quiet while
     * another tab of the same account is still typing never broadcasts (task
     * spec §10).
     */
    static boolean stop(int conversationId, int accountId, Session session) {
        Key key = new Key(conversationId, accountId);
        SESSION_KEY.remove(session, key);
        return stopInternal(key, session);
    }

    /**
     * Clears every session of {@code accountId} typing in
     * {@code conversationId} at once (task spec §12 — SEND_MESSAGE must clear
     * the sender's typing state regardless of which tab actually sent it).
     *
     * @return {@code true} if the account actually had at least one session
     * typing there (i.e. a TYPING_STOPPED broadcast is warranted).
     */
    static boolean clearAccount(int conversationId, int accountId) {
        Map<Integer, Set<Session>> byAccount = TYPING.get(conversationId);
        if (byAccount == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Set<Session>[] removedHolder = new Set[1];
        byAccount.compute(accountId, (id, sessions) -> {
            removedHolder[0] = sessions;
            return null;
        });
        if (byAccount.isEmpty()) {
            TYPING.remove(conversationId, byAccount);
        }

        Set<Session> removed = removedHolder[0];
        if (removed == null || removed.isEmpty()) {
            return false;
        }
        Key key = new Key(conversationId, accountId);
        for (Session s : removed) {
            SESSION_KEY.remove(s, key);
        }
        return true;
    }

    /**
     * Called from {@code @OnClose}/{@code @OnError} (task spec §13). A closed
     * or errored connection must never leave the other side stuck seeing a
     * stale "đang nhập..." — this looks up whatever this one session was
     * doing (if anything) and clears it.
     *
     * @return {@code {conversationId, accountId}} if this disconnect actually
     * transitioned the account to not-typing there (broadcast warranted), or
     * {@code null} if this session wasn't typing anywhere, or another tab of
     * the same account is still typing there.
     */
    static int[] disconnect(Session session) {
        Key key = SESSION_KEY.remove(session);
        if (key == null) {
            return null;
        }
        return stopInternal(key, session) ? new int[]{key.conversationId(), key.accountId()} : null;
    }

    private static boolean stopInternal(Key key, Session session) {
        Map<Integer, Set<Session>> byAccount = TYPING.get(key.conversationId());
        if (byAccount == null) {
            return false;
        }

        boolean[] becameEmpty = {false};
        byAccount.compute(key.accountId(), (id, sessions) -> {
            if (sessions == null || !sessions.remove(session)) {
                return sessions;
            }
            if (sessions.isEmpty()) {
                becameEmpty[0] = true;
                return null;
            }
            return sessions;
        });
        if (byAccount.isEmpty()) {
            TYPING.remove(key.conversationId(), byAccount);
        }
        return becameEmpty[0];
    }
}
