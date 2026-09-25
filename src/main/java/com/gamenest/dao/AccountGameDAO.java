package com.gamenest.dao;

import com.gamenest.exception.DuplicateAccountGameException;
import com.gamenest.model.AccountGame;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountGames is a pure junction table (task spec §11/§12 of the User
 * Profile task; schema from db/14_user_foundation.sql) — no status, no
 * soft-delete. Removing a relationship is a real DELETE, scoped by the
 * caller to (account_id, game_id, relationship_type) so a user can never
 * affect another account's rows (see AccountGameService).
 */
public class AccountGameDAO {

    private static final int SQL_ERROR_UNIQUE_VIOLATION = 2627;
    private static final int SQL_ERROR_DUPLICATE_KEY = 2601;

    private static final String SELECT_COLUMNS =
            "ag.account_game_id, ag.account_id, ag.game_id, ag.relationship_type, ag.created_at, "
                    + "g.name AS game_name, g.cover_image_url AS game_cover_image_url, g.status AS game_status ";
    private static final String BASE_SELECT =
            "SELECT " + SELECT_COLUMNS + "FROM dbo.AccountGames ag JOIN dbo.Games g ON g.game_id = ag.game_id ";

    public void insert(int accountId, int gameId, String relationshipType)
            throws SQLException, DuplicateAccountGameException {
        String sql = "INSERT INTO dbo.AccountGames (account_id, game_id, relationship_type) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, accountId);
            ps.setInt(2, gameId);
            ps.setString(3, relationshipType);
            ps.executeUpdate();

        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_ERROR_UNIQUE_VIOLATION || e.getErrorCode() == SQL_ERROR_DUPLICATE_KEY) {
                throw new DuplicateAccountGameException("Game này đã có trong danh sách của bạn.");
            }
            throw e;
        }
    }

    /**
     * Scoped strictly to (account_id, game_id, relationship_type) — always
     * called with account_id taken from the caller's own session, so this
     * can never delete another account's relationship row. 0 rows affected
     * is a normal, silent no-op (the relationship simply wasn't there).
     */
    public int delete(int accountId, int gameId, String relationshipType) throws SQLException {
        String sql = "DELETE FROM dbo.AccountGames WHERE account_id = ? AND game_id = ? AND relationship_type = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, gameId);
            ps.setString(3, relationshipType);
            return ps.executeUpdate();
        }
    }

    public List<AccountGame> findByAccountAndType(int accountId, String relationshipType) throws SQLException {
        String sql = BASE_SELECT + "WHERE ag.account_id = ? AND ag.relationship_type = ? ORDER BY ag.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, relationshipType);
            List<AccountGame> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        }
    }

    private AccountGame mapRow(ResultSet rs) throws SQLException {
        AccountGame accountGame = new AccountGame();
        accountGame.setAccountGameId(rs.getInt("account_game_id"));
        accountGame.setAccountId(rs.getInt("account_id"));
        accountGame.setGameId(rs.getInt("game_id"));
        accountGame.setRelationshipType(rs.getString("relationship_type"));
        accountGame.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        accountGame.setGameName(rs.getString("game_name"));
        accountGame.setGameCoverImageUrl(rs.getString("game_cover_image_url"));
        accountGame.setGameStatus(rs.getString("game_status"));
        return accountGame;
    }
}
