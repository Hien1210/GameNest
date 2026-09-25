package com.gamenest.dao;

import com.gamenest.model.LFGPost;
import com.gamenest.model.LFGStatus;
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
 * LFGPosts is never hard-deleted by application code (task spec §22) — only
 * a guarded status UPDATE to DELETED exists here, no delete() method.
 * Join/Leave counter updates ({@link #tryIncrementPlayers}/
 * {@link #tryDecrementPlayers}) are single atomic conditional UPDATE
 * statements — SQL Server row-locks the target row for the statement's
 * duration, so two concurrent Joins on the same lfg_id serialize correctly
 * without any extra application-level locking (task spec §9).
 */
public class LFGPostDAO {

    private static final String DETAIL_SELECT =
            "SELECT l.lfg_id, l.account_id, l.game_id, l.title, l.description, l.game_mode, l.required_rank, "
                    + "l.region, l.max_players, l.current_players, l.start_time, l.status, l.created_at, l.updated_at, "
                    + "a.username AS creator_username, g.name AS game_name "
                    + "FROM dbo.LFGPosts l "
                    + "JOIN dbo.Accounts a ON a.account_id = l.account_id "
                    + "JOIN dbo.Games g ON g.game_id = l.game_id ";

    public LFGPost insert(Connection conn, LFGPost post) throws SQLException {
        String sql = "INSERT INTO dbo.LFGPosts (account_id, game_id, title, description, max_players, "
                + "current_players, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, post.getAccountId());
            ps.setInt(2, post.getGameId());
            ps.setString(3, post.getTitle());
            ps.setString(4, post.getDescription());
            ps.setInt(5, post.getMaxPlayers());
            ps.setInt(6, post.getCurrentPlayers());
            ps.setString(7, post.getStatus());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    post.setLfgId(keys.getInt(1));
                }
            }
            return post;
        }
    }

    public Optional<LFGPost> findById(int lfgId) throws SQLException {
        String sql = DETAIL_SELECT + "WHERE l.lfg_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lfgId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * DB-side filtered + paginated listing. Excludes DELETED unconditionally
     * (task spec §5). When {@code status} is null/blank, defaults to
     * OPEN+FULL only ("ưu tiên OPEN/FULL" — task spec §5); an explicit
     * status filter (including CLOSED/EXPIRED) overrides that default.
     */
    public List<LFGPost> search(Integer gameId, String status, String keyword, int offset, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder(DETAIL_SELECT).append("WHERE l.status <> ? ");
        List<Object> params = new ArrayList<>();
        params.add(LFGStatus.DELETED);
        appendFilters(sql, params, gameId, status, keyword);
        sql.append("ORDER BY l.created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            return mapList(ps);
        }
    }

    public int count(Integer gameId, String status, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dbo.LFGPosts l WHERE l.status <> ? ");
        List<Object> params = new ArrayList<>();
        params.add(LFGStatus.DELETED);
        appendFilters(sql, params, gameId, status, keyword);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void appendFilters(StringBuilder sql, List<Object> params, Integer gameId, String status,
                                String keyword) {
        if (status != null && !status.isEmpty()) {
            sql.append("AND l.status = ? ");
            params.add(status);
        } else {
            sql.append("AND l.status IN (?, ?) ");
            params.add(LFGStatus.OPEN);
            params.add(LFGStatus.FULL);
        }
        if (gameId != null) {
            sql.append("AND l.game_id = ? ");
            params.add(gameId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (l.title LIKE ? ESCAPE '\\' OR l.description LIKE ? ESCAPE '\\') ");
            String pattern = likePattern(keyword);
            params.add(pattern);
            params.add(pattern);
        }
    }

    /**
     * Atomic conditional increment for Join: only succeeds while status is
     * OPEN and there is still room. Also flips status to FULL in the same
     * statement when the increment fills the last slot. 0 rows affected
     * means "cannot join" (full, not OPEN, or gone) — the caller treats
     * that as the authoritative answer, not a prior SELECT.
     */
    public int tryIncrementPlayers(Connection conn, int lfgId) throws SQLException {
        String sql = "UPDATE dbo.LFGPosts SET current_players = current_players + 1, "
                + "status = CASE WHEN current_players + 1 >= max_players THEN ? ELSE status END, "
                + "updated_at = SYSUTCDATETIME() "
                + "WHERE lfg_id = ? AND status = ? AND current_players < max_players";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LFGStatus.FULL);
            ps.setInt(2, lfgId);
            ps.setString(3, LFGStatus.OPEN);
            return ps.executeUpdate();
        }
    }

    /**
     * Atomic conditional decrement for Leave: flips FULL back to OPEN in
     * the same statement when a slot opens up. Guarded by
     * {@code current_players > 0} so the counter can never go negative.
     */
    public int tryDecrementPlayers(Connection conn, int lfgId) throws SQLException {
        String sql = "UPDATE dbo.LFGPosts SET current_players = current_players - 1, "
                + "status = CASE WHEN status = ? AND current_players - 1 < max_players THEN ? ELSE status END, "
                + "updated_at = SYSUTCDATETIME() "
                + "WHERE lfg_id = ? AND current_players > 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LFGStatus.FULL);
            ps.setString(2, LFGStatus.OPEN);
            ps.setInt(3, lfgId);
            return ps.executeUpdate();
        }
    }

    /**
     * Creator Edit — title/description/max_players only (task decision:
     * game_id is not editable, see LFGService). Recomputes OPEN/FULL from
     * the new max_players against the unchanged current_players in the
     * same statement, so capacity changes never leave status stale.
     * Refuses a DELETED post.
     */
    public int updateDetails(int lfgId, String title, String description, int maxPlayers) throws SQLException {
        String sql = "UPDATE dbo.LFGPosts SET title = ?, description = ?, max_players = ?, "
                + "status = CASE "
                + "  WHEN status = ? AND current_players >= ? THEN ? "
                + "  WHEN status = ? AND current_players < ? THEN ? "
                + "  ELSE status END, "
                + "updated_at = SYSUTCDATETIME() "
                + "WHERE lfg_id = ? AND status <> ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setInt(3, maxPlayers);
            ps.setString(4, LFGStatus.OPEN);
            ps.setInt(5, maxPlayers);
            ps.setString(6, LFGStatus.FULL);
            ps.setString(7, LFGStatus.FULL);
            ps.setInt(8, maxPlayers);
            ps.setString(9, LFGStatus.OPEN);
            ps.setInt(10, lfgId);
            ps.setString(11, LFGStatus.DELETED);
            return ps.executeUpdate();
        }
    }

    /**
     * Guarded status transition (Close/Soft-Delete) — only succeeds if the
     * post's current status is one of {@code allowedFrom}. Ownership is
     * checked by the Service before calling this (mirrors
     * QuestionService/AnswerService's existing owner-check convention),
     * not re-checked in this WHERE clause.
     */
    public int updateStatus(int lfgId, String newStatus, String... allowedFrom) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "UPDATE dbo.LFGPosts SET status = ?, updated_at = SYSUTCDATETIME() WHERE lfg_id = ? AND status IN (");
        for (int i = 0; i < allowedFrom.length; i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, newStatus);
            ps.setInt(2, lfgId);
            for (int i = 0; i < allowedFrom.length; i++) {
                ps.setString(3 + i, allowedFrom[i]);
            }
            return ps.executeUpdate();
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private String likePattern(String query) {
        String escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private List<LFGPost> mapList(PreparedStatement ps) throws SQLException {
        List<LFGPost> results = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    private LFGPost mapRow(ResultSet rs) throws SQLException {
        LFGPost post = new LFGPost();
        post.setLfgId(rs.getInt("lfg_id"));
        post.setAccountId(rs.getInt("account_id"));
        post.setGameId(rs.getInt("game_id"));
        post.setTitle(rs.getString("title"));
        post.setDescription(rs.getString("description"));
        post.setGameMode(rs.getString("game_mode"));
        post.setRequiredRank(rs.getString("required_rank"));
        post.setRegion(rs.getString("region"));
        post.setMaxPlayers(rs.getInt("max_players"));
        post.setCurrentPlayers(rs.getInt("current_players"));
        post.setStartTime(rs.getObject("start_time", LocalDateTime.class));
        post.setStatus(rs.getString("status"));
        post.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        post.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        post.setCreatorUsername(rs.getString("creator_username"));
        post.setGameName(rs.getString("game_name"));
        return post;
    }
}
