package com.gamenest.service;

import com.gamenest.model.AccountFriendship;
import com.gamenest.model.FriendshipStatus;
import com.gamenest.websocket.PresenceManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Presence visibility business logic — the single place that combines
 * runtime online/offline state ({@link PresenceManager}) with WHO is
 * allowed to see it. Presence is scoped to User↔User (Friend) relationships
 * only in this task (task spec §13); a Team's members are not covered.
 * Reuses {@link AccountFriendService}/{@link AccountBlockService} exactly
 * as-is — no Friend/Block SQL or business rule is duplicated here, matching
 * {@link ChatService}'s own reuse discipline for the same two services.
 */
public class PresenceService {

    private final AccountFriendService accountFriendService;
    private final AccountBlockService accountBlockService;

    public PresenceService() {
        this.accountFriendService = new AccountFriendService();
        this.accountBlockService = new AccountBlockService();
    }

    /**
     * Whether viewerAccountId may see targetAccountId's Presence, and if so,
     * its current runtime status. Visible only when they are ACCEPTED
     * Friends and not Blocked in either direction (task spec §13/§15) — the
     * exact same rule Direct Chat already enforces, reusing
     * {@link AccountFriendService#findActiveBetween}/{@link AccountBlockService#isBlockedBetween}
     * as-is. Returns {@code null} when not visible (not Friends, or
     * Blocked) — the caller must render a neutral/absent state, never
     * "Offline" as a stand-in for "hidden" (viewing your own account is
     * always visible, bypassing the Friend check, since there is nothing to
     * hide from yourself).
     */
    public Boolean getVisiblePresence(int viewerAccountId, int targetAccountId) throws SQLException {
        if (viewerAccountId == targetAccountId) {
            return PresenceManager.isOnline(targetAccountId);
        }
        Optional<AccountFriendship> friendship = accountFriendService.findActiveBetween(viewerAccountId, targetAccountId);
        if (friendship.isEmpty() || !FriendshipStatus.ACCEPTED.equals(friendship.get().getStatus())) {
            return null;
        }
        if (accountBlockService.isBlockedBetween(viewerAccountId, targetAccountId)) {
            return null;
        }
        return PresenceManager.isOnline(targetAccountId);
    }

    /**
     * Realtime PRESENCE_CHANGED broadcast targets for accountId (WebSocket
     * transport use only, task spec §13/§15): every ACCEPTED friend, minus
     * anyone currently Blocked in either direction. Under this project's
     * already-enforced invariant (Block always transitions an ACCEPTED
     * friendship to UNFRIENDED — see AccountBlockService#block — and a new
     * Friend request cannot be sent while Blocked), an ACCEPTED friendship
     * and an active Block between the same two accounts can never coexist,
     * so this Block check is technically redundant today; it is kept
     * anyway as an explicit, auditable defense-in-depth layer per this
     * task's own requirement, at the cost of one extra indexed lookup per
     * friend.
     */
    public List<Integer> getPresenceBroadcastTargets(int accountId) throws SQLException {
        List<Integer> friendIds = accountFriendService.listFriendAccountIds(accountId);
        List<Integer> visible = new ArrayList<>(friendIds.size());
        for (int friendId : friendIds) {
            if (!accountBlockService.isBlockedBetween(accountId, friendId)) {
                visible.add(friendId);
            }
        }
        return visible;
    }
}
