package com.gamenest.dao;

import com.gamenest.exception.DuplicateGameException;
import com.gamenest.model.Game;
import com.gamenest.model.GameStatus;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL Server unique-constraint violation error codes (used to detect a
 * duplicate game name race that slips past the service-layer pre-check).
 */
public class GameDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

    private static final String SELECT_COLUMNS =
            "game_id, name, description, cover_image_url, release_date, status, created_at, updated_at ";

    public List<Game> findActive(int offset, int limit) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + "FROM dbo.Games WHERE status = ? "
                + "ORDER BY name OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, GameStatus.ACTIVE);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            return mapList(ps);
        }
    }

    public int countActive() throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Games WHERE status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, GameStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<Game> searchActiveByName(String query, int offset, int limit) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + "FROM dbo.Games WHERE status = ? AND name LIKE ? ESCAPE '\\' "
                + "ORDER BY name OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, GameStatus.ACTIVE);
            ps.setString(2, likePattern(query));
            ps.setInt(3, offset);
            ps.setInt(4, limit);
            return mapList(ps);
        }
    }

    public int countSearchActiveByName(String query) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Games WHERE status = ? AND name LIKE ? ESCAPE '\\'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, GameStatus.ACTIVE);
            ps.setString(2, likePattern(query));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<Game> findAllForAdmin(int offset, int limit) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + "FROM dbo.Games ORDER BY created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, offset);
            ps.setInt(2, limit);
            return mapList(ps);
        }
    }

    public int countAllForAdmin() throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Games";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public Optional<Game> findById(int gameId) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + "FROM dbo.Games WHERE game_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    public Game insert(Game game) throws SQLException, DuplicateGameException {
        String sql = "INSERT INTO dbo.Games (name, description, cover_image_url, release_date, status) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, game.getName());
            ps.setString(2, game.getDescription());
            ps.setString(3, game.getCoverImageUrl());
            if (game.getReleaseDate() != null) {
                ps.setDate(4, Date.valueOf(game.getReleaseDate()));
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }
            ps.setString(5, game.getStatus());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    game.setGameId(keys.getInt(1));
                }
            }
            return game;

        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                throw new DuplicateGameException("Tên game đã tồn tại.");
            }
            throw e;
        }
    }

    public void update(Game game) throws SQLException, DuplicateGameException {
        String sql = "UPDATE dbo.Games SET name = ?, description = ?, cover_image_url = ?, "
                + "release_date = ?, updated_at = SYSUTCDATETIME() WHERE game_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, game.getName());
            ps.setString(2, game.getDescription());
            ps.setString(3, game.getCoverImageUrl());
            if (game.getReleaseDate() != null) {
                ps.setDate(4, Date.valueOf(game.getReleaseDate()));
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }
            ps.setInt(5, game.getGameId());

            ps.executeUpdate();

        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                throw new DuplicateGameException("Tên game đã tồn tại.");
            }
            throw e;
        }
    }

    public int updateStatus(int gameId, String status) throws SQLException {
        String sql = "UPDATE dbo.Games SET status = ?, updated_at = SYSUTCDATETIME() WHERE game_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, gameId);
            return ps.executeUpdate();
        }
    }

    private String likePattern(String query) {
        String escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private List<Game> mapList(PreparedStatement ps) throws SQLException {
        List<Game> games = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                games.add(mapRow(rs));
            }
        }
        return games;
    }

    private Game mapRow(ResultSet rs) throws SQLException {
        Game game = new Game();
        game.setGameId(rs.getInt("game_id"));
        game.setName(rs.getString("name"));
        game.setDescription(rs.getString("description"));
        game.setCoverImageUrl(rs.getString("cover_image_url"));

        Date releaseDate = rs.getDate("release_date");
        game.setReleaseDate(releaseDate == null ? null : releaseDate.toLocalDate());

        game.setStatus(rs.getString("status"));
        game.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        game.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return game;
    }
}
