package com.gamenest.dao;

import com.gamenest.exception.DuplicateFriendRequestException;
import com.gamenest.model.AccountFriendship;
import com.gamenest.model.FriendshipStatus;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AccountFriendships is a state-machine table (db/17_account_friendships.sql)
 * — unlike LFGMembers/AccountGames/AccountFollows there is no hard delete
 * here; every relationship change is a guarded conditional UPDATE. Every
 * status-transition UPDATE includes an ownership predicate
 * (requester_account_id/receiver_account_id = caller) AND a
 * {@code status = <expected current status>} guard, so double-processing
 * (two concurrent Accepts, etc.) and cross-account tampering both fail
 * safely with 0 rows affected rather than a second silent transition (see
 * AccountFriendService).
 */
public class AccountFriendshipDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

    private static final String SELECT_COLUMNS =
            "friendship_id, requester_account_id, receiver_account_id, status, created_at, responded_at ";
    private static final String BASE_SELECT = "SELECT " + SELECT_COLUMNS + "FROM dbo.AccountFriendships ";

    /** Only used when no row exists yet for this exact (requester, receiver) direction. */
    public AccountFriendship insert(int requesterAccountId, int receiverAccountId)
            throws SQLException, DuplicateFriendRequestException {
        String sql = "INSERT INTO dbo.AccountFriendships (requester_account_id, receiver_account_id, status) "
                + "VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, requesterAccountId);
            ps.setInt(2, receiverAccountId);
            ps.setString(3, FriendshipStatus.PENDING);
            ps.executeUpdate();

            AccountFriendship friendship = new AccountFriendship();
            friendship.setRequesterAccountId(requesterAccountId);
            friendship.setReceiverAccountId(receiverAccountId);
            friendship.setStatus(FriendshipStatus.PENDING);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    friendship.setFriendshipId(keys.getInt(1));
                }
            }
            return friendship;

        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                throw new DuplicateFriendRequestException("Đã tồn tại lời mời kết bạn giữa hai tài khoản này.");
            }
            throw e;
        }
    }

    /** The row for this exact (requester, receiver) direction, any status — at most one, per UQ_AccountFriendships_pair. */
    public Optional<AccountFriendship> findByPair(int requesterAccountId, int receiverAccountId) throws SQLException {
        String sql = BASE_SELECT + "WHERE requester_account_id = ? AND receiver_account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requesterAccountId);
            ps.setInt(2, receiverAccountId);
            return findOne(ps);
        }
    }

    /** PENDING or ACCEPTED between the two accounts, in either direction — at most one can exist under the invariants the Service enforces. */
    public Optional<AccountFriendship> findActiveBetween(int accountId1, int accountId2) throws SQLException {
        String sql = BASE_SELECT
                + "WHERE ((requester_account_id = ? AND receiver_account_id = ?) "
                + "OR (requester_account_id = ? AND receiver_account_id = ?)) "
                + "AND status IN (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId1);
            ps.setInt(2, accountId2);
            ps.setInt(3, accountId2);
            ps.setInt(4, accountId1);
            ps.setString(5, FriendshipStatus.PENDING);
            ps.setString(6, FriendshipStatus.ACCEPTED);
            return findOne(ps);
        }
    }

    public Optional<AccountFriendship> findById(int friendshipId) throws SQLException {
        String sql = BASE_SELECT + "WHERE friendship_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, friendshipId);
            return findOne(ps);
        }
    }

    /**
     * Re-friend after REJECTED/CANCELLED/UNFRIENDED: reuses the existing row
     * instead of inserting a new one (would violate UQ_AccountFriendships_pair)
     * — resets responded_at and refreshes created_at so the row reads like a
     * fresh request. Guarded to only fire from a terminal status, so two
     * concurrent re-friend attempts can't both succeed.
     */
    public int reactivateAsPending(int friendshipId) throws SQLException {
        String sql = "UPDATE dbo.AccountFriendships SET status = ?, responded_at = NULL, created_at = SYSUTCDATETIME() "
                + "WHERE friendship_id = ? AND status IN (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, FriendshipStatus.PENDING);
            ps.setInt(2, friendshipId);
            ps.setString(3, FriendshipStatus.REJECTED);
            ps.setString(4, FriendshipStatus.CANCELLED);
            ps.setString(5, FriendshipStatus.UNFRIENDED);
            return ps.executeUpdate();
        }
    }

    public int acceptIfPending(int friendshipId, int receiverAccountId) throws SQLException {
        return transitionIfPending(friendshipId, "receiver_account_id", receiverAccountId, FriendshipStatus.ACCEPTED);
    }

    public int rejectIfPending(int friendshipId, int receiverAccountId) throws SQLException {
        return transitionIfPending(friendshipId, "receiver_account_id", receiverAccountId, FriendshipStatus.REJECTED);
    }

    public int cancelIfPending(int friendshipId, int requesterAccountId) throws SQLException {
        return transitionIfPending(friendshipId, "requester_account_id", requesterAccountId, FriendshipStatus.CANCELLED);
    }

    private int transitionIfPending(int friendshipId, String ownerColumn, int ownerAccountId, String newStatus)
            throws SQLException {
        String sql = "UPDATE dbo.AccountFriendships SET status = ?, responded_at = SYSUTCDATETIME() "
                + "WHERE friendship_id = ? AND " + ownerColumn + " = ? AND status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, friendshipId);
            ps.setInt(3, ownerAccountId);
            ps.setString(4, FriendshipStatus.PENDING);
            return ps.executeUpdate();
        }
    }

    /** Either side of an ACCEPTED friendship may unfriend — never a third account. */
    public int unfriendIfAccepted(int friendshipId, int currentAccountId) throws SQLException {
        String sql = "UPDATE dbo.AccountFriendships SET status = ?, responded_at = SYSUTCDATETIME() "
                + "WHERE friendship_id = ? AND status = ? AND (requester_account_id = ? OR receiver_account_id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, FriendshipStatus.UNFRIENDED);
            ps.setInt(2, friendshipId);
            ps.setString(3, FriendshipStatus.ACCEPTED);
            ps.setInt(4, currentAccountId);
            ps.setInt(5, currentAccountId);
            return ps.executeUpdate();
        }
    }

    /**
     * Block side effects (task spec §7.2/§7.3), run inside AccountBlockService's
     * transaction on a caller-supplied Connection: any ACCEPTED friendship
     * between the two accounts (either direction) becomes UNFRIENDED, and any
     * PENDING request between them (either direction) becomes CANCELLED.
     * REJECTED/CANCELLED/UNFRIENDED rows are already terminal and untouched.
     * At most one row can match each UPDATE under the invariants
     * AccountFriendService already enforces (at most one active relationship
     * per pair at a time), so this never affects more than the single
     * relevant row.
     */
    public void cleanupForBlock(Connection conn, int accountA, int accountB) throws SQLException {
        transitionBetweenIfStatus(conn, accountA, accountB, FriendshipStatus.ACCEPTED, FriendshipStatus.UNFRIENDED);
        transitionBetweenIfStatus(conn, accountA, accountB, FriendshipStatus.PENDING, FriendshipStatus.CANCELLED);
    }

    private void transitionBetweenIfStatus(Connection conn, int accountA, int accountB,
                                            String requiredStatus, String newStatus) throws SQLException {
        String sql = "UPDATE dbo.AccountFriendships SET status = ?, responded_at = SYSUTCDATETIME() "
                + "WHERE status = ? AND ((requester_account_id = ? AND receiver_account_id = ?) "
                + "OR (requester_account_id = ? AND receiver_account_id = ?))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, requiredStatus);
            ps.setInt(3, accountA);
            ps.setInt(4, accountB);
            ps.setInt(5, accountB);
            ps.setInt(6, accountA);
            ps.executeUpdate();
        }
    }

    /** Accounts accountId is ACCEPTED-friends with, from either side — newest response first, DB-side paginated. */
    public List<AccountFriendship> listFriends(int accountId, int offset, int limit) throws SQLException {
        String sql = "SELECT f.friendship_id, f.requester_account_id, f.receiver_account_id, f.status, "
                + "f.created_at, f.responded_at, "
                + "a.username AS other_username, a.display_name AS other_display_name, a.avatar_url AS other_avatar_url "
                + "FROM dbo.AccountFriendships f "
                + "JOIN dbo.Accounts a ON a.account_id = "
                + "CASE WHEN f.requester_account_id = ? THEN f.receiver_account_id ELSE f.requester_account_id END "
                + "WHERE f.status = ? AND (f.requester_account_id = ? OR f.receiver_account_id = ?) "
                + "ORDER BY f.responded_at DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, FriendshipStatus.ACCEPTED);
            ps.setInt(3, accountId);
            ps.setInt(4, accountId);
            ps.setInt(5, offset);
            ps.setInt(6, limit);
            return mapOtherList(ps);
        }
    }

    /**
     * All of accountId's ACCEPTED-friend account ids, unpaginated — realtime
     * broadcast targeting (Presence), not a UI list, so {@link #listFriends}
     * (paginated, joined for display) is not reused as-is here.
     */
    public List<Integer> listFriendAccountIds(int accountId) throws SQLException {
        String sql = "SELECT CASE WHEN requester_account_id = ? THEN receiver_account_id ELSE requester_account_id END AS other_account_id "
                + "FROM dbo.AccountFriendships WHERE status = ? AND (requester_account_id = ? OR receiver_account_id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, FriendshipStatus.ACCEPTED);
            ps.setInt(3, accountId);
            ps.setInt(4, accountId);
            List<Integer> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getInt("other_account_id"));
                }
            }
            return results;
        }
    }

    public int countFriends(int accountId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.AccountFriendships "
                + "WHERE status = ? AND (requester_account_id = ? OR receiver_account_id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, FriendshipStatus.ACCEPTED);
            ps.setInt(2, accountId);
            ps.setInt(3, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** PENDING requests where accountId is the receiver — "other" is always the requester. */
    public List<AccountFriendship> listIncomingRequests(int accountId, int offset, int limit) throws SQLException {
        String sql = "SELECT f.friendship_id, f.requester_account_id, f.receiver_account_id, f.status, "
                + "f.created_at, f.responded_at, "
                + "a.username AS other_username, a.display_name AS other_display_name, a.avatar_url AS other_avatar_url "
                + "FROM dbo.AccountFriendships f "
                + "JOIN dbo.Accounts a ON a.account_id = f.requester_account_id "
                + "WHERE f.receiver_account_id = ? AND f.status = ? "
                + "ORDER BY f.created_at DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, FriendshipStatus.PENDING);
            ps.setInt(3, offset);
            ps.setInt(4, limit);
            return mapOtherList(ps);
        }
    }

    public int countIncomingRequests(int accountId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.AccountFriendships WHERE receiver_account_id = ? AND status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, FriendshipStatus.PENDING);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Optional<AccountFriendship> findOne(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        }
    }

    private List<AccountFriendship> mapOtherList(PreparedStatement ps) throws SQLException {
        List<AccountFriendship> results = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AccountFriendship f = mapRow(rs);
                f.setOtherUsername(rs.getString("other_username"));
                f.setOtherDisplayName(rs.getString("other_display_name"));
                f.setOtherAvatarUrl(rs.getString("other_avatar_url"));
                results.add(f);
            }
        }
        return results;
    }

    private AccountFriendship mapRow(ResultSet rs) throws SQLException {
        AccountFriendship f = new AccountFriendship();
        f.setFriendshipId(rs.getInt("friendship_id"));
        f.setRequesterAccountId(rs.getInt("requester_account_id"));
        f.setReceiverAccountId(rs.getInt("receiver_account_id"));
        f.setStatus(rs.getString("status"));
        f.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        f.setRespondedAt(rs.getObject("responded_at", LocalDateTime.class));
        return f;
    }
}
