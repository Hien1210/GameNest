package com.gamenest.service;

import com.gamenest.dao.LFGMemberDAO;
import com.gamenest.dao.LFGPostDAO;
import com.gamenest.exception.DuplicateLFGMemberException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.GameNotFoundException;
import com.gamenest.exception.LFGNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.LFGMember;
import com.gamenest.model.LFGPost;
import com.gamenest.model.LFGStatus;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Business rules for USER-facing LFG (task spec). Reuses
 * {@link GameService#getActiveGameDetail} as-is for target validation
 * (existence + ACTIVE check) instead of duplicating that logic. Join/Leave
 * are wrapped in a single transaction over a shared {@link Connection},
 * mirroring the exact pattern already established by
 * {@link AnswerService#acceptAnswer} — no new transaction abstraction was
 * introduced for this task.
 */
public class LFGService {

    private static final int PAGE_SIZE = 12;
    private static final int TITLE_MAX_LENGTH = 250;
    private static final int DESCRIPTION_MAX_LENGTH = 5000;
    private static final int MAX_PLAYERS_UPPER_BOUND = 100;

    private final LFGPostDAO lfgPostDAO;
    private final LFGMemberDAO lfgMemberDAO;
    private final GameService gameService;

    public LFGService() {
        this.lfgPostDAO = new LFGPostDAO();
        this.lfgMemberDAO = new LFGMemberDAO();
        this.gameService = new GameService();
    }

    public LFGService(LFGPostDAO lfgPostDAO, LFGMemberDAO lfgMemberDAO, GameService gameService) {
        this.lfgPostDAO = lfgPostDAO;
        this.lfgMemberDAO = lfgMemberDAO;
        this.gameService = gameService;
    }

    // ---- List ----

    public List<LFGPost> search(Integer gameId, String status, String keyword, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return lfgPostDAO.search(gameId, blankToNull(status), blankToNull(keyword), offset, PAGE_SIZE);
    }

    public int count(Integer gameId, String status, String keyword) throws SQLException {
        return lfgPostDAO.count(gameId, blankToNull(status), blankToNull(keyword));
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    // ---- Detail ----

    /**
     * A DELETED post behaves like it doesn't exist for normal viewing —
     * same "non-eligible = not found" rule already used by
     * {@link GameService#getActiveGameDetail} for INACTIVE games (task
     * spec §7).
     */
    public LFGPost getLfg(int lfgId) throws LFGNotFoundException, SQLException {
        LFGPost post = lfgPostDAO.findById(lfgId)
                .orElseThrow(() -> new LFGNotFoundException("Nhóm không tồn tại."));
        if (LFGStatus.DELETED.equals(post.getStatus())) {
            throw new LFGNotFoundException("Nhóm không tồn tại.");
        }
        return post;
    }

    public List<LFGMember> listMembers(int lfgId) throws SQLException {
        return lfgMemberDAO.findByLfgId(lfgId);
    }

    // ---- Create ----

    /**
     * accountId always comes from the caller's session — this method
     * trusts whatever int it is given, so callers must never source it
     * from a request parameter (task spec §6). The creator becomes a
     * member immediately (current_players starts at 1); if max_players is
     * 1, the post is FULL from the moment it's created.
     */
    public LFGPost create(int accountId, int gameId, String title, String description, int maxPlayers)
            throws ValidationException, GameNotFoundException, SQLException {

        String normalizedTitle = title == null ? null : title.trim();
        String normalizedDescription = normalizeOptional(description);
        validateTitle(normalizedTitle);
        validateDescription(normalizedDescription);
        validateMaxPlayers(maxPlayers);

        // Throws GameNotFoundException for a missing OR inactive game —
        // same rule the Games module already uses for hiding INACTIVE games.
        gameService.getActiveGameDetail(gameId);

        LFGPost post = new LFGPost();
        post.setAccountId(accountId);
        post.setGameId(gameId);
        post.setTitle(normalizedTitle);
        post.setDescription(normalizedDescription);
        post.setMaxPlayers(maxPlayers);
        post.setCurrentPlayers(1);
        post.setStatus(maxPlayers <= 1 ? LFGStatus.FULL : LFGStatus.OPEN);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LFGPost inserted = lfgPostDAO.insert(conn, post);
                lfgMemberDAO.insert(conn, inserted.getLfgId(), accountId);
                conn.commit();
                return inserted;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (DuplicateLFGMemberException e) {
                // Cannot happen on a brand-new post (creator is the first
                // member) — handled defensively rather than dropped.
                conn.rollback();
                throw new SQLException("Unexpected duplicate membership while creating LFG post", e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ---- Join / Leave ----

    /**
     * Atomicity for the "last slot" race (task spec §9): the capacity
     * check and the increment happen in ONE conditional UPDATE
     * ({@link LFGPostDAO#tryIncrementPlayers}), not a separate
     * SELECT-then-INSERT. SQL Server row-locks the LFGPosts row for that
     * statement, so two concurrent Joins on the same lfg_id serialize —
     * whichever commits first wins the slot, and the second sees 0 rows
     * affected and fails cleanly instead of over-filling the group.
     */
    public void join(int lfgId, int accountId)
            throws LFGNotFoundException, ValidationException, DuplicateLFGMemberException, SQLException {

        LFGPost post = getLfg(lfgId);

        if (post.getAccountId() == accountId) {
            throw new ValidationException("Bạn là người tạo nhóm này.");
        }
        if (!LFGStatus.OPEN.equals(post.getStatus())) {
            throw new ValidationException("Nhóm này hiện không nhận thêm thành viên.");
        }
        if (lfgMemberDAO.isMember(lfgId, accountId)) {
            throw new DuplicateLFGMemberException("Bạn đã tham gia nhóm này rồi.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int updated = lfgPostDAO.tryIncrementPlayers(conn, lfgId);
                if (updated == 0) {
                    conn.rollback();
                    throw new ValidationException("Nhóm này hiện không còn chỗ trống.");
                }
                lfgMemberDAO.insert(conn, lfgId, accountId);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (ValidationException e) {
                conn.rollback();
                throw e;
            } catch (DuplicateLFGMemberException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * The creator cannot Leave through this flow (task spec §11) — they
     * use Close/Delete instead, and ownership is never transferred.
     */
    public void leave(int lfgId, int accountId) throws LFGNotFoundException, ValidationException, SQLException {
        LFGPost post = getLfg(lfgId);

        if (post.getAccountId() == accountId) {
            throw new ValidationException("Người tạo nhóm không thể rời nhóm qua chức năng này.");
        }
        if (!lfgMemberDAO.isMember(lfgId, accountId)) {
            throw new ValidationException("Bạn không phải thành viên của nhóm này.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int deleted = lfgMemberDAO.delete(conn, lfgId, accountId);
                if (deleted > 0) {
                    lfgPostDAO.tryDecrementPlayers(conn, lfgId);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ---- Creator management ----

    /**
     * Title/description/max_players only — game_id is intentionally NOT
     * editable (task's own field list marks it optional: "nếu business
     * rule/schema cho phép"; changing the game of a post members already
     * joined for would be confusing, so this task keeps it fixed after
     * creation). Ownership is a strict server-side check, matching
     * QuestionService/AnswerService's existing owner-only editing rule —
     * never inferred from hidden fields.
     */
    public LFGPost edit(int lfgId, int requesterAccountId, String title, String description, int maxPlayers)
            throws LFGNotFoundException, ForbiddenException, ValidationException, SQLException {

        LFGPost post = getLfg(lfgId);
        requireOwner(post, requesterAccountId);

        String normalizedTitle = title == null ? null : title.trim();
        String normalizedDescription = normalizeOptional(description);
        validateTitle(normalizedTitle);
        validateDescription(normalizedDescription);
        validateMaxPlayers(maxPlayers);

        if (maxPlayers < post.getCurrentPlayers()) {
            throw new ValidationException(
                    "Số lượng tối đa không được nhỏ hơn số thành viên hiện tại (" + post.getCurrentPlayers() + ").");
        }

        int updated = lfgPostDAO.updateDetails(lfgId, normalizedTitle, normalizedDescription, maxPlayers);
        if (updated == 0) {
            throw new LFGNotFoundException("Nhóm không tồn tại hoặc đã bị xóa.");
        }

        post.setTitle(normalizedTitle);
        post.setDescription(normalizedDescription);
        post.setMaxPlayers(maxPlayers);
        return post;
    }

    /** OPEN/FULL -> CLOSED. Creator only. */
    public void close(int lfgId, int requesterAccountId)
            throws LFGNotFoundException, ForbiddenException, ValidationException, SQLException {

        LFGPost post = getLfg(lfgId);
        requireOwner(post, requesterAccountId);

        int updated = lfgPostDAO.updateStatus(lfgId, LFGStatus.CLOSED, LFGStatus.OPEN, LFGStatus.FULL);
        if (updated == 0) {
            throw new ValidationException("Nhóm này không ở trạng thái có thể đóng.");
        }
    }

    /** OPEN/FULL/CLOSED -> DELETED (soft delete only — task spec §22). Creator only. */
    public void softDelete(int lfgId, int requesterAccountId)
            throws LFGNotFoundException, ForbiddenException, SQLException {

        LFGPost post = getLfg(lfgId);
        requireOwner(post, requesterAccountId);

        lfgPostDAO.updateStatus(lfgId, LFGStatus.DELETED, LFGStatus.OPEN, LFGStatus.FULL, LFGStatus.CLOSED);
    }

    private void requireOwner(LFGPost post, int requesterAccountId) throws ForbiddenException {
        if (post.getAccountId() != requesterAccountId) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này.");
        }
    }

    private void validateTitle(String title) throws ValidationException {
        if (title == null || title.isEmpty()) {
            throw new ValidationException("Tiêu đề không được để trống.");
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new ValidationException("Tiêu đề không được vượt quá " + TITLE_MAX_LENGTH + " ký tự.");
        }
    }

    private void validateDescription(String description) throws ValidationException {
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new ValidationException("Mô tả không được vượt quá " + DESCRIPTION_MAX_LENGTH + " ký tự.");
        }
    }

    private void validateMaxPlayers(int maxPlayers) throws ValidationException {
        if (maxPlayers < 1) {
            throw new ValidationException("Số lượng tối đa phải lớn hơn 0.");
        }
        if (maxPlayers > MAX_PLAYERS_UPPER_BOUND) {
            throw new ValidationException("Số lượng tối đa không được vượt quá " + MAX_PLAYERS_UPPER_BOUND + ".");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }
}
