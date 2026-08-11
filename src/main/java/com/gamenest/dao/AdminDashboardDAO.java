package com.gamenest.dao;

import com.gamenest.dto.AdminDashboardStats;
import com.gamenest.model.AnswerStatus;
import com.gamenest.model.GameStatus;
import com.gamenest.model.QuestionStatus;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Read-only aggregate counts for the Admin Dashboard. Every number comes
 * from a SQL COUNT()/SUM() query against the real tables — no in-memory
 * counting of loaded rows, no fabricated data.
 */
public class AdminDashboardDAO {

    public long countAccounts() throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Accounts";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /**
     * Populates total/active/inactive Game counts in a single round trip.
     */
    public void fillGameStats(AdminDashboardStats stats) throws SQLException {
        String sql = "SELECT COUNT(*) AS total, "
                + "SUM(CASE WHEN status = ? THEN 1 ELSE 0 END) AS active_count "
                + "FROM dbo.Games";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, GameStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long total = rs.getLong("total");
                long active = rs.getLong("active_count");
                stats.setTotalGames(total);
                stats.setActiveGames(active);
                stats.setInactiveGames(total - active);
            }
        }
    }

    /**
     * Populates total/active/deleted Question counts in a single round trip.
     */
    public void fillQuestionStats(AdminDashboardStats stats) throws SQLException {
        String sql = "SELECT COUNT(*) AS total, "
                + "SUM(CASE WHEN status = ? THEN 1 ELSE 0 END) AS active_count, "
                + "SUM(CASE WHEN is_deleted = 1 THEN 1 ELSE 0 END) AS deleted_count "
                + "FROM dbo.Questions";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, QuestionStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                stats.setTotalQuestions(rs.getLong("total"));
                stats.setActiveQuestions(rs.getLong("active_count"));
                stats.setDeletedQuestions(rs.getLong("deleted_count"));
            }
        }
    }

    /**
     * Populates total/active/deleted Answer counts in a single round trip.
     */
    public void fillAnswerStats(AdminDashboardStats stats) throws SQLException {
        String sql = "SELECT COUNT(*) AS total, "
                + "SUM(CASE WHEN status = ? THEN 1 ELSE 0 END) AS active_count, "
                + "SUM(CASE WHEN is_deleted = 1 THEN 1 ELSE 0 END) AS deleted_count "
                + "FROM dbo.Answers";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, AnswerStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                stats.setTotalAnswers(rs.getLong("total"));
                stats.setActiveAnswers(rs.getLong("active_count"));
                stats.setDeletedAnswers(rs.getLong("deleted_count"));
            }
        }
    }
}
