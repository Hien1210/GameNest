package com.gamenest.dao;

import com.gamenest.model.Question;
import com.gamenest.model.QuestionStatus;
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

public class QuestionDAO {

    private static final String DETAIL_SELECT =
            "SELECT q.question_id, q.account_id, q.game_id, q.title, q.content, q.status, "
                    + "q.is_deleted, q.deleted_at, q.deleted_by, q.created_at, q.updated_at, "
                    + "a.username AS author_username, g.name AS game_name, "
                    + "(SELECT COUNT(*) FROM dbo.Answers ans WHERE ans.question_id = q.question_id "
                    + "AND ans.status = 'ACTIVE') AS answer_count "
                    + "FROM dbo.Questions q "
                    + "JOIN dbo.Accounts a ON a.account_id = q.account_id "
                    + "JOIN dbo.Games g ON g.game_id = q.game_id ";

    public Question insert(Question question) throws SQLException {
        String sql = "INSERT INTO dbo.Questions (account_id, game_id, title, content, status) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, question.getAccountId());
            ps.setInt(2, question.getGameId());
            ps.setString(3, question.getTitle());
            ps.setString(4, question.getContent());
            ps.setString(5, question.getStatus());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    question.setQuestionId(keys.getInt(1));
                }
            }
            return question;
        }
    }

    public Optional<Question> findById(int questionId) throws SQLException {
        String sql = DETAIL_SELECT + "WHERE q.question_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    public List<Question> listActiveByGame(int gameId, int offset, int limit) throws SQLException {
        String sql = DETAIL_SELECT + "WHERE q.game_id = ? AND q.status = ? "
                + "ORDER BY q.created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setString(2, QuestionStatus.ACTIVE);
            ps.setInt(3, offset);
            ps.setInt(4, limit);
            return mapList(ps);
        }
    }

    public int countActiveByGame(int gameId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Questions WHERE game_id = ? AND status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setString(2, QuestionStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<Question> searchActiveByGame(int gameId, String query, int offset, int limit) throws SQLException {
        String sql = DETAIL_SELECT + "WHERE q.game_id = ? AND q.status = ? AND q.title LIKE ? ESCAPE '\\' "
                + "ORDER BY q.created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setString(2, QuestionStatus.ACTIVE);
            ps.setString(3, likePattern(query));
            ps.setInt(4, offset);
            ps.setInt(5, limit);
            return mapList(ps);
        }
    }

    public int countSearchActiveByGame(int gameId, String query) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Questions WHERE game_id = ? AND status = ? AND title LIKE ? ESCAPE '\\'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setString(2, QuestionStatus.ACTIVE);
            ps.setString(3, likePattern(query));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void update(int questionId, String title, String content) throws SQLException {
        String sql = "UPDATE dbo.Questions SET title = ?, content = ?, updated_at = SYSUTCDATETIME() "
                + "WHERE question_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setInt(3, questionId);
            ps.executeUpdate();
        }
    }

    public int softDelete(int questionId, int deletedByAccountId) throws SQLException {
        String sql = "UPDATE dbo.Questions SET status = ?, is_deleted = 1, "
                + "deleted_at = SYSUTCDATETIME(), deleted_by = ? WHERE question_id = ? AND is_deleted = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, QuestionStatus.DELETED);
            ps.setInt(2, deletedByAccountId);
            ps.setInt(3, questionId);
            return ps.executeUpdate();
        }
    }

    /**
     * Plain status change — deliberately separate from {@link #softDelete},
     * which also owns is_deleted/deleted_at/deleted_by bookkeeping. Used by
     * Questions Moderation for ACTIVE/HIDDEN/LOCKED transitions that don't
     * touch those columns.
     */
    public int updateStatus(int questionId, String status) throws SQLException {
        String sql = "UPDATE dbo.Questions SET status = ?, updated_at = SYSUTCDATETIME() WHERE question_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, questionId);
            return ps.executeUpdate();
        }
    }

    /**
     * DB-side filtered + paginated listing across ALL games, for Moderator
     * review — unlike {@link #listActiveByGame}, not scoped to one game or
     * ACTIVE-only. Any filter left null/blank is simply omitted from the
     * WHERE clause; all values are bound via PreparedStatement placeholders.
     */
    public List<Question> searchForModeration(String status, Integer gameId, String authorUsername,
                                               String searchText, int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(DETAIL_SELECT).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendModerationFilters(sql, params, status, gameId, authorUsername, searchText);
        sql.append("ORDER BY q.created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            return mapList(ps);
        }
    }

    public int countForModeration(String status, Integer gameId, String authorUsername, String searchText)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dbo.Questions q "
                + "JOIN dbo.Accounts a ON a.account_id = q.account_id "
                + "JOIN dbo.Games g ON g.game_id = q.game_id WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendModerationFilters(sql, params, status, gameId, authorUsername, searchText);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void appendModerationFilters(StringBuilder sql, List<Object> params, String status, Integer gameId,
                                          String authorUsername, String searchText) {
        if (status != null && !status.isEmpty()) {
            sql.append("AND q.status = ? ");
            params.add(status);
        }
        if (gameId != null) {
            sql.append("AND q.game_id = ? ");
            params.add(gameId);
        }
        if (authorUsername != null && !authorUsername.isEmpty()) {
            sql.append("AND a.username LIKE ? ESCAPE '\\' ");
            params.add(likePattern(authorUsername));
        }
        if (searchText != null && !searchText.isEmpty()) {
            sql.append("AND (q.title LIKE ? ESCAPE '\\' OR q.content LIKE ? ESCAPE '\\') ");
            String pattern = likePattern(searchText);
            params.add(pattern);
            params.add(pattern);
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

    private List<Question> mapList(PreparedStatement ps) throws SQLException {
        List<Question> questions = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                questions.add(mapRow(rs));
            }
        }
        return questions;
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        Question question = new Question();
        question.setQuestionId(rs.getInt("question_id"));
        question.setAccountId(rs.getInt("account_id"));
        question.setGameId(rs.getInt("game_id"));
        question.setTitle(rs.getString("title"));
        question.setContent(rs.getString("content"));
        question.setStatus(rs.getString("status"));
        question.setDeleted(rs.getBoolean("is_deleted"));
        question.setDeletedAt(rs.getObject("deleted_at", LocalDateTime.class));
        int deletedBy = rs.getInt("deleted_by");
        question.setDeletedBy(rs.wasNull() ? null : deletedBy);
        question.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        question.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        question.setAuthorUsername(rs.getString("author_username"));
        question.setGameName(rs.getString("game_name"));
        question.setAnswerCount(rs.getInt("answer_count"));
        return question;
    }
}
