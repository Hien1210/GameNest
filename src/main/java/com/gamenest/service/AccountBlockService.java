package com.gamenest.service;

import com.gamenest.dao.AccountBlockDAO;
import com.gamenest.dao.AccountDAO;
import com.gamenest.dao.AccountFollowDAO;
import com.gamenest.dao.AccountFriendshipDAO;
import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;
import com.gamenest.model.AccountBlock;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Block is a one-way row (blocker → blocked) that Follow/Friend must treat
 * as a two-sided restriction (task spec §1/§10) — {@link #isBlockedBetween}
 * is the single check both {@link AccountFollowService} and
 * {@link AccountFriendService} call before creating any new Follow/Friend
 * relationship. Blocking has side effects on the existing Social graph
 * (Follow cleanup, Friendship state transitions) that must commit atomically
 * with the AccountBlocks INSERT (task spec §8) — see {@link #block}.
 * Unblocking never restores anything it previously removed (task spec §9).
 */
public class AccountBlockService {

    private static final int PAGE_SIZE = 20;

    private final AccountBlockDAO accountBlockDAO;
    private final AccountFollowDAO accountFollowDAO;
    private final AccountFriendshipDAO accountFriendshipDAO;
    private final AccountDAO accountDAO;
    private final AccountService accountService;

    public AccountBlockService() {
        this.accountBlockDAO = new AccountBlockDAO();
        this.accountFollowDAO = new AccountFollowDAO();
        this.accountFriendshipDAO = new AccountFriendshipDAO();
        this.accountDAO = new AccountDAO();
        this.accountService = new AccountService();
    }

    /**
     * blockerAccountId must come from the caller's session (task spec §4);
     * this method trusts whatever int it is given. targetUsername resolves
     * through {@link AccountService#getPublicProfile} (exists + ACTIVE),
     * same rule Follow/Friend already use.
     * <p>
     * A duplicate Block (already blocking target) is a silent idempotent
     * no-op (task spec §6) — {@link AccountBlockDAO#insertIfAbsent} returns
     * {@code false} and the side-effect cleanup below is skipped entirely,
     * since a pre-existing block already removed any Follow/Friend
     * relationship the first time it was created.
     * <p>
     * INSERT + Follow cleanup + Friendship cleanup run in one transaction
     * (task spec §8, shared-Connection pattern already used by
     * AnswerService#acceptAnswer/LFGService) — either the whole Block
     * operation lands, or none of it does.
     */
    public void block(int blockerAccountId, String targetUsername)
            throws ValidationException, AccountNotFoundException, SQLException {

        Account target = accountService.getPublicProfile(targetUsername);
        int blockedAccountId = target.getAccountId();
        if (blockedAccountId == blockerAccountId) {
            throw new ValidationException("Bạn không thể tự chặn chính mình.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean inserted = accountBlockDAO.insertIfAbsent(conn, blockerAccountId, blockedAccountId);
                if (inserted) {
                    // 7.1 Follow cleanup — both directions.
                    accountFollowDAO.delete(conn, blockerAccountId, blockedAccountId);
                    accountFollowDAO.delete(conn, blockedAccountId, blockerAccountId);
                    // 7.2/7.3 Friendship cleanup — ACCEPTED -> UNFRIENDED, PENDING -> CANCELLED, either direction.
                    accountFriendshipDAO.cleanupForBlock(conn, blockerAccountId, blockedAccountId);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        // No Notification for Block (task spec §13).
    }

    /**
     * blockerAccountId must come from the caller's session (task spec §4).
     * Deliberately resolves the target via {@link AccountDAO#findByUsername}
     * directly rather than {@link AccountService#getPublicProfile} — Unblock
     * must keep working even if the target account was deactivated after A
     * blocked it, same reasoning as {@link AccountFollowService#unfollow}.
     * No exception if the block doesn't exist — that's just a no-op (task
     * spec §3/§9). Never restores Follow/Friendship (task spec §9/§25).
     */
    public void unblock(int blockerAccountId, String targetUsername) throws AccountNotFoundException, SQLException {
        Account target = accountDAO.findByUsername(targetUsername == null ? null : targetUsername.trim())
                .orElseThrow(() -> new AccountNotFoundException("Người dùng không tồn tại."));

        accountBlockDAO.delete(blockerAccountId, target.getAccountId());
        // No Notification for Unblock (task spec §13).
    }

    /** Exact direction only — used to render "Đã chặn" / "Bỏ chặn" for the current viewer. */
    public boolean isBlocked(int blockerAccountId, int blockedAccountId) throws SQLException {
        return accountBlockDAO.exists(blockerAccountId, blockedAccountId);
    }

    /**
     * Either direction — the two-sided restriction check (task spec §10).
     * Follow/Friend call this before creating any new relationship; it never
     * reveals which side is the blocker to the caller.
     */
    public boolean isBlockedBetween(int accountA, int accountB) throws SQLException {
        return accountBlockDAO.existsEitherDirection(accountA, accountB);
    }

    public List<AccountBlock> listBlocked(int blockerAccountId, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return accountBlockDAO.listBlocked(blockerAccountId, offset, PAGE_SIZE);
    }

    public int countBlocked(int blockerAccountId) throws SQLException {
        return accountBlockDAO.countBlocked(blockerAccountId);
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }
}
