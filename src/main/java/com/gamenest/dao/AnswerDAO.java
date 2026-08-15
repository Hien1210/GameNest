package com.gamenest.dao;

import com.gamenest.model.Answer;
import com.gamenest.model.AnswerStatus;
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

public class AnswerDAO {

    private static final String DETAIL_SELECT =
            "SELECT an.answer_id, an.question_id, an.account_id, an.content, an.is_accepted, an.status, "
                    + "an.is_deleted, an.deleted_at, an.deleted_by, an.created_at, an.updated_at, "
                    + "a.username AS author_username "
                    + "FROM dbo.Answers an "
                    + "JOIN dbo.Accounts a ON a.account_id = an.account_id ";

    /**
     * Separate from {@link #DETAIL_SELECT} — adds Questions/Games joins
     * needed only for moderation list/filter (Question and Game filters,
     * task spec §5), so existing methods keep their original, unchanged
     * query shape.
     */
    private static final String MODERATION_SELECT =
            "SELECT an.answer_id, an.question_id, an.account_id, an.content, an.is_accepted, an.status, "
                    + "an.is_deleted, an.deleted_at, an.deleted_by, an.created_at, an.updated_at, "
                    + "a.username AS author_username, q.title AS question_title, g.name AS game_name "
                    + "FROM dbo.Answers an "
                    + "JOIN dbo.Accounts a ON a.account_id = an.account_id "
                    + "JOIN dbo.Questions q ON q.question_id = an.question_id "
                    + "JOIN dbo.Games g ON g.game_id = q.game_id ";

    public Answer insert(Answer answer) throws SQLException {
        String sql = "INSERT INTO dbo.Answers (question_id, account_id, content, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, answer.getQuestionId());
            ps.setInt(2, answer.getAccountId());
            ps.setString(3, answer.getContent());
            ps.setString(4, answer.getStatus());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    answer.setAnswerId(keys.getInt(1));
                }
            }
            return answer;
        }
    }

    public Optional<Answer> findById(int answerId) throws SQLException {
        String sql = DETAIL_SELECT + "WHERE an.answer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, answerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    public List<Answer> listActiveByQuestion(int questionId) throws SQLException {
        String sql = DETAIL_SELECT + "WHERE an.question_id = ? AND an.status = ? "
                + "ORDER BY an.is_accepted DESC, an.created_at ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.setString(2, AnswerStatus.ACTIVE);
            List<Answer> answers = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapRow(rs));
                }
            }
            return answers;
        }
    }

    public void update(int answerId, String content) throws SQLException {
        String sql = "UPDATE dbo.Answers SET content = ?, updated_at = SYSUTCDATETIME() WHERE answer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, content);
            ps.setInt(2, answerId);
            ps.executeUpdate();
        }
    }

    public int softDelete(int answerId, int deletedByAccountId) throws SQLException {
        String sql = "UPDATE dbo.Answers SET status = ?, is_deleted = 1, "
                + "deleted_at = SYSUTCDATETIME(), deleted_by = ? WHERE answer_id = ? AND is_deleted = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, AnswerStatus.DELETED);
            ps.setInt(2, deletedByAccountId);
            ps.setInt(3, answerId);
            return ps.executeUpdate();
        }
    }

    /**
     * Clears is_accepted on every answer of the given question. Must be run
     * in the same transaction/connection as {@link #setAccepted} so exactly
     * one answer ends up accepted (see AnswerService.acceptAnswer).
     */
    public void unacceptAllForQuestion(Connection conn, int questionId) throws SQLException {
        String sql = "UPDATE dbo.Answers SET is_accepted = 0 WHERE question_id = ? AND is_accepted = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.executeUpdate();
        }
    }

    public void setAccepted(Connection conn, int answerId, boolean accepted) throws SQLException {
        String sql = "UPDATE dbo.Answers SET is_accepted = ? WHERE answer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, accepted);
            ps.setInt(2, answerId);
            ps.executeUpdate();
        }
    }

    /**
     * Plain status change — deliberately separate from {@link #softDelete},
     * which also owns is_deleted/deleted_at/deleted_by bookkeeping. Used by
     * Answers Moderation for the ACTIVE/HIDDEN transitions that don't touch
     * those columns.
     */
    public int updateStatus(int answerId, String status) throws SQLException {
        String sql = "UPDATE dbo.Answers SET status = ?, updated_at = SYSUTCDATETIME() WHERE answer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, answerId);
            return ps.executeUpdate();
        }
    }

    /**
     * DB-side filtered + paginated listing across ALL questions/games, for
     * Moderator review — unlike {@link #listActiveByQuestion}, not scoped
     * to one question or ACTIVE-only. Any filter left null/blank is simply
     * omitted from the WHERE clause; all values are bound via
     * PreparedStatement placeholders.
     */
    public List<Answer> searchForModeration(String status, Integer questionId, Integer gameId,
                                             String authorUsername, String searchText, int offset, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder(MODERATION_SELECT).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendModerationFilters(sql, params, status, questionId, gameId, authorUsername, searchText);
        sql.append("ORDER BY an.created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            return mapModerationList(ps);
        }
    }

    public int countForModeration(String status, Integer questionId, Integer gameId, String authorUsername,
                                   String searchText) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dbo.Answers an "
                + "JOIN dbo.Accounts a ON a.account_id = an.account_id "
                + "JOIN dbo.Questions q ON q.question_id = an.question_id "
                + "JOIN dbo.Games g ON g.game_id = q.game_id WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendModerationFilters(sql, params, status, questionId, gameId, authorUsername, searchText);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void appendModerationFilters(StringBuilder sql, List<Object> params, String status, Integer questionId,
                                          Integer gameId, String authorUsername, String searchText) {
        if (status != null && !status.isEmpty()) {
            sql.append("AND an.status = ? ");
            params.add(status);
        }
        if (questionId != null) {
            sql.append("AND an.question_id = ? ");
            params.add(questionId);
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
            sql.append("AND an.content LIKE ? ESCAPE '\\' ");
            params.add(likePattern(searchText));
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

    private List<Answer> mapModerationList(PreparedStatement ps) throws SQLException {
        List<Answer> answers = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                answers.add(mapModerationRow(rs));
            }
        }
        return answers;
    }

    private Answer mapModerationRow(ResultSet rs) throws SQLException {
        Answer answer = mapRow(rs);
        answer.setQuestionTitle(rs.getString("question_title"));
        answer.setGameName(rs.getString("game_name"));
        return answer;
    }

    private Answer mapRow(ResultSet rs) throws SQLException {
        Answer answer = new Answer();
        answer.setAnswerId(rs.getInt("answer_id"));
        answer.setQuestionId(rs.getInt("question_id"));
        answer.setAccountId(rs.getInt("account_id"));
        answer.setContent(rs.getString("content"));
        answer.setAccepted(rs.getBoolean("is_accepted"));
        answer.setStatus(rs.getString("status"));
        answer.setDeleted(rs.getBoolean("is_deleted"));
        answer.setDeletedAt(rs.getObject("deleted_at", LocalDateTime.class));
        int deletedBy = rs.getInt("deleted_by");
        answer.setDeletedBy(rs.wasNull() ? null : deletedBy);
        answer.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        answer.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        answer.setAuthorUsername(rs.getString("author_username"));
        return answer;
    }
}
