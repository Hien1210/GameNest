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
