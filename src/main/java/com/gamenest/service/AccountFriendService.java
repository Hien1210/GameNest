package com.gamenest.service;

import com.gamenest.dao.AccountFriendshipDAO;
import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.DuplicateFriendRequestException;
import com.gamenest.exception.FriendRequestNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;
import com.gamenest.model.AccountFriendship;
import com.gamenest.model.FriendshipStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Friend is a request/accept workflow, distinct from the one-way, no-approval
 * Follow relationship (see AccountFollowService) — they intentionally share
 * no code and one never implies the other (task spec §22: no auto-follow on
 * friend, no auto-unfollow on friend/unfriend; not inferred here since no
 * such rule exists in the codebase). AccountFriendships never has a hard
 * DELETE — every transition is a guarded conditional UPDATE, enforced here
 * and backed by AccountFriendshipDAO's WHERE-guarded UPDATEs so ownership
 * and state checks can never be bypassed by skipping this Service.
 */
public class AccountFriendService {

    private static final Logger LOGGER = Logger.getLogger(AccountFriendService.class.getName());
    private static final int PAGE_SIZE = 20;

    private final AccountFriendshipDAO accountFriendshipDAO;
    private final AccountService accountService;
    private final NotificationService notificationService;
    private final AccountBlockService accountBlockService;

    public AccountFriendService() {
        this.accountFriendshipDAO = new AccountFriendshipDAO();
        this.accountService = new AccountService();
        this.notificationService = new NotificationService();
        this.accountBlockService = new AccountBlockService();
    }

    /**
     * requesterAccountId must come from the caller's session (task spec §5).
     * targetUsername resolves through {@link AccountService#getPublicProfile}
     * (exists + ACTIVE), same rule Follow already uses. Checks, in order:
     * self-request, already-ACCEPTED, PENDING in either direction — then
     * either reuses a terminal-status row for this exact direction (re-friend,
     * see db/17_account_friendships.sql) or inserts a fresh one.
     */
    public void sendRequest(int requesterAccountId, String requesterLabel, String targetUsername)
            throws ValidationException, AccountNotFoundException, DuplicateFriendRequestException, SQLException {

        Account target = accountService.getPublicProfile(targetUsername);
        int receiverAccountId = target.getAccountId();

        if (receiverAccountId == requesterAccountId) {
            throw new ValidationException("Bạn không thể tự kết bạn với chính mình.");
        }
        // Two-sided Block restriction (task spec §12) — never reveals which
        // side blocked whom, just refuses the action either way.
        if (accountBlockService.isBlockedBetween(requesterAccountId, receiverAccountId)) {
            throw new ValidationException("Không thể thực hiện thao tác này với người dùng này.");
        }

        Optional<AccountFriendship> active = accountFriendshipDAO.findActiveBetween(requesterAccountId, receiverAccountId);
        if (active.isPresent()) {
            AccountFriendship existing = active.get();
            if (FriendshipStatus.ACCEPTED.equals(existing.getStatus())) {
                throw new ValidationException("Hai bạn đã là bạn bè.");
            }
            // PENDING, either direction.
            if (existing.getRequesterAccountId() == requesterAccountId) {
                throw new ValidationException("Bạn đã gửi lời mời kết bạn cho người này rồi.");
            }
            throw new ValidationException("Người này đã gửi lời mời kết bạn cho bạn. Hãy vào mục Lời mời để phản hồi.");
        }

        Optional<AccountFriendship> existingPair = accountFriendshipDAO.findByPair(requesterAccountId, receiverAccountId);
        if (existingPair.isPresent()) {
            // Re-friend: a REJECTED/CANCELLED/UNFRIENDED row already exists
            // for this exact direction — reuse it (UNIQUE forbids a second
            // INSERT for the same pair).
            int updated = accountFriendshipDAO.reactivateAsPending(existingPair.get().getFriendshipId());
            if (updated == 0) {
                // Row changed state concurrently (e.g. reused by a parallel
                // request) between the read above and this UPDATE.
                throw new ValidationException("Không thể gửi lời mời lúc này, vui lòng thử lại.");
            }
        } else {
            accountFriendshipDAO.insert(requesterAccountId, receiverAccountId);
        }

        notificationService.notifyFriendRequest(receiverAccountId, requesterAccountId, requesterLabel);
    }

