package com.gamenest.service;

import com.gamenest.dao.AccountDAO;
import com.gamenest.dao.AccountFollowDAO;
import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.DuplicateAccountFollowException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;

import java.sql.SQLException;
import java.util.List;

/**
 * Follow is one-way and needs no approval (task spec §1): a row in
 * AccountFollows means "following", no row means "not following". Unfollow
 * is a real DELETE (task spec §17) — AccountFollows is a pure junction
 * table like LFGMembers/AccountGames.
 */
public class AccountFollowService {

    private static final int PAGE_SIZE = 20;

    private final AccountFollowDAO accountFollowDAO;
    private final AccountDAO accountDAO;
    private final AccountService accountService;
    private final NotificationService notificationService;
    private final AccountBlockService accountBlockService;

    public AccountFollowService() {
        this.accountFollowDAO = new AccountFollowDAO();
        this.accountDAO = new AccountDAO();
        this.accountService = new AccountService();
        this.notificationService = new NotificationService();
        this.accountBlockService = new AccountBlockService();
    }

    /**
     * followerAccountId must come from the caller's session (task spec §6);
     * this method trusts whatever int it is given. targetUsername resolves
     * through {@link AccountService#getPublicProfile}, which already
     * enforces "exists and ACTIVE" — the same rule that gates whether the
     * target's Public Profile is even visible (task spec §9), so Follow can
     * never target an account nobody could otherwise view.
     */
    public void follow(int followerAccountId, String followerLabel, String targetUsername)
            throws ValidationException, AccountNotFoundException, DuplicateAccountFollowException, SQLException {

        Account target = accountService.getPublicProfile(targetUsername);
        if (target.getAccountId() == followerAccountId) {
            throw new ValidationException("Bạn không thể tự Follow chính mình.");
        }
        // Two-sided Block restriction (task spec §11) — never reveals which
        // side blocked whom, just refuses the action either way.
        if (accountBlockService.isBlockedBetween(followerAccountId, target.getAccountId())) {
            throw new ValidationException("Không thể thực hiện thao tác này với người dùng này.");
        }

        accountFollowDAO.insert(followerAccountId, target.getAccountId());

        // Best-effort side effect (NotificationService.create swallows its
        // own SQLException) — a notification failure never fails Follow,
        // which has already committed above (task spec §10/§17).
        notificationService.notifyFollow(target.getAccountId(), followerAccountId, followerLabel);
    }

    /**
     * followerAccountId must come from the caller's session (task spec §6).
     * Deliberately resolves the target via {@link AccountDAO#findByUsername}
     * directly rather than {@link AccountService#getPublicProfile} — Unfollow
     * must keep working even if the target account was deactivated after A
     * followed it (removing a stale relationship should never be blocked by
     * the target's current status); see the Important Decisions note in the
     * task's final report for why this intentionally differs from
     * {@link #follow}. No exception if the relationship doesn't exist —
     * that's just a no-op (task spec §5).
     */
    public void unfollow(int followerAccountId, String targetUsername)
            throws AccountNotFoundException, SQLException {

        Account target = accountDAO.findByUsername(targetUsername == null ? null : targetUsername.trim())
                .orElseThrow(() -> new AccountNotFoundException("Người dùng không tồn tại."));

        accountFollowDAO.delete(followerAccountId, target.getAccountId());
    }

    public boolean isFollowing(int followerAccountId, int followingAccountId) throws SQLException {
        return accountFollowDAO.exists(followerAccountId, followingAccountId);
    }

    public int countFollowers(int accountId) throws SQLException {
        return accountFollowDAO.countFollowers(accountId);
    }

    public int countFollowing(int accountId) throws SQLException {
        return accountFollowDAO.countFollowing(accountId);
    }

    public List<Account> listFollowers(int accountId, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return accountFollowDAO.listFollowers(accountId, offset, PAGE_SIZE);
    }

    public List<Account> listFollowing(int accountId, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return accountFollowDAO.listFollowing(accountId, offset, PAGE_SIZE);
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }
}
