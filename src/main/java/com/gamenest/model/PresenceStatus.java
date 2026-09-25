package com.gamenest.model;

/**
 * Realtime connection presence — deliberately only 2 values (task spec §3):
 * no AWAY/BUSY/DO_NOT_DISTURB/INVISIBLE/IDLE/LAST_SEEN. Not an Account
 * status (see {@link AccountStatus}) — this is pure runtime state, never
 * persisted (task spec §4/§17).
 */
public final class PresenceStatus {

    public static final String ONLINE = "ONLINE";
    public static final String OFFLINE = "OFFLINE";

    private PresenceStatus() {
    }
}