    public void acceptRequest(int friendshipId, int receiverAccountId, String receiverLabel)
            throws FriendRequestNotFoundException, SQLException {

        int updated = accountFriendshipDAO.acceptIfPending(friendshipId, receiverAccountId);
        if (updated == 0) {
            throw new FriendRequestNotFoundException(
                    "Lời mời kết bạn không tồn tại, không thuộc về bạn, hoặc đã được xử lý.");
        }
        notifyAccepted(friendshipId, receiverAccountId, receiverLabel);
    }

    public void rejectRequest(int friendshipId, int receiverAccountId) throws FriendRequestNotFoundException, SQLException {
        int updated = accountFriendshipDAO.rejectIfPending(friendshipId, receiverAccountId);
        if (updated == 0) {
            throw new FriendRequestNotFoundException(
                    "Lời mời kết bạn không tồn tại, không thuộc về bạn, hoặc đã được xử lý.");
        }
        // No notification on reject (task spec §7).
    }

    public void cancelRequest(int friendshipId, int requesterAccountId) throws FriendRequestNotFoundException, SQLException {
        int updated = accountFriendshipDAO.cancelIfPending(friendshipId, requesterAccountId);
        if (updated == 0) {
            throw new FriendRequestNotFoundException(
                    "Lời mời kết bạn không tồn tại, không thuộc về bạn, hoặc đã được xử lý.");
        }
        // No notification on cancel (task spec §8).
    }

    public void unfriend(int friendshipId, int currentAccountId) throws FriendRequestNotFoundException, SQLException {
        int updated = accountFriendshipDAO.unfriendIfAccepted(friendshipId, currentAccountId);
        if (updated == 0) {
            throw new FriendRequestNotFoundException("Không tìm thấy quan hệ bạn bè phù hợp để hủy.");
        }
        // No notification on unfriend (task spec §9).
    }

    /**
     * The PENDING or ACCEPTED row between the two accounts, if any — used by
     * PublicProfileServlet to derive the NONE/OUTGOING_PENDING/
     * INCOMING_PENDING/FRIEND button state server-side (task spec §17). A
     * REJECTED/CANCELLED/UNFRIENDED row is deliberately not returned here —
     * from the viewer's perspective that relationship is effectively NONE
     * (re-friendable), matching sendRequest's own reuse logic above.
     */
    public Optional<AccountFriendship> findActiveBetween(int accountId1, int accountId2) throws SQLException {
        return accountFriendshipDAO.findActiveBetween(accountId1, accountId2);
    }

    public List<AccountFriendship> listFriends(int accountId, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return accountFriendshipDAO.listFriends(accountId, offset, PAGE_SIZE);
    }

    public int countFriends(int accountId) throws SQLException {
        return accountFriendshipDAO.countFriends(accountId);
    }

    /** Unpaginated friend account ids — realtime Presence broadcast targeting (task spec §14), not a UI list. Reuses {@link AccountFriendshipDAO#listFriendAccountIds} as-is. */
    public List<Integer> listFriendAccountIds(int accountId) throws SQLException {
        return accountFriendshipDAO.listFriendAccountIds(accountId);
    }

    public List<AccountFriendship> listIncomingRequests(int accountId, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return accountFriendshipDAO.listIncomingRequests(accountId, offset, PAGE_SIZE);
    }

    public int countIncomingRequests(int accountId) throws SQLException {
        return accountFriendshipDAO.countIncomingRequests(accountId);
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    /**
     * Best-effort side effect, same guarantee as ReportService#notifyReporter:
     * a failure to even look the row back up (let alone the notification
     * INSERT itself, already best-effort inside NotificationService) must
     * never surface as a failure of the Accept that already committed above.
     */
    private void notifyAccepted(int friendshipId, int receiverAccountId, String receiverLabel) {
        try {
            accountFriendshipDAO.findById(friendshipId).ifPresent(f ->
                    notificationService.notifyFriendAccepted(f.getRequesterAccountId(), receiverAccountId, receiverLabel));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load friendship for notification (accept already committed): "
                    + "friendshipId=" + friendshipId, e);
        }
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }
}
